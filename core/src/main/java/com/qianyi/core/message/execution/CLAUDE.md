# core.message.execution 包规范

节点执行指令消息，承载触发意图，
由 Controller 或 Runtime 投递，Runtime 消费。

Topic：runtime-cascade-execute
ConsumerGroup：runtime-consumer-group

## 消息体

**CascadeExecuteMessage**
- matterId：Matter 唯一标识
- nodeId：目标节点 ID
- triggerType：触发意图，决定幂等守卫的状态校验条件

## TriggerType 枚举与幂等守卫规则

| TriggerType   | 放行条件                        | 拒绝条件                      |
|---------------|-----------------------------|---------------------------|
| FIRST_TRIGGER | 节点在 executionStates 中无记录    | 任何已有状态                    |
| TASK_RETRY    | 节点状态为 ERROR                 | 其他状态                      |
| HUMAN_RESUME  | 节点状态为 WAITING               | 其他状态                      |
| RECOVERY      | 节点状态为 TRIGGERED             | 其他状态                      |
| （所有类型）      | -                           | TRIGGERED / COMPLETED 均拒绝 |

## 扩展规范

新增 TriggerType 时：
1. 在此文件补充枚举说明和幂等守卫规则
2. 在场景库中补充对应场景
3. 禁止在未补充场景的情况下直接编码