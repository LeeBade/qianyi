# com.qianyi.runtime.recovery 包规范

## 职责

监听 RocketMQ Consumer Rebalance 事件，在当前实例
新接管 MessageQueue 时，精确恢复归属于该 MessageQueue
的孤儿 TRIGGERED 节点。

## 触发时机

RocketMQ 客户端回调 MessageQueueListener，通知当前实例
新接管了哪些 MessageQueue 时触发。
不做全量扫描，不依赖定时轮询，不依赖 Nacos。

## 核心逻辑：四阶段恢复流程

RecoveryRunner 按以下顺序执行四个阶段，顺序不可颠倒：

### Phase 1 — FENCE（栅栏）

目标：建立"当前接管者"的纪元边界，使旧实例（僵尸进程）
的后续写入全部失败。

```
1. 读取本次接管 MessageQueue 中所有 MatterContext 的当前
   最大 epoch 值，记为 baselineEpoch
2. 对所有归属于本次接管 MessageQueue 的 MatterContext
   原子递增 epoch：
   
   updateMany(
     {queueId: {$in: takenOverQueueIds}},
     {$inc: {epoch: 1}}
   )
   
3. 计算 recoveryEpoch = baselineEpoch + 1，
   后续 QUERY 阶段的 triggeredEpoch 比较以此值为阈值
```

- FENCE 必须在其他任何操作之前完成
- updateMany 不加 epoch 过滤条件：直接递增所有归属文档，
  避免"不同文档不同 epoch"导致的漏网问题
- 若 RecoveryRunner 崩溃后重启，重新执行 FENCE 会导致
  epoch 再次递增（例如 0→1→2），recoveryEpoch 重新计算。
  这对正确性无影响：triggeredEpoch < recoveryEpoch
  的判断仍然准确，epoch 跳跃是纯美观问题，
  long 类型无溢出风险

### Phase 2 — REBIND（重绑定）

目标：更新 MatterContext.queueId 为当前实例的新
MessageQueue ID，使后续正常执行写入的 queueId 校验通过。

```
updateMany(
  {queueId: {$in: takenOverQueueIds}},
  {$set: {queueId: newQueueId}}
)
```

- 所有正常执行写入的 filter 条件为
  `{_id: matterId, queueId: currentQueueId, epoch: loadedEpoch}`
- REBIND 完成后，新实例的 queueId 校验通过，正常执行可以恢复

### Phase 3 — QUERY（查询）

目标：精确查询真正孤儿（在 FENCE 前触发）的 TRIGGERED 节点。

```
查询条件：
  queueId 在本次接管列表中
  AND executionStates 中存在 status=TRIGGERED 的节点
  AND (
    triggeredEpoch < recoveryEpoch
    OR triggeredEpoch 不存在（null，即 epoch 机制引入前的存量文档）
  )
```

- `triggeredEpoch < recoveryEpoch`：节点在 FENCE 前触发，
  原执行者已崩溃，当前无人执行 → 真正孤儿
- `triggeredEpoch >= recoveryEpoch`：节点在 FENCE 后触发，
  由当前实例的正常消费流程触发，正在执行中 → 非孤儿，跳过
- `triggeredEpoch 为 null`：存量文档，在 epoch 机制引入前
  写入，一定属于接管前触发 → 视为孤儿

### Phase 4 — RECOVER（恢复）

目标：对每个孤儿节点，按节点类型和 rpcDispatched 标记
决定恢复策略。

**SAFE Task 或非 TASK 节点**：
投递 CascadeExecuteMessage（triggerType=RECOVERY），
由正常消费流程从 cursor 位置恢复执行。

**UNSAFE Task 且 rpcDispatched=true**：
RPC 调用已发出但完成状态未确认。
- 不投递 RECOVERY 消息（禁止自动恢复）
- 写入节点状态 ERROR
- 写入 message："RPC was dispatched but completion unconfirmed — check external system before TASK_RETRY"
- 投递 BusinessNotificationMessage 通知业务人员介入
- 投递 SystemErrorMessage 通知 FDE

**UNSAFE Task 且 rpcDispatched 不为 true（null 或 false）**：
RPC 尚未发出或为存量文档（无此字段），
投递 RECOVERY 消息，由正常消费流程按 Task.idempotency
决定恢复策略（UNSAFE → 写入 ERROR 等待人工 TASK_RETRY）。

## 约束

- RecoveryRunner 只在 MessageQueueListener 回调中触发，
  禁止在其他地方调用
- 恢复执行复用正常消费流程，禁止单独实现一套恢复逻辑
- 只投递消息，不直接执行恢复，在单独的虚拟线程中运行
- queueId 字段已在 MongoDB 建立索引，保证查询性能
- 四阶段顺序不可颠倒：FENCE → REBIND → QUERY → RECOVER
- QUERY 的 triggeredEpoch 判断必须包含 null 兜底，
  否则 epoch 机制引入前的存量 MatterContext 无法恢复

## 已知边界

Rebalance 触发到 RecoveryRunner 完成 FENCE 之间存在
短暂窗口期，窗口期内僵尸进程可能通过 queueId 校验。
FENCE 完成后僵尸进程的所有后续写入将被 epoch 校验拦截。
这是可接受的最终一致性。
