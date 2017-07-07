package com.github.msemys.esjc;

import com.github.msemys.esjc.proto.EventStoreClientMessages;

/**
 * Structure represents a single event or an resolved link event.
 */
public class ResolvedEvent {

    /**
     * The event, or the resolved link event if this is a link event.
     */
    public final RecordedEvent event;

    /**
     * The link event if this is a link event.
     */
    public final RecordedEvent link;

    /**
     * The logical position of the original event.
     *
     * @see #originalEvent()
     */
    public final Position originalPosition;

    /**
     * Creates new instance from proto message.
     *
     * @param event resolved event.
     */
    public ResolvedEvent(EventStoreClientMessages.ResolvedEvent event) {
        this.event = (event.hasEvent()) ? new RecordedEvent(event.getEvent()) : null;
        this.link = (event.hasLink()) ? new RecordedEvent(event.getLink()) : null;
        this.originalPosition = new Position(event.getCommitPosition(), event.getPreparePosition());
    }

    /**
     * Creates new instance from proto message.
     *
     * @param event resolved indexed event.
     */
    public ResolvedEvent(EventStoreClientMessages.ResolvedIndexedEvent event) {
        this.event = (event.hasEvent()) ? new RecordedEvent(event.getEvent()) : null;
        this.link = (event.hasLink()) ? new RecordedEvent(event.getLink()) : null;
        this.originalPosition = null;
    }

    /**
     * Indicates whether this event is a resolved link event.
     *
     * @return {@code true} if it is a resolved link event, otherwise {@code false}
     */
    public boolean isResolved() {
        return (link != null) && (event != null);
    }

    /**
     * Gets event that was read or which triggered the subscription.
     *
     * @return the {@link #link} when it represents a link event, otherwise {@link #event}
     */
    public RecordedEvent originalEvent() {
        return (link != null) ? link : event;
    }

    /**
     * The stream name of the original event.
     *
     * @return stream name
     * @see #originalEvent()
     */
    public String originalStreamId() {
        return originalEvent().eventStreamId;
    }

    /**
     * The event number in the stream of original event.
     *
     * @return event number
     * @see #originalEvent()
     */
    public long originalEventNumber() {
        return originalEvent().eventNumber;
    }

}
