package com.qianyi.runtime.common;

import java.util.Map;

/**
 * Utility for reading context field values with pendingUpdates priority.
 *
 * All context field reads MUST go through this utility.
 * Direct access to pendingUpdates or MatterContext.context is prohibited.
 *
 * @author TianJunQi
 * @since 2026-06-29
 */
public final class ContextFieldReader {

    private ContextFieldReader() {
        throw new UnsupportedOperationException("Utility class — do not instantiate");
    }

    /**
     * Reads a context field value. Checks the per-thread pendingUpdates
     * map first; falls back to the persisted MatterContext.context map.
     *
     * Uses {@link Map#containsKey} to distinguish a null-value write
     * (key present in pendingUpdates, value is null) from a missing key
     * (key absent from both pendingUpdates and persisted context).
     *
     * @param ctx            the immutable execution context snapshot
     * @param pendingUpdates the per-thread pending updates map
     *                       (keys are MongoDB field paths, e.g. "context.start.amount")
     * @param contextKey     the bare context key (e.g. "start.amount");
     *                       will be prefixed with "context." internally
     * @return the value, or null if the key is not found in either source
     */
    public static Object getValue(ExecutionContext ctx,
                                  Map<String, Object> pendingUpdates,
                                  String contextKey) {
        String mongoKey = "context." + contextKey;
        if (pendingUpdates.containsKey(mongoKey)) {
            return pendingUpdates.get(mongoKey);
        }
        return ctx.getMatterContext()
                  .getContext()
                  .get(contextKey);
    }
}
