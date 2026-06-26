package com.qianyi.core.model.flow.node;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Flow节点抽象基类，id与triggers为所有节点公共字段
 *
 * @author TianJunQi
 * @since 2026-06-26
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StartNode.class, name = "START"),
        @JsonSubTypes.Type(value = TaskNode.class, name = "TASK"),
        @JsonSubTypes.Type(value = HumanNode.class, name = "HUMAN")
})
@Getter
@Setter
public abstract sealed class FlowNode permits StartNode, TaskNode, HumanNode {

    private String id;

    private List<Trigger> triggers = List.of();
}
