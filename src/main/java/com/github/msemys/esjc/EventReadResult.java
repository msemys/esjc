package com.github.msemys.esjc;

import com.github.msemys.esjc.proto.EventStoreClientMessages.ResolvedIndexedEvent;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;

public class EventReadResult {
    public final EventReadStatus status;
    public final String stream;
    public final int eventNumber;
    public final ResolvedEvent event;

    public EventReadResult(EventReadStatus status, String stream, int eventNumber, ResolvedIndexedEvent event) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        this.status = status;
        this.stream = stream;
        this.eventNumber = eventNumber;
        this.event = (status == EventReadStatus.Success) ? new ResolvedEvent(event) : null;
    }
}
