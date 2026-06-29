package com.qianyi.core.model.matter;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

/**
 * Matter运行时上下文，绑定Flow、保存平铺状态与责任人映射
 *
 * @author TianJunQi
 * @since 2026-06-16
 */
@Document(collection = "matter_contexts")
@Data
public class MatterContext {

    @Id
    private String matterId;

    private String flowId;

    private Map<String, Object> context = Map.of();

    private Map<String, List<AssigneeBinding>> assigneeMapping = Map.of();

    @Field("execution_states")
    private Map<String, NodeExecutionState> executionStates = Map.of();

    private String queueId;

    /**
     * 单调递增的归属纪元，RecoveryRunner 接管时递增。
     * 所有持久化操作必须校验 epoch 归属当前实例，校验失败立即终止执行。
     */
    private long epoch = 0;
}