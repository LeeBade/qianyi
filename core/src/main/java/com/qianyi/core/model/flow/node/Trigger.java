package com.qianyi.core.model.flow.node;

import lombok.Data;

import java.util.List;

/**
 * 节点触发器，描述出度路由规则
 *
 * @author TianJunQi
 * @since 2026-06-26
 */
@Data
public class Trigger {
    private List<String> target = List.of();
    private ConditionNode condition;
}
