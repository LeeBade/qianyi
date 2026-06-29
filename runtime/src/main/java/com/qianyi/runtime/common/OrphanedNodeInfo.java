package com.qianyi.runtime.common;

import com.qianyi.core.model.matter.ActionChainName;
import lombok.Getter;

/**
 * Lightweight DTO returned by Phase 3 QUERY of the RecoveryRunner.
 * Represents a single orphaned TRIGGERED node that requires recovery.
 *
 * @author TianJunQi
 * @since 2026-06-29
 */
@Getter
public final class OrphanedNodeInfo {

    private final String matterId;
    private final String nodeId;
    private final String queueId;
    private final String parentNodeId;
    private final ActionChainName currentChain;
    private final int cursor;
    /**
     * true means RPC was dispatched but completion unconfirmed.
     * Phase 4 RECOVER uses this to decide: auto-recover vs. ERROR + notify.
     * null for legacy documents written before the rpcDispatched mechanism.
     */
    private final Boolean rpcDispatched;
    /**
     * The epoch snapshot captured when this node was first triggered.
     * null for legacy documents written before the epoch mechanism was introduced.
     */
    private final Long triggeredEpoch;

    public OrphanedNodeInfo(String matterId, String nodeId, String queueId,
                            String parentNodeId, ActionChainName currentChain,
                            int cursor, Boolean rpcDispatched, Long triggeredEpoch) {
        this.matterId = matterId;
        this.nodeId = nodeId;
        this.queueId = queueId;
        this.parentNodeId = parentNodeId;
        this.currentChain = currentChain;
        this.cursor = cursor;
        this.rpcDispatched = rpcDispatched;
        this.triggeredEpoch = triggeredEpoch;
    }
}
