## 异常处理原则

Runtime 严格区分两类异常：

- **业务异常**：用户可感知、可介入的异常，一律通过节点执行状态表达，**禁止向上抛出**。例：TASK 执行失败，写入 `ERROR` 状态后 `stop()`。
- **系统异常**：用户无法介入、需要 FDE 排查的异常，由全局 `UncaughtExceptionHandler` 统一捕获并记录日志。例：ctx 组装失败（MatterContext 不存在、FlowDefinition 不存在、currentNode 找不到）。

全局 handler 注册位置：Spring Boot 启动类，`Thread.setDefaultUncaughtExceptionHandler`。

**此 handler 禁止处理任何业务异常**，业务异常在到达此处之前必须已被消化。

---

## 包结构


api：外部入口

engine： 执行链的执行引擎

strategy：执行链相关组件

common：共享基础设施
