## API 层

### RocketMQ Consumer

Runtime 通过 RocketMQ 接收触发指令，**不暴露任何 HTTP / gRPC 端口**。

| Topic | ConsumerGroup | 用途 |
|---|---|---|
| `runtime-cascade-execute` | `runtime-consumer-group` | 所有级联触发消息 |

**消息体结构**：字段类型均为 `String`。

```json
{
  "matterId": "1801234567890123456",
  "nodeId": "node_audit"
}
```

**消息体 DTO**：放 `api` 包，禁止放入 Core。

```java
public class CascadeExecuteMessage {
    private String matterId;
    private String nodeId;
    // getter / setter
}
```

### CascadeExecuteConsumer

```java
@Component
@RocketMQMessageListener(
    topic = "runtime-cascade-execute",
    consumerGroup = "runtime-consumer-group"
)
public class CascadeExecuteConsumer implements RocketMQListener<CascadeExecuteMessage> {

    private final FlowApi flowApi;

    public CascadeExecuteConsumer(FlowApi flowApi) {
        this.flowApi = flowApi;
    }

    @Override
    public void onMessage(CascadeExecuteMessage message) {
        flowApi.cascadeExecute(message.getMatterId(), message.getNodeId());
    }
}
```

**职责边界**：Consumer 只做消息反序列化和方法调用，禁止任何业务判断或数据读写。

**多实例并发安全**：同一 `consumerGroup` 内 RocketMQ 保证竞争消费，不会重复消费。

**不依赖 MQ 重投**：Consumer 框架线程立即返回，MQ 自动 ACK，不等待虚拟线程执行结果。

### FlowApi

```java
@Component
public class FlowApi {

    private final ActionChainRouter router;
    private final ActionChainRegistry registry;
    private final FlowStore flowStore;

    public void cascadeExecute(String matterId, String nodeId) {
        ActionChainName chain = router.route(matterId, nodeId);
        ExecutionEngine engine = new ExecutionEngine(registry, flowStore);
        Thread.ofVirtual().start(() -> engine.execute(chain, matterId, nodeId));
    }
}
```

**职责**：传输层到引擎层的适配，通过 `ActionChainRouter` 路由初始链，创建独立 `ExecutionEngine` 实例，启动虚拟线程。系统异常在此抛出，冒泡至全局 handler。

**可观测性盲区**：消息从投递到节点写入 `TRIGGERED` 之间，上游无法感知执行状态，由上游自行处理，Runtime 不提供回调通知。

---
