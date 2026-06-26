# core.message 包规范

Runtime、Controller 之间以及与自然人交互的所有消息契约
统一定义在此包，禁止在各微服务内部定义跨服务消息体。

## 子包职责

**execution**：执行指令消息，承载节点触发意图。
生产方：Controller、Runtime
消费方：Runtime

**notification**：通知消息，驱动自然人介入。
生产方：Runtime、Controller
消费方：站内信、飞书、企微等通知消费端

## 规范

- 消息体只使用基本类型和 core.model 中的类型，
  禁止依赖任何微服务内部类
- 消息体统一使用 @Data，不手写 setter
- 新增消息类型必须在对应子包的 CLAUDE.md 中补充说明