package com.qianyi.core.model.flow.node;

import com.qianyi.core.model.schema.JsonSchema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

/**
 * START节点：Flow唯一入口
 *
 * @author TianJunQi
 * @since 2026-06-26
 */
@TypeAlias("START")
@Data
@EqualsAndHashCode(callSuper = true)
public final class StartNode extends FlowNode {

    private JsonSchema outputSchema;
}

