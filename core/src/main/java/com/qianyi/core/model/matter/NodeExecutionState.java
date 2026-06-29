package com.qianyi.core.model.matter;


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
    private String message;
    private ActionChainName currentChain;
    private int cursor;

    /**
     * 节点首次触发时的 MatterContext.epoch 快照。
     * RecoveryRunner 以此判断节点是否在接管前触发：
     * triggeredEpoch < currentEpoch → 真正孤儿，需恢复；
     * triggeredEpoch >= currentEpoch → 接管后触发，不恢复。
     * null 表示 epoch 机制引入前的存量文档，视为接管前触发。
     */
    private Long triggeredEpoch;

    /**
     * UNSAFE Task 专用：RPC 调用是否已发出但完成状态未确认。
     * 在 TRIGGERED 写入时原子置为 true，RPC 完成时清空。
     * RecoveryRunner 发现 rpcDispatched=true 时禁止自动恢复，
     * 写入 ERROR 等待人工确认。
     */
    private Boolean rpcDispatched;
}
