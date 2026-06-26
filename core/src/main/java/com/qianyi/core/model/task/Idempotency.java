package com.qianyi.core.model.task;

/**
 * Task幂等性声明。
 *
 * SAFE   ：RPC 端点本身支持重复调用，多次调用与单次调用结果一致，
 *          FDE 注册时需主动声明并承担保证责任。
 * UNSAFE ：RPC 端点不保证重复调用安全（外部服务、物理设备、无法改造的遗留系统等），
 *          崩溃后不可自动重试，由人工介入决定是否触发 TASK_RETRY。
 *          默认值，FDE 未声明时 Runtime 保守处理。
 * @author TianJunQi
 * @since 2026-06-24
 */
public enum Idempotency {
    SAFE,
    UNSAFE;
}
