package com.qianyi.core.model.flow.node;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 *
 *
 * @author TianJunQi
 * @since 2026-06-26
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConditionGroup.class, name = "group"),
        @JsonSubTypes.Type(value = ConditionRule.class, name = "rule")
})
public abstract class ConditionNode {}
