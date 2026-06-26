package com.qianyi.core.model.flow.node;


import lombok.Data;

/**
 *
 *
 * @author TianJunQi
 * @since 2026-06-26
 */
@Data
public class ConditionRule extends ConditionNode {
    private String field;  // 格式：{node_id}.{field_name}
    private CompareOperator op; // == | != | > | >= | < | <=
    private Object value;  // 基本类型：字符串、数字、布尔值
}
