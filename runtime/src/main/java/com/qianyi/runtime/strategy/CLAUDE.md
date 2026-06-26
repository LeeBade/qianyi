## strategy 包

### 包结构

```
com.qianyi.runtime.strategy
├── NodeStateActions
├── TaskExecutionActions
├── NotifyActions
├── TriggerActions
├── RpcDispatcher
└── RecoveryRunner
```

### @Checkpoint

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Checkpoint {}
```

`CheckpointAspect` 在标注方法执行完成后，从 `ExecutionScope.ENGINE` 取当前 `cursor` 和 `currentChain`，`$set` 写入对应节点的 `execution_states`。

**不需要标注 `@Checkpoint` 的方法**：`commitResult`、`commitHumanOutput`、`clearSubtreeAndWriteTriggered`，这三个方法本身已包含原子写回，写回时一并携带 cursor 和 currentChain。

### NodeStateActions

```java
@Component
public class NodeStateActions {

    private final FlowStore flowStore;

    // START_TRIGGER 链 idx=0
    // 检查 executionStates 是否已存在本节点，已存在则 stop()，否则写入 TRIGGERED
    @Checkpoint
    public void initExecutionRecord();

    // TASK_EXECUTE / TASK_RETRY / HUMAN_TRIGGER 链 idx=0
    // 写入 TRIGGERED 状态，$set 写入
    @Checkpoint
    public void writeTriggered();

    // TASK_RETRY 链 idx=0
    // 单次 $set 原子提交：遍历子节点清空 execution_states + 写入本节点 TRIGGERED + checkpoint 写回
    public void clearSubtreeAndWriteTriggered();

    // TASK_EXECUTE / TASK_RETRY 链 idx=2
    // 原子提交：task output 写入 context + 节点状态置为 COMPLETED + checkpoint 写回
    public void commitResult();

    // HUMAN_TRIGGER 链 idx=2
    // 写入 WAITING 状态，$set 写入，然后调用 stop()
    @Checkpoint
    public void writeWaiting();

    // HUMAN_RESUME 链 idx=0
    // 原子提交：human output 写入 context + 节点状态置为 TRIGGERED + checkpoint 写回
    public void commitHumanOutput();
}
```

### TaskExecutionActions

```java
@Component
public class TaskExecutionActions {

    private final FlowStore flowStore;
    private final RpcDispatcher rpcDispatcher;
    private final DefaultPolicyProperties defaultPolicy;

    // TASK_EXECUTE / TASK_RETRY 链 idx=1
    // 解析 input_mapping 构造入参，Resilience4j 包装重试和熔断：
    //   CircuitBreaker 按 endpoint 共享，Retry / TimeLimiter 按 Task 实例
    // 执行成功：将 output 写入 ExecutionScope.CONTEXT（刷新 matterContext）
    // 执行失败：写入 ERROR 状态和 message，调用 stop()
    @Checkpoint
    public void executeTask();
}
```

### NotifyActions

```java
@Component
public class NotifyActions {

    private final RocketMQTemplate rocketMQTemplate;
    private final FlowStore flowStore;

    // 解析通知对象并投递 RocketMQ 消息
    // 当前节点为 HUMAN：直接取 HumanNode.assignee
    // 当前节点为 TASK：回溯 flowDefinition 父路径找第一个 HUMAN 节点取 assignee，
    //   若父路径无 HUMAN 节点则通知 Matter 发起人（取 MatterContext.initiator）
    @Checkpoint
    public void notify();
}
```

### TriggerActions

```java
@Component
public class TriggerActions {

    private final FlowApi flowApi;

    // 解析 triggers，对每个 condition 求值（从最新 ExecutionScope.CONTEXT 取值），
    // 满足条件的出度目标通过 FlowApi.cascadeExecute 发送 RocketMQ 消息触发子节点
    @Checkpoint
    public void triggerOutbound();
}
```

### RpcDispatcher

```java
@Component
public class RpcDispatcher {

    // 按 RpcAction.protocol 内部适配，新增协议只改内部实现
    public Map<String, Object> dispatch(RpcAction action, Map<String, Object> input);
}
```

上帝类，`dispatch` 统一入口，内部按协议类型路由到对应适配器（HTTP、gRPC 等）。

### RecoveryRunner

```java
@Component
public class RecoveryRunner implements ApplicationRunner {

    private final FlowStore flowStore;
    private final ActionChainRegistry registry;
    private final FlowApi flowApi;

    @Override
    public void run(ApplicationArguments args) {
        List<InterruptedNode> interrupted = flowStore.findInterrupted();
        for (InterruptedNode node : interrupted) {
            ExecutionEngine engine = new ExecutionEngine(registry, flowStore);
            Thread.ofVirtual().start(() -> {
                ExecutionScope.ENGINE.set(engine);
                // 从 MongoDB 重新加载最新 ctx 写入 ThreadLocal
                ExecutionContext ctx = flowStore.buildContext(node.getMatterId(), node.getNodeId());
                ExecutionScope.CONTEXT.set(ctx);
                // 切换到中断时的链，跳过已完成的方法
                engine.switchTo(node.getCurrentChain());
                engine.skipTo(node.getCursor());
                // 直接进入执行循环
                engine.resumeLoop();
            });
        }
    }
}
```

**`resumeLoop`**：`ExecutionEngine` 暴露的内部循环入口，供 `RecoveryRunner` 在 `switchTo` + `skipTo` 之后直接驱动执行，跳过 `buildContext` 步骤。

**`findInterrupted` 查询条件**：扫描所有 MatterContext，返回 `execution_states` 中 status 为 `TRIGGERED` 的节点（`TRIGGERED` 是唯一能证明执行中途崩溃的状态）。

---
