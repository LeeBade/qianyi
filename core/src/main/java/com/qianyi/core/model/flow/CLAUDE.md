
Flow 是一棵**静态有向有根树**，满足以下约束：
- **唯一根节点**：有且仅有一个 `START` 节点，入度严格为 0
- **单一前驱**：除 `START` 外所有节点入度严格等于 1，只有分裂，绝对无汇聚
- **绝对无环**：不存在任何从节点出发回到自身的路径
- **状态剥离**：Flow 本身是无状态的静态指令集，运行期变量全部存储在 MatterContext

**字段说明**
- **`id`**：资产唯一标识，作为 MongoDB `_id`，遵循命名契约，版本号由系统自动分配。
- **`description`**：自然语言描述，面向 IntentRouter 和 Flow 设计者。
- **`nodes`**：节点哈希表`Map<String, FlowNode>`，key 为 `nodeId`
- **`provenance`**：溯源信息，记录当前版本是由哪个 Patch 演进而来
  - `authored_by`：创建者身份
  - `patch_id`：演进来源的 Patch ID
  - `matter_id`：触发本次演进的 Matter ID

## Flow.json说明

**命名契约** `flows/{domain}/{id}/{version}`
- `domain`、`id` 只允许英文、数字、下划线
- `version` 只允许正整数，从 `1` 起始，线性递增，由系统自动分配

**公共字段**
- **`id`**：节点唯一标识
- **`type`**：枚举值 `START | TASK | HUMAN`
- **`triggers`**：出度路由数组，每个元素包含：
    - `target`：目标节点 ID 数组
    - `condition`：可选，字符串表达式，为空时无条件触发

**共三种节点类型**
- **START**：Flow 唯一入口，有且仅有一个，协作流程开始的指令。
- **TASK**：调用一个 Task 资产的指令
- **HUMAN**：需要特定人类意志介入的指令

**独有字段**

- **START**
    - `output_schema`：标准 JSON Schema Draft-07，声明 Matter 发起人的表单字段，全部可选。

- **TASK**
    - `asset_ref`：Task 资产完整路径
    - `input_mapping`：强制显式声明的数据映射表，格式为 `{asset参数名}: "{node_id}.{field_name}"`

- **HUMAN**
    - `assignee`：描述符数组，每个元素为自由字符串
    - `output_schema`：标准 JSON Schema Draft-07，声明责任人的表单字段，全部可选。


# condition 规范草稿

**数据结构**

condition 是一个 ConditionNode 对象，存储时序列化为 JSON 字符串，运行期反序列化求值。condition 为 null 时无条件触发。

支持递归嵌套，无深度限制，但建议 Flow 设计者保持层级克制。


约束：
- Trigger.condition 为 null 时无条件触发，不需要构造任何 ConditionNode
- field 只允许引用 MatterContext.context 中已存在的字段，格式严格对应 `{node_id}.{field_name}`
- value 类型必须与 field 实际类型一致，类型不匹配时求值异常，投递 BusinessNotificationMessage 通知 Flow 设计者修复
- ConditionGroup.rules 至少包含一条，不允许空数组
- ConditionNode、ConditionGroup、ConditionRule 均放入 core.model

**前端生成规则**

业务人员通过可视化操作递归构建条件树：
- 添加规则：选择 context 字段（下拉）、选择运算符（下拉）、填入比较值
- 添加条件组：选择 AND 或 OR，在组内继续添加规则或嵌套条件组
- 前端将条件树序列化为 JSON 写入 Trigger.condition

**Trigger 变更**

```java
@Data
public class Trigger {
    private List<String> target = List.of();
    private ConditionNode condition;
}
```
---
