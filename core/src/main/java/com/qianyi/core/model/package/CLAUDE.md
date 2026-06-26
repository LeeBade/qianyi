## Package

**定位**：IntentRouter 的候选集声明，描述一类问题的解决方案集合。

**`id`**：资产唯一标识，作为 MongoDB `_id`，根前缀固定为 `packages/`，后接自由软目录。

**`description`**：自然语言描述，面向 IntentRouter 语义匹配。

**`includes`**：Flow 路径列表，不含版本号，IntentRouter 据此获取候选 Flow 集合，支持跨域引用，只允许包含 Flow。

---
