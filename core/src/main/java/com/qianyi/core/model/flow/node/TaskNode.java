package com.qianyi.core.model.flow.node;

import com.qianyi.core.model.task.Policy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

import java.util.Map;

/**
 * TASK节点：调用Task资产的指令
 *
 * @author TianJunQi
 * @since 2026-06-26
 */
@TypeAlias("TASK")
@Data
@EqualsAndHashCode(callSuper = true)
public final class TaskNode extends FlowNode {

    private String assetRef;

    private Map<String, String> inputMapping = Map.of();


}