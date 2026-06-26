package com.qianyi.core.message.execution;

/**
 * 节点触发意图枚举
 *
 * FIRST_TRIGGER：首次触发，节点在executionStates中无记录时放行
 * TASK_RETRY   ：人工重试，节点状态为ERROR时放行
 * HUMAN_RESUME ：人工唤醒，节点状态为WAITING时放行
 *
 * 幂等守卫以triggerType和当前节点状态联合判断，
 * TRIGGERED和COMPLETED状态下任何triggerType均被拒绝
 *
 * @author TianJunQi
 * @since 2026-06-26
 */
public enum TriggerType {
    FIRST_TRIGGER,
    TASK_RETRY,
    HUMAN_RESUME
}
