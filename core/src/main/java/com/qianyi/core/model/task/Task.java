package com.qianyi.core.model.task;


import com.qianyi.core.model.schema.JsonSchema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Task 资产定义，描述外部工具或 Agent 的调用契约
 *
 * @author TianJunQi
 * @since 2026-06-18
 */
@Document(collection = "tasks")
@Data
public class Task {

    @Id
    private String id;

    private String description;

    @Field("input_schema")
    private JsonSchema inputSchema;
    /**
     * 幂等性声明，由 FDE 注册 Task 时主动声明，默认 UNSAFE。
     * Runtime 在崩溃恢复时依据此字段决定是否自动重试：
     *   SAFE   → 自动重试，cursor 回退到 executeTask 重新执行
     *   UNSAFE → 写入 ERROR 状态，等待人工触发 TASK_RETRY 链
     */
    private Idempotency idempotency = Idempotency.UNSAFE;
    @Field("output_schema")
    private JsonSchema outputSchema;

    private RpcAction rpcAction;

    private Policy policy;
}
