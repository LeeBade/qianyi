package com.qianyi.core.model.flow;

import com.qianyi.core.model.flow.node.FlowNode;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * Flow资产定义，描述完整的工作流结构与溯源信息
 *
 * @author TianJunQi
 * @since 2026-06-16
 */
@Document(collection = "flow_definitions")
@Data
public class FlowDefinition {

    @Id
    private String id;

    private String description;

    private Provenance provenance;

    private Map<String, FlowNode> nodes = Map.of();
}