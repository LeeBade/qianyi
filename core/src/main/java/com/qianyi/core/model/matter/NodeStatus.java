package com.qianyi.core.model.flow.node;

/**
 * 节点执行状态枚举
 *
 * @author TianJunQi
 * @since 2026-06-18
 */
public enum NodeStatus {
    TRIGGERED,
    RUNNING,
    WAITING,
    COMPLETED,
    ERROR
}
