package com.qianyi.runtime.common;

import com.qianyi.core.model.matter.MatterContext;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates all persistence operations on MatterContext documents.
 *
 * Write operations use MongoDB atomic {@code $set} with a composite
 * filter {@code {_id, queueId, epoch}} to enforce ownership validation.
 * Filter mismatch throws {@link OwnershipLostException}.
 *
 * This interface is pure Java — no Spring stereotypes. The MongoDB
 * implementation ({@code MongoMatterContextWriter}) carries
 * {@code @Repository}.
 *
 * @author TianJunQi
 * @since 2026-06-29
 */
public interface MatterContextWriter {

    // ==================== Engine-facing operations ====================

    /**
     * Atomically flushes all pending updates to the MatterContext document
     * using {@code $set}. The write filter includes {@code {_id, queueId, epoch}}.
     *
     * On success, the caller MUST clear its per-thread pendingUpdates map.
     * The writer does NOT modify the caller's map.
     *
     * On filter mismatch (modifiedCount == 0), throws
     * {@link OwnershipLostException}. The calling virtual thread
     * MUST terminate immediately.
     *
     * @param matterId       the MatterContext._id
     * @param queueId        the current RocketMQ MessageQueue ID for ownership check
     * @param epoch          the epoch value from {@link ExecutionContext#getLoadedEpoch()}
     * @param pendingUpdates keys are MongoDB field paths, values are the new values
     * @throws OwnershipLostException if queueId or epoch no longer matches
     */
    void flush(String matterId, String queueId, long epoch, Map<String, Object> pendingUpdates);

    /**
     * Loads the current MatterContext document by its {@code _id}.
     * Used when building a new ExecutionContext (initial load and post-flush reload).
     *
     * @param matterId the MatterContext._id
     * @return the loaded document, or {@code null} if not found
     */
    MatterContext loadMatterContext(String matterId);

    // ==================== RecoveryRunner-facing operations ====================

    /**
     * Phase 1 — FENCE.
     * Atomically increments {@code epoch} by 1 for every MatterContext
     * whose {@code queueId} is in the provided list.
     *
     * This establishes the new ownership epoch boundary. Any in-flight
     * writes from the previous owner will fail the epoch filter check.
     *
     * @param takenOverQueueIds the MessageQueue IDs taken over after Rebalance
     * @return the number of documents modified
     */
    long incrementEpoch(List<String> takenOverQueueIds);

    /**
     * Phase 2 — REBIND.
     * Updates {@code queueId} on all MatterContexts belonging to the
     * taken-over queues to point to the new instance's MessageQueue.
     *
     * After this completes, normal execution writes from the new
     * instance will pass the queueId filter check.
     *
     * @param takenOverQueueIds the MessageQueue IDs taken over after Rebalance
     * @param newQueueId        the new MessageQueue ID assigned to the current instance
     * @return the number of documents modified
     */
    long updateQueueId(List<String> takenOverQueueIds, String newQueueId);

    /**
     * Phase 3 — QUERY.
     * Finds all TRIGGERED nodes that are true orphans —
     * triggered before the FENCE (triggeredEpoch {@code <} recoveryEpoch
     * or triggeredEpoch is null for legacy documents).
     *
     * Nodes with {@code triggeredEpoch >= recoveryEpoch} were triggered
     * AFTER the FENCE, by the current instance's normal consumption, and
     * are actively executing — they are excluded.
     *
     * @param queueIds       the IDs of queues taken over in this Rebalance
     * @param recoveryEpoch  the epoch value after FENCE increment (baselineEpoch + 1)
     * @return list of orphaned node records; empty list if none found
     */
    List<OrphanedNodeInfo> findOrphanedNodes(List<String> queueIds, long recoveryEpoch);
}
