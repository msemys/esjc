package com.github.msemys.esjc.projection;

/**
 * Represents the details for a projection.
 */
public class Projection {

    /**
     * Core processing time
     */
    public final long coreProcessingTime;

    /**
     * Projection version
     */
    public final long version;

    /**
     * The epoch
     */
    public final long epoch;

    /**
     * Projection effective name
     */
    public final String effectiveName;

    /**
     * The number of writes in progress
     */
    public final int writesInProgress;

    /**
     * The number of reads in progress
     */
    public final int readsInProgress;

    /**
     * The number of cached partitions
     */
    public final int partitionsCached;

    /**
     * Projection status
     */
    public final String status;

    /**
     * Projection state reason
     */
    public final String stateReason;

    /**
     * Projection name
     */
    public final String name;

    /**
     * Projection mode
     */
    public final String mode;

    /**
     * Projection position
     */
    public final String position;

    /**
     * Projection progress
     */
    public final float progress;

    /**
     * Last checkpoint
     */
    public final String lastCheckpoint;

    /**
     * The number of events processed after restart
     */
    public final long eventsProcessedAfterRestart;

    /**
     * Projection status URL
     */
    public final String statusUrl;

    /**
     * Projection state URL
     */
    public final String stateUrl;

    /**
     * Projection result URL
     */
    public final String resultUrl;

    /**
     * Projection query URL
     */
    public final String queryUrl;

    /**
     * Projection enable command URL
     */
    public final String enableCommandUrl;

    /**
     * Projection disable command URL
     */
    public final String disableCommandUrl;

    /**
     * Projection checkpoint status
     */
    public final String checkpointStatus;

    /**
     * The number of buffered events
     */
    public final long bufferedEvents;

    /**
     * The number of write pending events before checkpoint
     */
    public final int writePendingEventsBeforeCheckpoint;

    /**
     * The number of write pending events after checkpoint
     */
    public final int writePendingEventsAfterCheckpoint;

    public Projection(long coreProcessingTime,
                      long version,
                      long epoch,
                      String effectiveName,
                      int writesInProgress,
                      int readsInProgress,
                      int partitionsCached,
                      String status,
                      String stateReason,
                      String name,
                      String mode,
                      String position,
                      float progress,
                      String lastCheckpoint,
                      long eventsProcessedAfterRestart,
                      String statusUrl,
                      String stateUrl,
                      String resultUrl,
                      String queryUrl,
                      String enableCommandUrl,
                      String disableCommandUrl,
                      String checkpointStatus,
                      long bufferedEvents,
                      int writePendingEventsBeforeCheckpoint,
                      int writePendingEventsAfterCheckpoint) {
        this.coreProcessingTime = coreProcessingTime;
        this.version = version;
        this.epoch = epoch;
        this.effectiveName = effectiveName;
        this.writesInProgress = writesInProgress;
        this.readsInProgress = readsInProgress;
        this.partitionsCached = partitionsCached;
        this.status = status;
        this.stateReason = stateReason;
        this.name = name;
        this.mode = mode;
        this.position = position;
        this.progress = progress;
        this.lastCheckpoint = lastCheckpoint;
        this.eventsProcessedAfterRestart = eventsProcessedAfterRestart;
        this.statusUrl = statusUrl;
        this.stateUrl = stateUrl;
        this.resultUrl = resultUrl;
        this.queryUrl = queryUrl;
        this.enableCommandUrl = enableCommandUrl;
        this.disableCommandUrl = disableCommandUrl;
        this.checkpointStatus = checkpointStatus;
        this.bufferedEvents = bufferedEvents;
        this.writePendingEventsBeforeCheckpoint = writePendingEventsBeforeCheckpoint;
        this.writePendingEventsAfterCheckpoint = writePendingEventsAfterCheckpoint;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Projection{");
        sb.append("coreProcessingTime=").append(coreProcessingTime);
        sb.append(", version=").append(version);
        sb.append(", epoch=").append(epoch);
        sb.append(", effectiveName='").append(effectiveName).append('\'');
        sb.append(", writesInProgress=").append(writesInProgress);
        sb.append(", readsInProgress=").append(readsInProgress);
        sb.append(", partitionsCached=").append(partitionsCached);
        sb.append(", status='").append(status).append('\'');
        sb.append(", stateReason='").append(stateReason).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", mode='").append(mode).append('\'');
        sb.append(", position='").append(position).append('\'');
        sb.append(", progress=").append(progress);
        sb.append(", lastCheckpoint='").append(lastCheckpoint).append('\'');
        sb.append(", eventsProcessedAfterRestart=").append(eventsProcessedAfterRestart);
        sb.append(", statusUrl='").append(statusUrl).append('\'');
        sb.append(", stateUrl='").append(stateUrl).append('\'');
        sb.append(", resultUrl='").append(resultUrl).append('\'');
        sb.append(", queryUrl='").append(queryUrl).append('\'');
        sb.append(", enableCommandUrl='").append(enableCommandUrl).append('\'');
        sb.append(", disableCommandUrl='").append(disableCommandUrl).append('\'');
        sb.append(", checkpointStatus='").append(checkpointStatus).append('\'');
        sb.append(", bufferedEvents=").append(bufferedEvents);
        sb.append(", writePendingEventsBeforeCheckpoint=").append(writePendingEventsBeforeCheckpoint);
        sb.append(", writePendingEventsAfterCheckpoint=").append(writePendingEventsAfterCheckpoint);
        sb.append('}');
        return sb.toString();
    }
}
