package com.github.msemys.esjc.task;

import com.github.msemys.esjc.Subscription;
import com.github.msemys.esjc.SubscriptionListener;
import com.github.msemys.esjc.operation.UserCredentials;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

public class StartPersistentSubscription implements Task {
    public final CompletableFuture<Subscription> result;

    public final String subscriptionId;
    public final String streamId;
    public final int bufferSize;
    public final UserCredentials userCredentials;
    public final SubscriptionListener listener;
    public final int maxRetries;
    public final Duration timeout;

    public StartPersistentSubscription(CompletableFuture<Subscription> result,
                                       String subscriptionId,
                                       String streamId,
                                       int bufferSize,
                                       UserCredentials userCredentials,
                                       SubscriptionListener listener,
                                       int maxRetries,
                                       Duration timeout) {
        checkNotNull(result, "result");
        checkNotNull(listener, "listener");

        this.result = result;
        this.subscriptionId = subscriptionId;
        this.streamId = streamId;
        this.bufferSize = bufferSize;
        this.userCredentials = userCredentials;
        this.listener = listener;
        this.maxRetries = maxRetries;
        this.timeout = timeout;
    }

}
