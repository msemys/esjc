package lt.msemys.esjc;

import lt.msemys.esjc.proto.EventStoreClientMessages;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.ClientAPI/AllEventsSlice.cs">EventStore.ClientAPI/AllEventsSlice.cs</a>
 */
public class AllEventsSlice {

    public final ReadDirection readDirection;
    public final Position fromPosition;
    public final Position nextPosition;
    public final List<ResolvedEvent> events;

    public AllEventsSlice(ReadDirection readDirection,
                          Position fromPosition,
                          Position nextPosition,
                          List<EventStoreClientMessages.ResolvedEvent> events) {
        this.readDirection = readDirection;
        this.fromPosition = fromPosition;
        this.nextPosition = nextPosition;
        this.events = (events == null) ? emptyList() : events.stream()
                .map(e -> new ResolvedEvent(e))
                .collect(toCollection(() -> new ArrayList(events.size())));
    }

    public boolean isEndOfStream() {
        return events.isEmpty();
    }

}
