package com.github.msemys.esjc.util;

import java.time.Duration;
import java.time.Instant;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;


public class SystemTime {
    private long nanos;

    private SystemTime(long nanos) {
        this.nanos = nanos;
    }

    public void update() {
        nanos = systemNanoTime();
    }

    public long elapsedNanos() {
        return systemNanoTime() - nanos;
    }

    public boolean isElapsed(Duration duration) {
        checkNotNull(duration, "duration is null");
        return elapsedNanos() > duration.toNanos();
    }

    @Override
    public String toString() {
        Instant instant = nanos > 0 ? Instant.now().minusNanos(elapsedNanos()) : Instant.MIN;
        return instant.toString() + "~" + nanos;
    }

    public static SystemTime now() {
        return new SystemTime(systemNanoTime());
    }

    public static SystemTime zero() {
        return new SystemTime(0);
    }

    private static long systemNanoTime() {
        return System.nanoTime();
    }
}
