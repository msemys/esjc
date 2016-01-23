package lt.msemys.esjc;

import lt.msemys.esjc.util.EmptyArrays;

import java.util.UUID;

public class EventData {
    public final UUID eventId;
    public final String type;
    public final boolean isJson;
    public final byte[] data;
    public final byte[] metadata;

    public EventData(UUID eventId, String type, boolean isJson, byte[] data, byte[] metadata) {
        this.eventId = eventId;
        this.type = type;
        this.isJson = isJson;
        this.data = (data == null) ? EmptyArrays.EMPTY_BYTES : data;
        this.metadata = (metadata == null) ? EmptyArrays.EMPTY_BYTES : metadata;
    }
}
