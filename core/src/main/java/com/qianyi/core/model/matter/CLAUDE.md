
## MatterContext

所有写入通过 `$set` 字段级原子操作，不依赖多文档事务，单节点 MongoDB 可用。


**字段说明**

**`matter_id`**：Matter 唯一标识，雪花 ID，作为 MongoDB `_id`。

**`flow_id`**：当前绑定的 Flow 资产完整路径，含版本号。

**`context`**：平铺哈希表，key 格式为 `{node_id}.{field}`。

**`assignee_mapping`**：HUMAN 节点的责任人映射，key 为节点 ID，value 为描述符与自然人的绑定数组。

**`execution_states`**：节点执行状态表，key 为节点 ID，value 为 `NodeExecutionState`。

**`queueId`**：Matter 归属的 RocketMQ MessageQueue ID，
- 由 Runtime 消费 START 节点消息时从消息元数据中取得，
- 随 TRIGGERED 状态原子写入。
- 用于 RecoveryRunner 在 Rebalance 后精确查询归属：新接管 MessageQueue 的 TRIGGERED 节点。
- 必须建立 MongoDB 索引。

---

### NodeExecutionState

```java
@Data
public class NodeExecutionState {
    private String parentNodeId;
    private NodeStatus status;
    private String message;
    private ActionChainName currentChain;
    private int cursor;
}
```

**字段说明**

**`parentNodeId`**：触发当前节点的父节点 ID，用于重试场景下清空子节点状态时的树形遍历。

**`status`**：当前节点执行状态，枚举值见 `NodeStatus`。

**`message`**：补充信息，正常流转时为空，异常时写入 `userMessage`。

**`currentChain`**：当前执行的链名，用于断点恢复。

**`cursor`**：下一个待执行的 Action 索引，用于断点恢复。

---

### NodeStatus

```java
public enum NodeStatus {
    TRIGGERED,  // 已触发，执行中
    WAITING,    // 等待人类介入（HUMAN 节点专用）
    COMPLETED,  // 执行完成
    ERROR       // 执行失败，需人工触发重试
}
```

| 状态 | 适用节点 | 说明 |
|---|---|---|
| `TRIGGERED` | ALL | 节点已开始执行第一个策略 |
| `WAITING` | HUMAN | 等待责任人提交结构化数据 |
| `COMPLETED` | TASK、START | 节点正常完成 |
| `ERROR` | TASK | 执行失败，需人工触发重试 |

---

### ActionChainName

位于 Core 包。

```java
public enum ActionChainName {
    START_TRIGGER,
    TASK_EXECUTE,
    TASK_RETRY,
    HUMAN_TRIGGER,
    HUMAN_RESUME,
}
```

纯命名枚举，仅赋予动作链明确的业务含义，不承担任何调用时机语义。
