package lt.msemys.esjc;

import lt.msemys.esjc.proto.EventStoreClientMessages;

public class ResolvedEvent {

    public final RecordedEvent event;
    public final RecordedEvent link;
    public final Position originalPosition;

    public ResolvedEvent(EventStoreClientMessages.ResolvedEvent event) {
        this.event = (event.hasEvent()) ? new RecordedEvent(event.getEvent()) : null;
        this.link = (event.hasLink()) ? new RecordedEvent(event.getLink()) : null;
        this.originalPosition = new Position(event.getCommitPosition(), event.getPreparePosition());
    }

    public ResolvedEvent(EventStoreClientMessages.ResolvedIndexedEvent event) {
        this.event = (event.hasEvent()) ? new RecordedEvent(event.getEvent()) : null;
        this.link = (event.hasLink()) ? new RecordedEvent(event.getLink()) : null;
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
