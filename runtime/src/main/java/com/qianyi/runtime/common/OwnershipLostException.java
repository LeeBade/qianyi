package com.qianyi.runtime.common;

/**
 * Thrown when a MongoDB write operation is rejected because the
 * queueId or epoch filter no longer matches. Indicates that the
 * current Runtime instance has lost ownership of this Matter.
 *
 * The calling virtual thread MUST terminate immediately.
 * No retry, no recovery attempt is permitted.
 *
 * @author TianJunQi
 * @since 2026-06-29
 */
public final class OwnershipLostException extends RuntimeException {

    private final String matterId;

    public OwnershipLostException(String matterId, String detail) {
        super(String.format("Matter[%s]: ownership lost — %s", matterId, detail));
        this.matterId = matterId;
    }

    /**
     * @return the matter whose ownership was lost
     */
    public String getMatterId() {
        return matterId;
    }

    /**
     * Suppress stack trace for performance.
     * This is a normal distributed-systems condition, not a code defect.
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
