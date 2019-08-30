package com.github.msemys.esjc.subscription;

import com.github.msemys.esjc.*;
import com.github.msemys.esjc.operation.AccessDeniedException;
import com.github.msemys.esjc.operation.InspectionDecision;
import com.github.msemys.esjc.operation.InspectionResult;
import com.github.msemys.esjc.proto.EventStoreClientMessages.*;
import com.github.msemys.esjc.proto.EventStoreClientMessages.PersistentSubscriptionNakEvents.NakAction;
import com.github.msemys.esjc.tcp.TcpCommand;
import com.github.msemys.esjc.tcp.TcpFlag;
import com.github.msemys.esjc.tcp.TcpPackage;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import io.netty.channel.Channel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static com.github.msemys.esjc.util.UUIDConverter.toBytes;
import static java.util.stream.Collectors.toCollection;

public class PersistentSubscriptionOperation extends AbstractSubscriptionOperation<PersistentSubscriptionChannel, RetryableResolvedEvent> implements PersistentSubscriptionProtocol {
    private final String groupName;
    private final int bufferSize;
    private String subscriptionId;

    public PersistentSubscriptionOperation(CompletableFuture<Subscription> result,
                                           String groupName,
                                           String streamId,
                                           int bufferSize,
                                           UserCredentials userCredentials,
                                           SubscriptionListener<PersistentSubscriptionChannel, RetryableResolvedEvent> listener,
                                           Supplier<Channel> connectionSupplier,
                                           Duration maxCongestionWaitTime,
                                           Executor executor) {
        super(result, TcpCommand.ConnectToPersistentSubscription, streamId, false, userCredentials, listener, connectionSupplier, maxCongestionWaitTime, executor);
        this.groupName = groupName;
        this.bufferSize = bufferSize;
    }

    @Override
    protected MessageLite createSubscribeMessage() {
        return ConnectToPersistentSubscription.newBuilder()
            .setSubscriptionId(groupName)
            .setEventStreamId(streamId)
            .setAllowedInFlightMessages(bufferSize)
            .build();
    }

    @Override
    protected PersistentSubscriptionChannel createSubscription(long lastCommitPosition, Long lastEventNumber) {
        return new PersistentSubscriptionChannel(this, streamId, lastCommitPosition, lastEventNumber);
    }

    @Override
    protected boolean inspect(TcpPackage tcpPackage, InspectionResult.Builder builder) {
        switch (tcpPackage.command) {
            case PersistentSubscriptionConfirmation:
                PersistentSubscriptionConfirmation confirmation = newInstance(PersistentSubscriptionConfirmation.getDefaultInstance(), tcpPackage.data);
                confirmSubscription(confirmation.getLastCommitPosition(), confirmation.hasLastEventNumber() ? confirmation.getLastEventNumber() : null);
                builder.decision(InspectionDecision.Subscribed).description("SubscriptionConfirmation");
                subscriptionId = confirmation.getSubscriptionId();
                return true;
            case PersistentSubscriptionStreamEventAppeared:
                PersistentSubscriptionStreamEventAppeared streamEventAppeared = newInstance(PersistentSubscriptionStreamEventAppeared.getDefaultInstance(), tcpPackage.data);
                eventAppeared(new RetryableResolvedEvent(streamEventAppeared.getEvent(), streamEventAppeared.hasRetryCount() ? streamEventAppeared.getRetryCount() : null));
                builder.decision(InspectionDecision.DoNothing).description("StreamEventAppeared");
                return true;
            case SubscriptionDropped:
                SubscriptionDropped subscriptionDropped = newInstance(SubscriptionDropped.getDefaultInstance(), tcpPackage.data);
                switch (subscriptionDropped.getReason()) {
                    case AccessDenied:
                        drop(SubscriptionDropReason.AccessDenied, new AccessDeniedException("You do not have access to the stream."));
                        builder.decision(InspectionDecision.EndOperation).description("SubscriptionDropped");
                        return true;
                    case NotFound:
                        drop(SubscriptionDropReason.NotFound, new IllegalArgumentException("Subscription not found"));
                        builder.decision(InspectionDecision.EndOperation).description("SubscriptionDropped");
                        return true;
                    case PersistentSubscriptionDeleted:
                        drop(SubscriptionDropReason.PersistentSubscriptionDeleted, new PersistentSubscriptionDeletedException());
                        builder.decision(InspectionDecision.EndOperation).description("SubscriptionDropped");
                        return true;
                    case SubscriberMaxCountReached:
                        drop(SubscriptionDropReason.MaxSubscribersReached, new MaximumSubscribersReachedException());
                        builder.decision(InspectionDecision.EndOperation).description("SubscriptionDropped");
                        return true;
                    case Unsubscribed:
                        drop(SubscriptionDropReason.UserInitiated, null, connectionSupplier.get());
                        builder.decision(InspectionDecision.EndOperation).description("SubscriptionDropped");
                        return true;
                }
        }
        return false;
    }

    @Override
    public void notifyEventsProcessed(List<UUID> processedEvents) {
        checkNotNull(processedEvents, "processedEvents is null");

        PersistentSubscriptionAckEvents message = PersistentSubscriptionAckEvents.newBuilder()
            .setSubscriptionId(subscriptionId)
            .addAllProcessedEventIds(processedEvents.stream()
                .map(uuid -> ByteString.copyFrom(toBytes(uuid)))
                .collect(toCollection(() -> new ArrayList<>(processedEvents.size()))))
            .build();

        send(TcpPackage.newBuilder()
            .command(TcpCommand.PersistentSubscriptionAckEvents)
            .flag(userCredentials != null ? TcpFlag.Authenticated : TcpFlag.None)
            .correlationId(correlationId)
            .login(userCredentials != null ? userCredentials.username : null)
            .password(userCredentials != null ? userCredentials.password : null)
            .data(message.toByteArray())
            .build());
    }

    @Override
    public void notifyEventsFailed(List<UUID> processedEvents, PersistentSubscriptionNakEventAction action, String reason) {
        checkNotNull(processedEvents, "processedEvents is null");
        checkNotNull(reason, "reason is null");

        PersistentSubscriptionNakEvents message = PersistentSubscriptionNakEvents.newBuilder()
            .setSubscriptionId(subscriptionId)
            .addAllProcessedEventIds(processedEvents.stream()
                .map(uuid -> ByteString.copyFrom(toBytes(uuid)))
                .collect(toCollection(() -> new ArrayList<>(processedEvents.size()))))
            .setMessage(reason)
            .setAction(NakAction.valueOf(action.name()))
            .build();

        send(TcpPackage.newBuilder()
            .command(TcpCommand.PersistentSubscriptionNakEvents)
            .flag(userCredentials != null ? TcpFlag.Authenticated : TcpFlag.None)
            .correlationId(correlationId)
            .login(userCredentials != null ? userCredentials.username : null)
            .password(userCredentials != null ? userCredentials.password : null)
            .data(message.toByteArray())
            .build());
    }

}
