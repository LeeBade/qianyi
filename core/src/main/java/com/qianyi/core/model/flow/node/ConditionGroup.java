package com.qianyi.core.model.flow.node;


import lombok.Data;

import java.util.List;

/**
 *
 *
 * @author TianJunQi
 * @since 2026-06-26
 */
@Data
public class ConditionGroup extends ConditionNode {
    private LogicOperator logic; // AND | OR
    private List<ConditionNode> rules;
}
