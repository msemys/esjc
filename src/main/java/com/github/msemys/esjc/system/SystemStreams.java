package com.github.msemys.esjc.system;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;

public class SystemStreams {
    public static final String STREAMS_STREAM = "$streams";
    public static final String SETTINGS_STREAM = "$settings";
    public static final String STATS_STREAM_PREFIX = "$stats";
    public static final String METASTREAM_PREFIX = "$$";

    private SystemStreams() {
    }

    public static String metastreamOf(String streamId) {
        return METASTREAM_PREFIX + streamId;
    }

    public static boolean isMetastream(String streamId) {
        return streamId.startsWith(METASTREAM_PREFIX);
    }

    public static String originalStreamOf(String metastreamId) {
        checkArgument(isMetastream(metastreamId), "'%s' is not metastream", metastreamId);
        return metastreamId.substring(METASTREAM_PREFIX.length());
    }

}
