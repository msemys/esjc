package lt.msemys.esjc.subscription;

import java.util.List;
import java.util.UUID;

public interface PersistentSubscriptionProtocol {

    void notifyEventsProcessed(List<UUID> processedEvents);

    void notifyEventsFailed(List<UUID> processedEvents, PersistentSubscriptionNakEventAction action, String reason);

    void unsubscribe();

}
