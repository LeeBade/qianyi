## Task

**`id`**：资产唯一标识，作为 MongoDB `_id`。
只允许英文、数字、下划线、斜杠。

**`description`**：自然语言描述，面向 Flow 设计者。

**`idempotency`**：幂等性声明，默认 UNSAFE。
RPC 端点本身支持重复调用则声明 SAFE，否则声明 UNSAFE。
FDE 注册时需主动声明并承担保证责任。

**`policy`**：可选。未填写的键运行期采用系统默认值。
idempotency 决定部分键的默认值和是否允许覆盖：

| 键 | 含义 | SAFE 默认值 | UNSAFE 默认值 | UNSAFE 可覆盖 |
|---|---|---|---|---|
| `timeout_ms` | RPC 调用超时时间 | 60000 | 60000 | 是 |
| `retry_max_attempts` | 最大重试次数 | 3 | 1 | 否，强制为1 |
| `retry_backoff_ms` | 重试间隔 | 1000 | 不适用 | 是 |
| `cb_failure_threshold` | 触发熔断的连续失败次数 | 5 | 5 | 是 |
| `cb_recovery_ms` | 熔断恢复等待时间 | 30000 | 30000 | 是 |

SAFE 的 retry_max_attempts 上限为 10，超出则取 10。

**`input_schema` & `output_schema`**：标准 JSON Schema
Draft-07 语法。output_schema 中二进制类型字段必须声明为
`string` + `format: uri`，FDE 负责将二进制内容上传文件存储
后返回 URL，禁止 RPC 直接返回二进制内容。

**`rpc_action`**：RPC 寻址声明。
- `protocol`：协议类型，需要 SPI 插件支持
- `params`：协议参数，随 `protocol` 各自定义