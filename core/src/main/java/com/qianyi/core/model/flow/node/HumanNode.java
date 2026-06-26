package com.qianyi.core.model.flow.node;

import com.qianyi.core.model.schema.JsonSchema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

import java.util.List;

/**
 * HUMAN节点：需要特定人类意志介入的指令
 *
 * @author TianJunQi
 * @since 2026-06-26
 */
@TypeAlias("HUMAN")
@Data
@EqualsAndHashCode(callSuper = true)
public final class HumanNode extends FlowNode {

    private List<String> assignee = List.of();

    private JsonSchema outputSchema;
}
