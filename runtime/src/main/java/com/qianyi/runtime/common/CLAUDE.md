## common 包

### 包结构

```
com.qianyi.runtime.common
├── repository
│   ├── MatterContextRepository
│   ├── FlowDefinitionRepository
│   └── TaskRepository
├── store
│   └── FlowStore
├── exception
│   └── FlowExecutionException
└── config
    └── DefaultPolicyProperties
```

### ExecutionContext

放 `common` 包，engine 和 strategy 均可访问。

```java
@Data
public class ExecutionContext {
    private String matterId;
    private String nodeId;
    private MatterContext matterContext;
    private FlowDefinition flowDefinition;
    private FlowNode currentNode;
}
```

`ExecutionContext` 由 `ExecutionEngine.execute` 在虚拟线程启动时构建，写入 `ExecutionScope.CONTEXT`。strategy 层方法通过 `ExecutionScope.CONTEXT.get()` 取用，不通过参数传递。

### Repository

三个接口均继承 `MongoRepository`，无自定义方法。

```java
public interface MatterContextRepository extends MongoRepository<MatterContext, String> {}
public interface FlowDefinitionRepository extends MongoRepository<FlowDefinition, String> {}
public interface TaskRepository extends MongoRepository<Task, String> {}
```

### FlowStore

所有 MongoDB 读写的唯一入口，strategy 层只注入 `FlowStore`，禁止直接依赖任何 Repository。

```java
@Component
public class FlowStore {

    // 组装完整 ExecutionContext，任意字段缺失直接抛出运行时异常
    public ExecutionContext buildContext(String matterId, String nodeId);

    // 按需刷新，链内方法需要最新 MatterContext 时调用
    public MatterContext loadMatterContext(String matterId);

    // execution_states 初始化节点，START_TRIGGER 链第一个方法调用
    public void initNodeState(String matterId, String nodeId, String parentNodeId);

    // execution_states 原子更新，$set 字段级操作，禁止整个文档替换
    public void updateNodeStatus(String matterId, String nodeId, NodeStatus status, String message);

    // 原子提交：task output 写入 context + 节点状态置为 COMPLETED + checkpoint 写回
    // 单文档 $set，事务外层捕获异常重试
    public void commitNodeResult(String matterId, String nodeId,
        MatterContextUpdater updater, NodeStatus status, String message);

    // 原子提交：清空当前节点所有子节点 execution_states + 写入本节点 TRIGGERED
    // 单文档 $set，遍历子节点在同一次写入中完成
    public void clearSubtreeAndWriteTriggered(String matterId, String nodeId,
        FlowDefinition flowDefinition);

    // 查找所有处于 TRIGGERED 状态的中断节点，Runtime 启动时 RecoveryRunner 调用
    public List<InterruptedNode> findInterrupted();
}
```

**buildContext 实现要求**：

1. 读 `MatterContext`，不存在抛运行时异常
2. 读 `FlowDefinition`，不存在抛运行时异常
3. 从 `nodes` 找 `nodeId` 对应 `FlowNode`，找不到抛运行时异常
4. 若 `FlowNode` 是 `TaskNode`，读 `Task` 并内联到 `TaskNode.task`，不存在抛运行时异常
5. 返回组装好的 `ExecutionContext`

**commitNodeResult 实现要求**：

单文档 `$set` 原子操作，合并 context 变更、execution_states 变更、checkpoint 写回为一次写入，事务外层捕获异常重试：

```java
public void commitNodeResult(String matterId, String nodeId,
        MatterContextUpdater updater, NodeStatus status, String message) {
    while (true) {
        try {
            ExecutionEngine engine = ExecutionScope.ENGINE.get();
            Query query = Query.query(Criteria.where("_id").is(matterId));
            Update update = updater.toUpdate("context")
                .set("execution_states." + nodeId + ".status", status)
                .set("execution_states." + nodeId + ".message", message)
                .set("execution_states." + nodeId + ".currentChain", engine.getCurrentChainName())
                .set("execution_states." + nodeId + ".cursor", engine.getCursor());
            mongoTemplate.updateFirst(query, update, MatterContext.class);
            return;
        } catch (Exception e) {
            // 重试
        }
    }
}
```

### MatterContextUpdater

链式构造字段级变更，支持更新和追加，`toUpdate(prefix)` 生成 `Update` 对象。

```java
public class MatterContextUpdater {
    public MatterContextUpdater set(String field, Object value);
    public Update toUpdate(String prefix);
}
```

### FlowExecutionException

```java
public class FlowExecutionException extends RuntimeException {
    private final String userMessage;

    public FlowExecutionException(String userMessage) {
        super(userMessage);
        this.userMessage = userMessage;
    }

    public FlowExecutionException(String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
```

业务异常场景抛出 `FlowExecutionException`，strategy 层捕获后调用 `FlowStore.updateNodeStatus` 写入 `ERROR` 状态和 `userMessage`，然后调用 `stop()`。

### DefaultPolicyProperties

```java
@Component
@ConfigurationProperties(prefix = "qianyi.policy.default")
@Data
public class DefaultPolicyProperties {
    private long timeoutMs;
    private int retryMaxAttempts;
    private long retryBackoffMs;
    private int cbFailureThreshold;
    private long cbRecoveryMs;
}
```

`application.yml`：

```yaml
qianyi:
  policy:
    default:
      timeout-ms: 60000
      retry-max-attempts: 3
      retry-backoff-ms: 1000
      cb-failure-threshold: 5
      cb-recovery-ms: 30000
```

---
