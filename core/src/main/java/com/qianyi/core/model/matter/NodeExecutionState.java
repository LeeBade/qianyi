package com.qianyi.core.model.flow.node;


import lombok.Data;

/**
 * 节点执行状态
 *
 * @author TianJunQi
 * @since 2026-06-18
 */
@Data
public class NodeExecutionState {

    private String parentNodeId;

    private NodeStatus status;
}
