package com.github.msemys.esjc.system;

public enum SystemEventType {

    STREAM_DELETED("$streamDeleted"),

    STATS_COLLECTED("$statsCollected"),

    LINK_TO("$>"),

    STREAM_METADATA("$metadata"),

    SETTINGS("$settings");

    public final String value;

    SystemEventType(String value) {
        this.value = value;
    }
}
