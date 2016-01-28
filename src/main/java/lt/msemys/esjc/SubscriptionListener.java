package lt.msemys.esjc;

public interface SubscriptionListener {

    void onEvent(ResolvedEvent event);

    default void onClose(SubscriptionDropReason reason, Exception exception) {

    }

}
