# 已知漏洞与边界

本文件记录千依 Runtime 设计中已识别但尚未完全解决的漏洞，
供后续演进参考。每个漏洞包含完整的前因后果，不依赖其他文档。

---

## 漏洞一：Rebalance 窗口期内僵尸进程脑裂 ✅ RESOLVED

### 背景

千依 Runtime 是消息驱动的分布式执行引擎，多个 Runtime
实例通过 RocketMQ 消费执行指令。故障恢复的前提是能精确
定位残局归属——崩溃后，存活实例必须知道自己需要恢复哪些
Matter，不能全量扫描，也不能多实例竞争抢占。

为此利用 RocketMQ MessageQueue 与消费者实例的天然绑定
关系：同一 Matter 的所有消息归属于同一 MessageQueue，
同一 MessageQueue 在同一时刻只由一个实例消费。崩溃后
新实例接管 MessageQueue，天然知道自己负责哪些 Matter，
精确恢复，无竞争。

MatterContext 中存储 queueId 字段，记录当前 Matter 归属的
MessageQueue ID。所有持久化操作前，Runtime 必须校验
queueId 归属当前实例，校验和写入在 MongoDB 层面原子完成，
校验失败则立即终止执行。

当某个 Runtime 实例崩溃或下线时，RocketMQ 触发
Consumer Rebalance，将该实例负责的 MessageQueue 移交给
存活实例。新实例的 RecoveryRunner 监听 Rebalance 回调，
接管后更新 MatterContext 中的 queueId 为新的
MessageQueue ID，并投递 RECOVERY 消息恢复孤儿节点执行。

### 漏洞描述

长时间 GC 停顿或网络停顿可能导致僵尸进程：Runtime 实例
没有真正崩溃，但停止响应心跳，RocketMQ 认为其已下线并
触发 Rebalance，将其 MessageQueue 移交给新实例。

僵尸进程在 Rebalance 进行中苏醒，此时 queueId 尚未被
RecoveryRunner 更新，僵尸进程的 queueId 前置校验通过，
与新实例同时执行同一个 Matter 的同一个节点，产生脑裂。

具体场景：
1. 僵尸进程执行到事务消息投递步骤，cursor 持久化作为本地事务提交
2. 下游节点 CascadeExecuteMessage 对消费方可见，无法撤回
3. RecoveryRunner 随后投递 RECOVERY 消息
4. 下游节点先收到 FIRST_TRIGGER（僵尸投递），写入 TRIGGERED
5. RECOVERY 到达时幂等守卫检查状态为 TRIGGERED，放行
6. 下游节点被执行两次

### 解决方案：Epoch 栅栏

引入 MatterContext.epoch 单调递增计数器作为栅栏令牌：

1. **NodeExecutionState 新增 triggeredEpoch 字段**：
   节点首次触发时记录当前 epoch 快照

2. **RecoveryRunner FENCE 阶段**（在所有其他操作之前）：
   原子递增所有接管 MatterContext 的 epoch

3. **RECOVERY 幂等守卫收紧**：
   放行条件从"状态为 TRIGGERED"收紧为
   "状态为 TRIGGERED 且（triggeredEpoch < currentEpoch
   或 triggeredEpoch 为 null）"

4. **所有写入增加 epoch 校验**：
   MongoDB 写入 filter 从 `{_id, queueId}` 扩展为
   `{_id, queueId, epoch}`

### 解决效果

- Rebalance 完成后苏醒：FENCE 已完成，epoch 已递增，
  僵尸进程写入被 epoch 校验拦截，执行终止 → 完全防御
- Rebalance 进行中苏醒并完成事务消息投递：
  FIRST_TRIGGER 在 FENCE 后写入 triggeredEpoch >= currentEpoch，
  RECOVERY 幂等守卫拒绝放行 → 下游节点只执行一次
- 存量文档（triggeredEpoch 为 null）：QUERY 阶段
  triggeredEpoch 不存在视为接管前触发，正常恢复

### 残留风险

FENCE 完成前的极端窗口：僵尸进程在 FENCE 执行前完成
事务消息投递且 FIRST_TRIGGER 已被消费写入（triggeredEpoch
为旧值 < recoveryEpoch），RECOVERY 仍会放行。
此窗口仅存在于 FENCE updateMany 执行期间（通常 < 100ms），
且需 zombie 恰好在此窗口内完成事务提交 + 消息消费 +
TRIGGERED 写入三个步骤。概率极低，业界成熟系统也接受。

---

## 漏洞二：UNSAFE Task RPC 调用的重复执行 🟡 MITIGATED

### 背景

千依 Runtime 执行 TASK 节点时，需要按照 Task 资产声明的
RPC 寻址信息调用外部服务。Task 分为两种幂等性声明：

- **SAFE**：RPC 端点支持重复调用，Runtime 可在崩溃恢复后
  自动重新执行
- **UNSAFE**：RPC 端点不支持重复调用，重复调用会产生不可
  撤回的副作用（如转账、发货、发送通知等）

为保护 UNSAFE Task，设计了以下机制：
- UNSAFE Task 的 retry_max_attempts 强制为 1，不允许重试
- 崩溃恢复时 UNSAFE Task 写入 ERROR 状态，等待人工触发
  TASK_RETRY，而不是自动恢复执行

### 漏洞描述

无论采用何种校验顺序，UNSAFE RPC 调用都存在重复执行风险：

**先校验后调用**：
校验 queueId 归属成功 → 调用 RPC → 写入状态
校验和调用之间存在窗口，僵尸进程在校验通过后苏醒，
两个实例都校验通过，都发出 RPC 调用，
UNSAFE 工具被执行两次，副作用无法撤回。

**先调用后校验**：
调用 RPC → 校验 queueId → 写入状态
RPC 已经执行，副作用已经发生，校验失败后无法回滚，
UNSAFE 工具同样被执行了，只是状态写入被拦截。

### 根本原因

RPC 调用是外部系统操作，不在 MongoDB 的事务边界内。
校验归属可以做到原子，但无法把 RPC 调用纳入同一个原子操作。
跨系统操作没有全局原子性，这是分布式系统的本质限制。

### 缓解措施

**epoch 栅栏**（漏洞一的解决方案）缩小了僵尸进程的写入窗口：
FENCE 完成后，僵尸进程的后续写入全部被拦截。
但 RPC 调用仍可能在 FENCE 前发出。

**rpcDispatched 标记**（NodeExecutionState 新增字段）：
UNSAFE Task 在执行 RPC 前，将 rpcDispatched 与 TRIGGERED
写入合并为单次原子 `$set`。

RecoveryRunner 发现 UNSAFE Task 的 TRIGGERED 节点且
rpcDispatched=true 时：
- 不投递 RECOVERY（禁止自动恢复）
- 写入 ERROR 并通知业务人员："RPC was dispatched but
  completion unconfirmed — check external system before TASK_RETRY"

这确保了 **UNSAFE RPC 不会被自动重复执行**。
人工 TASK_RETRY 由业务人员确认外部系统状态后触发。

### 触发条件

需同时满足：
1. 发生长时间 GC 停顿或网络停顿
2. 停顿期间恰好触发 Rebalance
3. 停顿结束时 FENCE 尚未完成
4. 当前节点是 UNSAFE Task
5. 僵尸进程在 FENCE 前完成 TRIGGERED + rpcDispatched 原子写入
   并发出 RPC 调用
6. 业务人员后续触发 TASK_RETRY（未经确认外部系统状态）

### 当前结论

Runtime 层面无法完全消灭此漏洞，已做到最大程度的自动防御：
- epoch 栅栏缩小窗口
- rpcDispatched 标记防止自动重复执行
- UNSAFE Task retry_max_attempts 强制为 1

**根本解法需要在 Task 设计层面由 FDE 保证**：
声明为 UNSAFE 的 Task，其 RPC 端点必须在业务层面具备
幂等能力，即使被重复调用，业务结果也是正确的。
这是 FDE 注册 UNSAFE Task 时必须承担的责任。

记录为已缓解但不可完全消灭的边界，
在 Task 资产规范中保留 FDE 责任声明。
