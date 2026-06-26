# com.qianyi.runtime.recovery 包规范

## 职责

监听 RocketMQ Consumer Rebalance 事件，在当前实例
新接管 MessageQueue 时，精确恢复归属于该 MessageQueue
的孤儿 TRIGGERED 节点。

## 触发时机

RocketMQ 客户端回调 MessageQueueListener，通知当前实例
新接管了哪些 MessageQueue 时触发。
不做全量扫描，不依赖定时轮询，不依赖 Nacos。

## 核心逻辑

1. 从回调中获取新接管的 MessageQueue ID 列表
2. 查询 MongoDB，条件：queueId 在列表中且
   executionStates 中存在状态为 TRIGGERED 的节点
3. 对每个 TRIGGERED 节点，投递
   CascadeExecuteMessage（triggerType=RECOVERY），
   由正常消费流程接管恢复执行，
   RocketMQ 消费速率天然限流，不会引发级联雪崩

## 约束

- RecoveryRunner 只在 MessageQueueListener 回调中触发，
  禁止在其他地方调用
- 恢复执行复用正常消费流程，禁止单独实现一套恢复逻辑
- 只投递消息，不直接执行恢复，在单独的虚拟线程中运行
- queueId 字段已在 MongoDB 建立索引，保证查询性能

## 已知边界

Rebalance 触发到 RecoveryRunner 投递完成之间存在
短暂窗口期，窗口期内孤儿节点暂时无人处理，
这是可接受的最终一致性。