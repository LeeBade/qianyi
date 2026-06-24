package com.qianyi.core.model.flow;


import com.qianyi.core.model.flow.node.NodeExecutionState;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * 流程实例执行状态记录，维护 matter 下所有已触发节点的执行状态
 *
 * @author TianJunQi
 * @since 2026-06-18
 */
@Document(collection = "execution_records")
@Data
public class ExecutionRecord {

    @Id
    private String matterId;

    private Map<String, NodeExecutionState> nodes;

}
