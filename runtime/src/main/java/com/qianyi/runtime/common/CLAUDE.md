# com.qianyi.runtime.common 包规范

## 定位

Runtime 模块的最底层包，提供所有其他 runtime 包共享的基础设施。
不依赖 engine、strategy、api、recovery 中任何代码。
只依赖 core 模块和 Spring Data MongoDB。

## 类职责

| 类 | 职责 | Spring |
|---|---|---|
| `ExecutionContext` | 虚拟线程执行上下文，一次性构建，不可变 | — |
| `MatterContextWriter` | MatterContext 持久化接口，封装 queueId + epoch 校验 | — |
| `MongoMatterContextWriter` | MongoDB 实现，使用 MongoTemplate 原子写入 | `@Repository` |
| `OwnershipLostException` | 归属丧失异常，无堆栈跟踪，触发虚拟线程终止 | — |
| `ContextFieldReader` | pendingUpdates 优先的上下文字段读取工具 | — |
| `OrphanedNodeInfo` | 孤儿节点查询结果 DTO，Phase 3 QUERY → Phase 4 RECOVER | — |

## 核心约定

### 写入过滤

所有 MatterContext 写入必须同时校验三个条件：
`{_id: matterId, queueId: currentQueueId, epoch: loadedEpoch}`

modifiedCount == 0 → OwnershipLostException → 虚拟线程立即终止。

### pendingUpdates 生命周期

- 虚拟线程创建时 new LinkedHashMap<>()
- 纯内存 Action 写入 pendingUpdates（不 flush）
- 副作用 Action 调用 writer.flush() 后自行 clear()
- key 统一使用 MongoDB 字段路径

### 依赖方向

common → core（单向）。common 内无循环依赖。
engine、strategy、api、recovery → common（单向）。

## 禁止事项

- 禁止在 common 中引入 engine/strategy/api/recovery 的类
- 禁止在接口中引入 Spring 注解
- 禁止 `@Autowired` 字段注入（只允许构造注入）
- 禁止直接访问 pendingUpdates 或 MatterContext.context 读取上下文字段（必须通过 ContextFieldReader）

## 代码注释模板

```java
/**
 * ${Description}
 *
 * @author TianJunQi
 * @since ${YEAR}-${MONTH}-${DAY}
 */
```
