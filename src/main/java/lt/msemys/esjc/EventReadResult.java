package lt.msemys.esjc;

import lt.msemys.esjc.proto.EventStoreClientMessages.ResolvedIndexedEvent;

import static lt.msemys.esjc.util.Preconditions.checkArgument;
import static lt.msemys.esjc.util.Strings.isNullOrEmpty;

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
