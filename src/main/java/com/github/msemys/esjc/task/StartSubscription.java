package com.github.msemys.esjc.task;

import com.github.msemys.esjc.Subscription;
import com.github.msemys.esjc.UserCredentials;
import com.github.msemys.esjc.VolatileSubscriptionListener;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

public class StartSubscription implements Task {
    public final CompletableFuture<Subscription> result;

    public final String streamId;
    public final boolean resolveLinkTos;
    public final UserCredentials userCredentials;
    public final VolatileSubscriptionListener listener;

    public final int maxRetries;
    public final Duration timeout;

    public StartSubscription(CompletableFuture<Subscription> result,
                             String streamId,
                             boolean resolveLinkTos,
                             UserCredentials userCredentials,
                             VolatileSubscriptionListener listener,
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
