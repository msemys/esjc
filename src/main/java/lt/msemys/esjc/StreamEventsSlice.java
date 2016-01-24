package lt.msemys.esjc;

import lt.msemys.esjc.proto.EventStoreClientMessages.ResolvedIndexedEvent;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;
import static lt.msemys.esjc.util.Preconditions.checkArgument;
import static lt.msemys.esjc.util.Strings.isNullOrEmpty;

public class StreamEventsSlice {
    public final SliceReadStatus status;
    public final String stream;
    public final int fromEventNumber;
    public final ReadDirection readDirection;
    public final List<ResolvedEvent> events;
    public final int nextEventNumber;
    public final int lastEventNumber;
    public final boolean isEndOfStream;

    public StreamEventsSlice(SliceReadStatus status,
                             String stream,
                             int fromEventNumber,
                             ReadDirection readDirection,
                             List<ResolvedIndexedEvent> events,
                             int nextEventNumber,
                             int lastEventNumber,
                             boolean isEndOfStream) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        this.status = status;
        this.stream = stream;
        this.fromEventNumber = fromEventNumber;
        this.readDirection = readDirection;
        this.events = (events == null) ? emptyList() : events.stream()
            .map(ResolvedEvent::new)
            .collect(toCollection(() -> new ArrayList(events.size())));
        this.nextEventNumber = nextEventNumber;
        this.lastEventNumber = lastEventNumber;
        this.isEndOfStream = isEndOfStream;
    }
}
