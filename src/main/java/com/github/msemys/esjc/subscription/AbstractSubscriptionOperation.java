package com.github.msemys.esjc.subscription;

import com.github.msemys.esjc.*;
import com.github.msemys.esjc.operation.*;
import com.github.msemys.esjc.proto.EventStoreClientMessages.NotHandled;
import com.github.msemys.esjc.proto.EventStoreClientMessages.NotHandled.MasterInfo;
import com.github.msemys.esjc.proto.EventStoreClientMessages.SubscriptionDropped;
import com.github.msemys.esjc.proto.EventStoreClientMessages.UnsubscribeFromStream;
import com.github.msemys.esjc.tcp.TcpCommand;
import com.github.msemys.esjc.tcp.TcpFlag;
import com.github.msemys.esjc.tcp.TcpPackage;
import com.github.msemys.esjc.util.Throwables;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.github.msemys.esjc.util.Preconditions.*;
import static com.github.msemys.esjc.util.Strings.defaultIfEmpty;
import static com.github.msemys.esjc.util.Strings.newString;

public abstract class AbstractSubscriptionOperation<T extends Subscription, E extends ResolvedEvent> implements SubscriptionOperation {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSubscriptionOperation.class);

    private static final int MAX_QUEUE_SIZE = 2000;

    private final CompletableFuture<Subscription> result;
    private final TcpCommand subscribeCommand;
    protected final String streamId;
    protected final boolean resolveLinkTos;
    protected final UserCredentials userCredentials;
    protected final SubscriptionListener<T, E> listener;
    protected final Supplier<Channel> connectionSupplier;
    private final Executor executor;
    private final Queue<Runnable> actionQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean actionExecuting = new AtomicBoolean();
    private T subscription;
    private final AtomicBoolean unsubscribed = new AtomicBoolean();
    protected UUID correlationId;

    protected AbstractSubscriptionOperation(CompletableFuture<Subscription> result,
                                            TcpCommand subscribeCommand,
                                            String streamId,
                                            boolean resolveLinkTos,
                                            UserCredentials userCredentials,
                                            SubscriptionListener<T, E> listener,
                                            Supplier<Channel> connectionSupplier,
                                            Executor executor) {
        checkNotNull(result, "result is null");
        checkNotNull(subscribeCommand, "subscribeCommand is null");
        checkNotNull(listener, "listener is null");
        checkNotNull(connectionSupplier, "connectionSupplier is null");
        checkNotNull(executor, "executor is null");

        this.result = result;
        this.subscribeCommand = subscribeCommand;
        this.streamId = streamId;
        this.resolveLinkTos = resolveLinkTos;
        this.userCredentials = userCredentials;
        this.listener = listener;
        this.connectionSupplier = connectionSupplier;
        this.executor = executor;
    }

    protected abstract MessageLite createSubscribeMessage();

    protected abstract T createSubscription(long lastCommitPosition, Long lastEventNumber);

    protected abstract boolean inspect(TcpPackage tcpPackage, InspectionResult.Builder builder);

    @Override
    public boolean subscribe(UUID correlationId, Channel connection) {
        checkNotNull(connection, "connection is null");

        if (subscription != null || unsubscribed.get()) {
            return false;
        } else {
            this.correlationId = correlationId;
            connection.writeAndFlush(TcpPackage.newBuilder()
                .command(subscribeCommand)
                .flag(userCredentials != null ? TcpFlag.Authenticated : TcpFlag.None)
                .correlationId(correlationId)
                .login(userCredentials != null ? userCredentials.username : null)
                .password(userCredentials != null ? userCredentials.password : null)
                .data(createSubscribeMessage().toByteArray())
                .build());
            return true;
        }
    }

    @Override
    public void drop(SubscriptionDropReason reason, Exception exception, Channel connection) {
        if (unsubscribed.compareAndSet(false, true)) {
            logger.trace("Subscription {} to {}: closing subscription, reason: {}", correlationId, streamId(), reason, exception);

            if (reason != SubscriptionDropReason.UserInitiated) {
                checkNotNull(exception, "No exception provided for subscription drop reason '%s", reason);
                result.completeExceptionally(exception);
            }

            if (reason == SubscriptionDropReason.UserInitiated && subscription != null && connection != null) {
                connection.writeAndFlush(TcpPackage.newBuilder()
                    .command(TcpCommand.UnsubscribeFromStream)
                    .correlationId(correlationId)
                    .data(UnsubscribeFromStream.getDefaultInstance().toByteArray())
                    .build());
            }

            if (subscription != null) {
                action(() -> listener.onClose(subscription, reason, exception));
            }
        }
    }

    @Override
    public InspectionResult inspect(TcpPackage tcpPackage) {
        try {
            InspectionResult.Builder builder = InspectionResult.newBuilder();
            if (inspect(tcpPackage, builder)) {
                return builder.build();
            }

            switch (tcpPackage.command) {
                case SubscriptionDropped:
                    SubscriptionDropped subscriptionDropped = newInstance(SubscriptionDropped.getDefaultInstance(), tcpPackage.data);
                    switch (subscriptionDropped.getReason()) {
                        case Unsubscribed:
                            drop(SubscriptionDropReason.UserInitiated, null);
                            break;
                        case AccessDenied:
                            drop(SubscriptionDropReason.AccessDenied,
                                new AccessDeniedException(String.format("Subscription to '%s' failed due to access denied.", streamId())));
                            break;
                        case NotFound:
                            drop(SubscriptionDropReason.NotFound,
                                new IllegalArgumentException(String.format("Subscription to '%s' failed due to not found.", streamId())));
                            break;
                        default:
                            logger.trace("Subscription dropped by server. Reason: {}.", subscriptionDropped.getReason());
                            drop(SubscriptionDropReason.Unknown,
                                new CommandNotExpectedException(String.format("Unsubscribe reason: '%s'.", subscriptionDropped.getReason())));
                            break;
                    }
                    return InspectionResult.newBuilder()
                        .decision(InspectionDecision.EndOperation)
                        .description(String.format("SubscriptionDropped: %s", subscriptionDropped.getReason()))
                        .build();
                case NotAuthenticated:
                    drop(SubscriptionDropReason.NotAuthenticated,
                        new NotAuthenticatedException(defaultIfEmpty(newString(tcpPackage.data), "Authentication error")));
                    return InspectionResult.newBuilder()
                        .decision(InspectionDecision.EndOperation)
                        .description("NotAuthenticated")
                        .build();
                case BadRequest:
                    drop(SubscriptionDropReason.ServerError,
                        new ServerErrorException(defaultIfEmpty(newString(tcpPackage.data), "<no message>")));
                    return InspectionResult.newBuilder()
                        .decision(InspectionDecision.EndOperation)
                        .description("BadRequest: " + newString(tcpPackage.data))
                        .build();
                case NotHandled:
                    checkState(subscription == null, "NotHandled command appeared while we were already subscribed.");
                    NotHandled notHandled = newInstance(NotHandled.getDefaultInstance(), tcpPackage.data);
                    switch (notHandled.getReason()) {
                        case NotReady:
                            return InspectionResult.newBuilder()
                                .decision(InspectionDecision.Retry)
                                .description("NotHandled - NotReady")
                                .build();
                        case TooBusy:
                            return InspectionResult.newBuilder()
                                .decision(InspectionDecision.Retry)
                                .description("NotHandled - TooBusy")
                                .build();
                        case NotMaster:
                            MasterInfo masterInfo = newInstance(MasterInfo.getDefaultInstance(), notHandled.getAdditionalInfo().toByteArray());
                            return InspectionResult.newBuilder()
                                .decision(InspectionDecision.Reconnect)
                                .description("NotHandled - NotMaster")
                                .address(masterInfo.getExternalTcpAddress(), masterInfo.getExternalTcpPort())
                                .secureAddress(masterInfo.getExternalSecureTcpAddress(), masterInfo.getExternalSecureTcpPort())
                                .build();
                        default:
                            logger.error("Unknown NotHandledReason: {}.", notHandled.getReason());
                            return InspectionResult.newBuilder()
                                .decision(InspectionDecision.Retry)
                                .description("NotHandled - <unknown>")
                                .build();
                    }
                default:
                    drop(SubscriptionDropReason.ServerError,
                        new CommandNotExpectedException(tcpPackage.command.toString()));
                    return InspectionResult.newBuilder()
                        .decision(InspectionDecision.EndOperation)
                        .description(tcpPackage.command.toString())
                        .build();
            }
        } catch (Exception e) {
            drop(SubscriptionDropReason.Unknown, e);
            return InspectionResult.newBuilder()
                .decision(InspectionDecision.EndOperation)
                .description("Exception - " + e.getMessage())
                .build();
        }
    }

    @Override
    public void connectionClosed() {
        drop(SubscriptionDropReason.ConnectionClosed, new ConnectionClosedException("Connection was closed."));
    }

    public void unsubscribe() {
        drop(SubscriptionDropReason.UserInitiated, null, connectionSupplier.get());
    }

    protected void confirmSubscription(long lastCommitPosition, Long lastEventNumber) {
        checkArgument(lastCommitPosition >= -1, "Invalid lastCommitPosition %d on subscription confirmation.", lastCommitPosition);
        checkState(subscription == null, "Double confirmation of subscription.");

        logger.trace("Subscription {} to {}: subscribed at CommitPosition: {}, EventNumber: {}.",
            correlationId, streamId(), lastCommitPosition, lastEventNumber);

        subscription = createSubscription(lastCommitPosition, lastEventNumber);
        result.complete(subscription);
    }

    protected void eventAppeared(E event) {
        if (!unsubscribed.get()) {
            checkNotNull(subscription, "Subscription not confirmed, but event appeared!");

            logger.trace("Subscription {} to {}: event appeared ({}, {}, {} @ {}).",
                correlationId, streamId(), event.originalStreamId(), event.originalEventNumber(), event.originalEvent().eventType, event.originalPosition);

            action(() -> listener.onEvent(subscription, event));
        }
    }

    protected void send(TcpPackage tcpPackage) {
        connectionSupplier.get().writeAndFlush(tcpPackage);
    }

    private String streamId() {
        return defaultIfEmpty(streamId, "<all>");
    }

    private void action(Runnable action) {
        actionQueue.offer(action);

        if (actionQueue.size() > MAX_QUEUE_SIZE) {
            drop(SubscriptionDropReason.ProcessingQueueOverflow, new SubscriptionBufferOverflowException("client buffer too big"));
        }

        if (actionExecuting.compareAndSet(false, true)) {
            executor.execute(this::run);
        }
    }

    private void run() {
        do {
            Runnable action;

            while ((action = actionQueue.poll()) != null) {
                action.run();
            }

            actionExecuting.set(false);
        } while (!actionQueue.isEmpty() && actionExecuting.compareAndSet(false, true));
    }

    @SuppressWarnings("unchecked")
    protected static <R extends MessageLite> R newInstance(R message, byte[] data) {
        try {
            return (R) message.getParserForType().parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw Throwables.propagate(e);
        }
    }

}
