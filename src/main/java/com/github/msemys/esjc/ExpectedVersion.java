package com.github.msemys.esjc;

/**
 * Expected version constants.
 * <p>
 * The use of expected version can be a bit tricky especially when discussing idempotency assurances
 * given by the Event Store. There are three possible constant values that can be used for the
 * passing of an expected version:
 * <ul>
 * <li>{@link ExpectedVersion#NO_STREAM} - says that the stream should not exist when doing your write.</li>
 * <li>{@link ExpectedVersion#ANY} - says that you should not conflict with anything.</li>
 * <li>{@link ExpectedVersion#STREAM_EXISTS} - says the stream or a metadata stream should exist when doing your write.</li>
 * </ul>
 * Any other value states that the last event written to the stream should have a sequence number
 * matching your expected value.
 * <p>
 * The Event Store will assure idempotency for all operations using any value, except for
 * {@link ExpectedVersion#ANY} and {@link ExpectedVersion#STREAM_EXISTS}. When using {@link ExpectedVersion#ANY} or
 * {@link ExpectedVersion#STREAM_EXISTS} the Event Store will do its best to assure idempotency but
 * will not guarantee idempotency.
 */
public class ExpectedVersion {

    /**
     * Specifies the expectation that target stream does not yet exist.
     */
    public static final long NO_STREAM = -1;

    /**
     * Disables the optimistic concurrency check (idempotence is not guaranteed).
     */
    public static final long ANY = -2;

    /**
     * Specifies the expectation that stream or a metadata stream should exist (idempotence is not guaranteed).
     */
    public static final long STREAM_EXISTS = -4;

    private ExpectedVersion() {
    }

}
