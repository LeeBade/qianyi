
# 产品定位

千依是一个解决方案协作网络，将问题的解决过程沉淀为可复用、可执行、可持续演进的资产。具体定位请查阅README.md

---

# 微服务构成

**Runtime**：以 MatterContext 为共享上下文，根据 Flow 节点类型与当前执行状态选择并执行对应指令，并将执行结果写回 MatterContext。对上游而言是可靠的、内敛的服务。

**Controller**：唯一的跨服务编排者。承接自然人发起协作请求，调用 Roster、IntentRouter，并与前端交互。

**Roster**：将 `List<String> assignee` 描述符数组解析为 `Map<String, List<AssigneeBinding>>` 责任人映射，键为节点 ID，值为描述符与自然人的绑定结果。解析规则由对接的第三方系统实现，Roster 本身只做路由。

**Asset**：负责 Flow、Task、Package 三类资产的管理与版本控制。Flow 版本由 Asset 自动分配，线性递增。

**IntentRouter**：外部 AI 服务（Python + LangChain），不在 Java 微服务范围内，由 Controller 通过 HTTP 调用。

---

# 服务间关系

| 调用方 | 被调用方 | 传递什么 | 方式 |
|---|---|---|---|
| Controller | Roster | 描述符 → 责任人映射解析 | OpenFeign |
| Controller | IntentRouter | 用户意图 → Flow 匹配 | HTTP |
| Controller | MongoDB | 读写资产、MatterContext | 直接读写 |
| Controller | Runtime | 触发 Matter 执行 | RocketMQ |
| Runtime | MongoDB | 读取 Flow、Task，写回 MatterContext | 直接读写 |
| Runtime | 通知消费端 | HUMAN 节点触发通知 | RocketMQ |

---

# RocketMQ 职责边界

RocketMQ 承担两条独立通道：

**触发通道**：自然人通过 Controller 发起 Matter，Controller 将执行指令投递到 RocketMQ，Runtime 消费并开始执行。Runtime在级联触发时，将触发子节点的指令封装为消息体投递到RocketMQ

**通知通道**：Runtime 执行到 HUMAN 节点后投递通知消息，消费端（站内信、飞书、企微等）独立实现，互不干扰。

---

# 数据层规范

各服务直接读写 MongoDB，无需通过其他服务中转。Asset 负责资产的管理与版本控制，不作为数据提供方对外提供查询接口。

---

# 前端

两个核心能力：
- Flow 动态构建
- 协作通知（站内信）

通知通过 RocketMQ 广播解耦，消费端可同时支持站内信、飞书、企微等，各消费端独立实现。

---

# 技术规范

**同步服务调用**：OpenFeign + Spring Web MVC
- 声明式接口，接口契约与 core.model 天然绑定
- 接口变更只改 core.model，不存在双重维护问题
- 排除 gRPC：服务间调用非高频低延迟场景，无跨语言强需求
- 排除 RestTemplate：Spring 官方已标记为维护模式
- 排除 WebClient：响应式模型与虚拟线程同步风格不兼容

**异步通信**：RocketMQ

**数据层**：MongoDB，字段级原子写入（`$set`），禁止整文档替换

**服务发现**：Nacos（各服务设计阶段展开）

---

# 共享内核 core.model

被超过一个微服务使用的数据模型和枚举统一放入 core.model。

**禁止**：任何微服务依赖其他微服务的内部类。

当前已确认放入：
- FlowDefinition 及所有节点类型
- Task
- MatterContext
- NodeStatus 枚举
- CascadeExecuteMessage
# 代码注释模板

```java

/**
 * ${Description}
 *
 * @author TianJunQi
 * @since ${YEAR}-${MONTH}-${DAY}
 */ 


```

## 规划阶段

只实现已在场景库和设计文档中明确记录的内容，禁止任何未经设计的扩展。

