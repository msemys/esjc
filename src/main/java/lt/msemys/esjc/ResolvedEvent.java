package lt.msemys.esjc;

import lt.msemys.esjc.proto.EventStoreClientMessages;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.ClientAPI/ResolvedEvent.cs">EventStore.ClientAPI/ResolvedEvent.cs</a>
 */
public class ResolvedEvent {

    public final RecordedEvent event;
    public final RecordedEvent link;
    public final Position originalPosition;

    public ResolvedEvent(EventStoreClientMessages.ResolvedEvent event) {
        this.event = (event.getEvent() == null) ? null : new RecordedEvent(event.getEvent());
        this.link = (event.getLink() == null) ? null : new RecordedEvent(event.getLink());
        this.originalPosition = new Position(event.getCommitPosition(), event.getPreparePosition());
    }

    public ResolvedEvent(EventStoreClientMessages.ResolvedIndexedEvent event) {
        this.event = (event.getEvent() == null) ? null : new RecordedEvent(event.getEvent());
        this.link = (event.getLink() == null) ? null : new RecordedEvent(event.getLink());
        this.originalPosition = null;
    }

    public boolean isResolved() {
        return (link != null) && (event != null);
    }

    public RecordedEvent originalEvent() {
        return (link != null) ? link : event;
    }

    public String originalStreamId() {
        return originalEvent().eventStreamId;
    }

    public int originalEventNumber() {
        return originalEvent().eventNumber;
    }

}
