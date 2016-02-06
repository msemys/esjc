package lt.msemys.esjc.task;

import lt.msemys.esjc.Subscription;
import lt.msemys.esjc.SubscriptionListener;
import lt.msemys.esjc.operation.UserCredentials;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;

public class StartSubscription implements Task {
    public final CompletableFuture<Subscription> result;

    public final String streamId;
    public final boolean resolveLinkTos;
    public final UserCredentials userCredentials;
    public final SubscriptionListener listener;

    public final int maxRetries;
    public final Duration timeout;

    public StartSubscription(CompletableFuture<Subscription> result,
                             String streamId,
                             boolean resolveLinkTos,
                             UserCredentials userCredentials,
                             SubscriptionListener listener,
                             int maxRetries,
                             Duration timeout) {
        checkNotNull(result, "result");
        checkNotNull(listener, "listener");

        this.result = result;
        this.streamId = streamId;
        this.resolveLinkTos = resolveLinkTos;
        this.userCredentials = userCredentials;
        this.listener = listener;
        this.maxRetries = maxRetries;
        this.timeout = timeout;
    }

}
