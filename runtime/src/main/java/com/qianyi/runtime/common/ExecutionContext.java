package com.qianyi.runtime.common;

import com.qianyi.core.model.flow.FlowDefinition;
import com.qianyi.core.model.flow.node.FlowNode;
import com.qianyi.core.model.matter.MatterContext;
import lombok.Getter;

import java.util.Objects;

/**
 * Immutable snapshot of execution state for a single node execution
 * within a virtual thread. Constructed once at thread creation.
 * Never mutated — after a flush the caller builds an entirely new
 * ExecutionContext from the reloaded MatterContext.
 *
 * @author TianJunQi
 * @since 2026-06-29
 */
@Getter
public final class ExecutionContext {

    private final String matterId;
    private final String nodeId;
    private final String queueId;
    private final MatterContext matterContext;
    private final FlowDefinition flowDefinition;
    private final FlowNode currentNode;
    /**
     * The MatterContext.epoch value at the time this context was loaded.
     * Included in every write filter to detect ownership loss (zombie fencing).
     */
    private final long loadedEpoch;

    public ExecutionContext(String matterId,
                            String nodeId,
                            String queueId,
                            MatterContext matterContext,
                            FlowDefinition flowDefinition,
                            FlowNode currentNode) {
        this.matterId = Objects.requireNonNull(matterId, "matterId must not be null");
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null");
        this.queueId = Objects.requireNonNull(queueId, "queueId must not be null");
        this.matterContext = Objects.requireNonNull(matterContext, "matterContext must not be null");
        this.flowDefinition = Objects.requireNonNull(flowDefinition, "flowDefinition must not be null");
        this.currentNode = Objects.requireNonNull(currentNode, "currentNode must not be null");
        this.loadedEpoch = matterContext.getEpoch();
    }
}
