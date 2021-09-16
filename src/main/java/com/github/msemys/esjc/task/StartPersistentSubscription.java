package com.github.msemys.esjc.task;

import com.github.msemys.esjc.RetryableResolvedEvent;
import com.github.msemys.esjc.Subscription;
import com.github.msemys.esjc.SubscriptionListener;
import com.github.msemys.esjc.UserCredentials;
import com.github.msemys.esjc.subscription.PersistentSubscriptionChannel;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

public class StartPersistentSubscription implements Task {
    public final CompletableFuture<Subscription> result;

    public final String subscriptionId;
    public final String streamId;
    public final int bufferSize;
    public final UserCredentials userCredentials;
    public final SubscriptionListener<PersistentSubscriptionChannel, RetryableResolvedEvent> listener;
    public final int maxRetries;
    public final Duration timeout;

    public StartPersistentSubscription(CompletableFuture<Subscription> result,
                                       String subscriptionId,
                                       String streamId,
                                       int bufferSize,
                                       UserCredentials userCredentials,
                                       SubscriptionListener<PersistentSubscriptionChannel, RetryableResolvedEvent> listener,
                                       int maxRetries,
                                       Duration timeout) {
        checkNotNull(result, "result is null");
        checkNotNull(listener, "listener is null");

        this.result = result;
        this.subscriptionId = subscriptionId;
        this.streamId = streamId;
        this.bufferSize = bufferSize;
        this.userCredentials = userCredentials;
        this.listener = listener;
        this.maxRetries = maxRetries;
        this.timeout = timeout;
    }

    @Override
    public void fail(Exception exception) {
        result.completeExceptionally(exception);
    }

}
