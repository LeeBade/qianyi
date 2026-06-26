## engine 包

### 包结构

```
com.qianyi.runtime.engine
├── ExecutionEngine
├── ExecutionControl
├── ExecutionScope
├── ActionChain
├── ActionChainRegistry
└── ActionChainRouter
```

### ExecutionScope

```java
public class ExecutionScope {
    public static final ThreadLocal<ExecutionEngine> ENGINE = new ThreadLocal<>();
    public static final ThreadLocal<ExecutionContext> CONTEXT = new ThreadLocal<>();
}
```

每个虚拟线程独立持有自己的 `ExecutionEngine` 实例和 `ExecutionContext`，链内方法通过 ThreadLocal 访问，无需参数传递。

### ExecutionControl

```java
public interface ExecutionControl {
    void switchTo(ActionChainName name);
    void stop();
    void skipTo(int cursor);
}
```

`ExecutionEngine` 暴露给 strategy 层的控制句柄。

- `switchTo`：切换到另一条链，重置游标为 0
- `stop`：将游标推至末尾，终止当前链执行
- `skipTo`：将游标设置到指定位置，用于断点恢复

### ExecutionEngine

多例，由 `FlowApi` 直接 `new` 创建，不由 Spring 管理。

```java
public class ExecutionEngine implements ExecutionControl {

    private final ActionChainRegistry registry;
    private final FlowStore flowStore;
    private ActionChain current;
    private int cursor;

    public ExecutionEngine(ActionChainRegistry registry, FlowStore flowStore) {
        this.registry = registry;
        this.flowStore = flowStore;
    }

    public void execute(ActionChainName initial, String matterId, String nodeId) {
        ExecutionContext ctx = flowStore.buildContext(matterId, nodeId);
        ExecutionScope.ENGINE.set(this);
        ExecutionScope.CONTEXT.set(ctx);
        switchTo(initial);
        while (cursor < current.getActions().size()) {
            current.getActions().get(cursor++).run();
        }
    }

    @Override
    public void switchTo(ActionChainName name) {
        this.current = registry.getChain(name);
        this.cursor = 0;
    }

    @Override
    public void stop() {
        this.cursor = current.getActions().size();
    }

    @Override
    public void skipTo(int cursor) {
        this.cursor = cursor;
    }
}
```

持有当前链和游标，实现 `ExecutionControl`。`execute` 负责构建 ctx 并写入 ThreadLocal，链内所有方法直接从 `ExecutionScope` 取用。

### ActionChain

```java
public class ActionChain {
    private final ActionChainName name;
    private final List<Runnable> actions;
}
```

持有链名和对应的 Runnable 列表，是注册表的值对象。

### ActionChainRegistry

```java
@Component
public class ActionChainRegistry {

    private final NodeStateActions nodeStateActions;
    private final TaskExecutionActions taskExecutionActions;
    private final NotifyActions notifyActions;
    private final TriggerActions triggerActions;

    public ActionChain getChain(ActionChainName name) {
        return switch (name) {
            case START_TRIGGER -> new ActionChain(name, List.of(
                nodeStateActions::initExecutionRecord,   // idx=0
                triggerActions::triggerOutbound          // idx=1
            ));
            case TASK_EXECUTE -> new ActionChain(name, List.of(
                nodeStateActions::writeTriggered,        // idx=0
                taskExecutionActions::executeTask,       // idx=1
                nodeStateActions::commitResult,          // idx=2
                notifyActions::notify,                   // idx=3
                triggerActions::triggerOutbound          // idx=4
            ));
            case TASK_RETRY -> new ActionChain(name, List.of(
                nodeStateActions::clearSubtreeAndWriteTriggered,  // idx=0
                taskExecutionActions::executeTask,                 // idx=1
                nodeStateActions::commitResult,                    // idx=2
                notifyActions::notify,                             // idx=3
                triggerActions::triggerOutbound                    // idx=4
            ));
            case HUMAN_TRIGGER -> new ActionChain(name, List.of(
                nodeStateActions::writeTriggered,        // idx=0
                notifyActions::notify,                   // idx=1
                nodeStateActions::writeWaiting           // idx=2，内部调用 stop()
            ));
            case HUMAN_RESUME -> new ActionChain(name, List.of(
                nodeStateActions::commitHumanOutput,     // idx=0
                notifyActions::notify,                   // idx=1
                triggerActions::triggerOutbound          // idx=2
            ));
        };
    }
}
```

纯注册表，定义每条链的方法组装顺序。禁止引入任何业务逻辑。

### ActionChainRouter

```java
@Component
public class ActionChainRouter {

    private final FlowStore flowStore;

    public ActionChainRouter(FlowStore flowStore) {
        this.flowStore = flowStore;
    }

    public ActionChainName route(String matterId, String nodeId);
}
```

**职责**：从 MongoDB 读取 MatterContext 和 FlowDefinition，基于节点类型和 `executionStates` 中的节点状态映射到对应的 `ActionChainName`。

**场景映射表**：

| 节点类型 | 当前状态 | 映射结果 |
|---|---|---|
| START | 任意 | `START_TRIGGER` |
| TASK | `null`（无记录） | `TASK_EXECUTE` |
| TASK | `ERROR` | `TASK_RETRY` |
| TASK | `TRIGGERED` / `COMPLETED` | 守卫，返回后由调用方 `stop()` |
| HUMAN | `null`（无记录） | `HUMAN_TRIGGER` |
| HUMAN | `WAITING` | `HUMAN_RESUME` |
| HUMAN | `COMPLETED` | 守卫，返回后由调用方 `stop()` |

**调用方**：仅 `FlowApi` 和 `RecoveryRunner` 调用。

---
