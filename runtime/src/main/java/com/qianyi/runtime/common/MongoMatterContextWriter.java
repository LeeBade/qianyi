package com.qianyi.runtime.common;

import com.qianyi.core.model.matter.MatterContext;
import com.qianyi.core.model.matter.NodeExecutionState;
import com.qianyi.core.model.matter.NodeStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MongoDB implementation of {@link MatterContextWriter}.
 *
 * All write operations use {@link MongoTemplate#updateFirst} or
 * {@link MongoTemplate#updateMulti} with composite filter
 * {@code {_id, queueId, epoch}}. No raw MongoDB driver usage.
 *
 * @author TianJunQi
 * @since 2026-06-29
 */
@Repository
public class MongoMatterContextWriter implements MatterContextWriter {

    private final MongoTemplate mongoTemplate;

    public MongoMatterContextWriter(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void flush(String matterId, String queueId, long epoch,
                      Map<String, Object> pendingUpdates) {
        if (pendingUpdates.isEmpty()) {
            return;
        }

        Query query = new Query(
            Criteria.where("_id").is(matterId)
                    .and("queueId").is(queueId)
                    .and("epoch").is(epoch)
        );

        Update update = new Update();
        pendingUpdates.forEach(update::set);

        long modified = mongoTemplate.updateFirst(query, update, MatterContext.class)
                                     .getModifiedCount();

        if (modified == 0) {
            throw new OwnershipLostException(matterId,
                "flush rejected: queueId or epoch mismatch");
        }
    }

    @Override
    public MatterContext loadMatterContext(String matterId) {
        return mongoTemplate.findById(matterId, MatterContext.class);
    }

    @Override
    public long incrementEpoch(List<String> takenOverQueueIds) {
        Query query = new Query(
            Criteria.where("queueId").in(takenOverQueueIds)
        );
        Update update = new Update().inc("epoch", 1);

        return mongoTemplate.updateMulti(query, update, MatterContext.class)
                            .getModifiedCount();
    }

    @Override
    public long updateQueueId(List<String> takenOverQueueIds, String newQueueId) {
        Query query = new Query(
            Criteria.where("queueId").in(takenOverQueueIds)
        );
        Update update = new Update().set("queueId", newQueueId);

        return mongoTemplate.updateMulti(query, update, MatterContext.class)
                            .getModifiedCount();
    }

    @Override
    public List<OrphanedNodeInfo> findOrphanedNodes(List<String> queueIds,
                                                     long recoveryEpoch) {
        Query query = new Query(Criteria.where("queueId").in(queueIds));
        List<MatterContext> contexts = mongoTemplate.find(query, MatterContext.class);

        List<OrphanedNodeInfo> orphans = new ArrayList<>();

        for (MatterContext ctx : contexts) {
            for (Map.Entry<String, NodeExecutionState> entry
                    : ctx.getExecutionStates().entrySet()) {

                String nodeId = entry.getKey();
                NodeExecutionState state = entry.getValue();

                if (state.getStatus() != NodeStatus.TRIGGERED) {
                    continue;
                }

                Long triggeredEpoch = state.getTriggeredEpoch();

                // triggeredEpoch == null  → legacy doc, definitely pre-FENCE orphan
                // triggeredEpoch < recoveryEpoch → triggered before FENCE, orphan
                // triggeredEpoch >= recoveryEpoch → triggered AFTER FENCE, actively executing, skip
                if (triggeredEpoch != null && triggeredEpoch >= recoveryEpoch) {
                    continue;
                }

                orphans.add(new OrphanedNodeInfo(
                    ctx.getMatterId(),
                    nodeId,
                    ctx.getQueueId(),
                    state.getParentNodeId(),
                    state.getCurrentChain(),
                    state.getCursor(),
                    state.getRpcDispatched(),
                    triggeredEpoch
                ));
            }
        }

        return orphans;
    }
}
