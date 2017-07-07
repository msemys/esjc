package com.github.msemys.esjc;

/**
 * Expected version - optimistic concurrency check for stream write operations.
 */
public class ExpectedVersion {

    /**
     * Specifies the expectation that target stream does not yet exist.
     */
    public static final ExpectedVersion NO_STREAM = new ExpectedVersion(-1);

    /**
     * Disables the optimistic concurrency check (idempotence is not guaranteed).
     */
    public static final ExpectedVersion ANY = new ExpectedVersion(-2);

    /**
     * Specifies the expectation that stream should exist (idempotence is not guaranteed).
     * If it or a metadata stream does not exist treat that as a concurrency problem.
     */
    public static final ExpectedVersion STREAM_EXISTS = new ExpectedVersion(-4);

    /**
     * Stream version value
     */
    public final long value;

    private ExpectedVersion(long value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExpectedVersion that = (ExpectedVersion) o;

        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    /**
     * Creates expected version of the specified event number (idempotence is guaranteed).
     *
     * @param eventNumber event number that you expect the target stream to currently be at.
     * @return expected version of the specified event number.
     */
    public static ExpectedVersion of(long eventNumber) {
        return new ExpectedVersion(eventNumber);
    }

}
