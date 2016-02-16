package com.github.msemys.esjc.system;

public class SystemStreams {
    public static final String STREAMS_STREAM = "$streams";
    public static final String SETTINGS_STREAM = "$settings";
    public static final String STATS_STREAM_PREFIX = "$stats";

    private SystemStreams() {
    }

    public static String metastreamOf(String streamId) {
        return "$$" + streamId;
    }

    public static boolean isMetastream(String streamId) {
        return streamId.startsWith("$$");
    }

    public static String originalStreamOf(String metastreamId) {
        return metastreamId.substring(2);
    }

}
