package com.github.msemys.esjc;

import com.github.msemys.esjc.proto.EventStoreClientMessages.EventRecord;

import java.time.Instant;
import java.util.UUID;

import static com.github.msemys.esjc.util.EmptyArrays.EMPTY_BYTES;
import static com.github.msemys.esjc.util.UUIDConverter.toUUID;
import static java.time.Instant.ofEpochMilli;

/**
 * Represents a previously written event.
 */
public class RecordedEvent {

    /**
     * The event stream that this event belongs to.
     */
    public final String eventStreamId;

    /**
     * The unique identifier representing this event.
     */
    public final UUID eventId;

    /**
     * The number of this event in the stream.
     */
    public final long eventNumber;

    /**
     * The type of event.
     */
    public final String eventType;

    /**
     * A byte array representing the data of this event.
     */
    public final byte[] data;

    /**
     * A byte array representing the metadata associated with this event.
     */
    public final byte[] metadata;

    /**
     * Indicates whether the content is internally marked as JSON.
     */
    public final boolean isJson;

    /**
     * A datetime representing when this event was created in the system.
     */
    public final Instant created;

    /**
     * Creates new instance from proto message.
     *
     * @param eventRecord event record.
     */
    public RecordedEvent(EventRecord eventRecord) {
        eventStreamId = eventRecord.getEventStreamId();

        eventId = toUUID(eventRecord.getEventId().toByteArray());
        eventNumber = eventRecord.getEventNumber();

        eventType = eventRecord.getEventType();

        data = (eventRecord.hasData()) ? eventRecord.getData().toByteArray() : EMPTY_BYTES;
        metadata = (eventRecord.hasMetadata()) ? eventRecord.getMetadata().toByteArray() : EMPTY_BYTES;
        isJson = eventRecord.getDataContentType() == 1;

        created = eventRecord.hasCreatedEpoch() ? ofEpochMilli(eventRecord.getCreatedEpoch()) : null;
    }

}
