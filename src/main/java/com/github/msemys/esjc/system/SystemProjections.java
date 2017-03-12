package com.github.msemys.esjc.system;

/**
 * System built-in projections.
 * <p>
 * <b>Note:</b> when you start Event Store from a fresh database, these projections are present but disabled.
 */
public class SystemProjections {

    /**
     * Projection that links existing events from streams to a new stream with a {@code $ce-} prefix (a category)
     * by splitting a stream id by a configurable separator.
     * <p>
     * <b>Note:</b> edit this projection to provide your own values, such as where to split the stream id and separator.
     */
    public static final String BY_CATEGORY = "$by_category";

    /**
     * Projection that links existing events from streams to a new stream with a {@code $et-} prefix.
     * <p>
     * <b>Note:</b> this projection cannot be configured.
     */
    public static final String BY_EVENT_TYPE = "$by_event_type";

    /**
     * Projection that links existing events from streams to a new stream with a {@code $category-} prefix
     * by splitting a stream id by a configurable separator.
     * <p>
     * <b>Note:</b> edit this projection to provide your own values, such as where to split the stream id and separator.
     */
    public static final String STREAM_BY_CATEGORY = "$stream_by_category";

    /**
     * Projection that links existing events from streams to a stream named $streams.
     * <p>
     * <b>Note:</b> this projection cannot be configured.
     */
    public static final String STREAMS = "$streams";

    private SystemProjections() {
    }

}
