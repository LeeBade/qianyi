

# core.message.notification 包规范

通知消息，由 Runtime、Controller 投递，各通知消费端独立消费，互不干扰。

Topic：runtime-notification
ConsumerGroup：各消费端独立定义，不在 core 中约束

## 规范

- 通知消息体只承载通知所需的最小信息集，不携带业务执行上下文
- 消费端幂等由各消费端自行保证，core 不约束
- Roster 作为通知消费端，负责将消息路由给正确的自然人，FDE 视为特殊的业务人员

## 当前消息类型

**SystemErrorMessage**
- traceId：链路追踪 ID，FDE 凭此在日志系统定位完整堆栈
- errorMessage：直接来自底层异常 message，供 FDE 快速分诊

**BusinessNotificationMessage**
- matterId：Matter 唯一标识
- nodeId：目标节点 ID
- message：自然语言描述，由抛出业务异常处显式声明，面向业务人员