package com.qianyi.core.model.matter;

/**
 * 纯命名枚举，仅赋予动作链明确的业务含义，不承担任何调用时机语义。
 *
 * @author TianJunQi
 * @since 2026-06-23
 */
public enum ActionChainName {
    START_TRIGGER,
    TASK_EXECUTE,
    TASK_RETRY,
    HUMAN_TRIGGER,
    HUMAN_RESUME,
}
