package lt.msemys.esjc;

import lt.msemys.esjc.proto.EventStoreClientMessages;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.time.Instant.ofEpochMilli;
import static lt.msemys.esjc.util.EmptyArrays.EMPTY_BYTES;
import static lt.msemys.esjc.util.UUIDConverter.toUUID;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.ClientAPI/RecordedEvent.cs">EventStore.ClientAPI/RecordedEvent.cs</a>
 */
public class RecordedEvent {

    public final String eventStreamId;
    public final UUID eventId;
    public final int eventNumber;
    public final String eventType;
    public final byte[] data;
    public final byte[] metadata;
    public final boolean isJson;
    public final Optional<Instant> created;

    public RecordedEvent(EventStoreClientMessages.EventRecord systemRecord) {
        eventStreamId = systemRecord.getEventStreamId();

        eventId = toUUID(systemRecord.getEventId().toByteArray());
        eventNumber = systemRecord.getEventNumber();

        eventType = systemRecord.getEventType();

        data = (systemRecord.getData() == null) ? EMPTY_BYTES : systemRecord.getData().toByteArray();
        metadata = (systemRecord.getMetadata() == null) ? EMPTY_BYTES : systemRecord.getMetadata().toByteArray();
        isJson = systemRecord.getDataContentType() == 1;

        created = systemRecord.hasCreatedEpoch() ? Optional.of(ofEpochMilli(systemRecord.getCreatedEpoch())) : Optional.empty();
    }

}
