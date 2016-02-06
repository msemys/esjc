package lt.msemys.esjc;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.ScheduledFuture;
import lt.msemys.esjc.event.Events;
import lt.msemys.esjc.node.EndPointDiscoverer;
import lt.msemys.esjc.node.NodeEndPoints;
import lt.msemys.esjc.node.cluster.ClusterDnsEndPointDiscoverer;
import lt.msemys.esjc.node.static_.StaticEndPointDiscoverer;
import lt.msemys.esjc.operation.*;
import lt.msemys.esjc.operation.manager.OperationItem;
import lt.msemys.esjc.subscription.AllCatchUpSubscription;
import lt.msemys.esjc.subscription.StreamCatchUpSubscription;
import lt.msemys.esjc.subscription.VolatileSubscription;
import lt.msemys.esjc.subscription.VolatileSubscriptionOperation;
import lt.msemys.esjc.subscription.manager.SubscriptionItem;
import lt.msemys.esjc.task.*;
import lt.msemys.esjc.tcp.ChannelId;
import lt.msemys.esjc.tcp.TcpPackage;
import lt.msemys.esjc.transaction.TransactionManager;
import lt.msemys.esjc.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static lt.msemys.esjc.tcp.handler.AuthenticationHandler.AuthenticationStatus;
import static lt.msemys.esjc.util.Preconditions.checkArgument;
import static lt.msemys.esjc.util.Preconditions.checkNotNull;
import static lt.msemys.esjc.util.Strings.*;

public class EventStore extends AbstractEventStore {
    private static final Logger logger = LoggerFactory.getLogger(EventStore.class);

    protected static final int MAX_READ_SIZE = 4 * 1024;

    private enum ConnectingPhase {INVALID, RECONNECTING, ENDPOINT_DISCOVERY, CONNECTION_ESTABLISHING, AUTHENTICATION, CONNECTED}

    private volatile ScheduledFuture timer;
    private final TransactionManager transactionManager = new TransactionManagerImpl();
    private final TaskQueue tasks;
    private final EndPointDiscoverer discoverer;
    private final ReconnectionInfo reconnectionInfo = new ReconnectionInfo();
    private volatile ConnectingPhase connectingPhase = ConnectingPhase.INVALID;
    private Instant lastOperationTimeoutCheck = Instant.MIN;

    public EventStore(Settings settings) {
        super(settings);

        if (settings.staticNodeSettings.isPresent()) {
            discoverer = new StaticEndPointDiscoverer(settings.staticNodeSettings.get(), settings.ssl);
        } else if (settings.clusterNodeSettings.isPresent()) {
            discoverer = new ClusterDnsEndPointDiscoverer(settings.clusterNodeSettings.get());
        } else {
            throw new IllegalStateException("Node settings not found");
        }

        tasks = new TaskQueue(executor);
        tasks.register(StartConnection.class, this::handle);
        tasks.register(CloseConnection.class, this::handle);
        tasks.register(EstablishTcpConnection.class, this::handle);
        tasks.register(StartOperation.class, this::handle);
        tasks.register(StartSubscription.class, this::handle);
    }

    @Override
    public CompletableFuture<DeleteResult> deleteStream(String stream,
                                                        ExpectedVersion expectedVersion,
                                                        boolean hardDelete,
                                                        UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkNotNull(expectedVersion, "expectedVersion");

        CompletableFuture<DeleteResult> result = new CompletableFuture<>();
        enqueue(new DeleteStreamOperation(result, settings.requireMaster, stream, expectedVersion.value, hardDelete, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<WriteResult> appendToStream(String stream,
                                                         ExpectedVersion expectedVersion,
                                                         Iterable<EventData> events,
                                                         UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkNotNull(expectedVersion, "expectedVersion");
        checkNotNull(events, "events");

        CompletableFuture<WriteResult> result = new CompletableFuture<>();
        enqueue(new AppendToStreamOperation(result, settings.requireMaster, stream, expectedVersion.value, events, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<Transaction> startTransaction(String stream,
                                                           ExpectedVersion expectedVersion,
                                                           UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkNotNull(expectedVersion, "expectedVersion");

        CompletableFuture<Transaction> result = new CompletableFuture<>();
        enqueue(new StartTransactionOperation(result, settings.requireMaster, stream, expectedVersion.value, transactionManager, userCredentials));
        return result;
    }

    @Override
    public Transaction continueTransaction(long transactionId, UserCredentials userCredentials) {
        return new Transaction(transactionId, userCredentials, transactionManager);
    }

    @Override
    public CompletableFuture<EventReadResult> readEvent(String stream,
                                                        int eventNumber,
                                                        boolean resolveLinkTos,
                                                        UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkArgument(eventNumber > -1, "Event number out of range");

        CompletableFuture<EventReadResult> result = new CompletableFuture<>();
        enqueue(new ReadEventOperation(result, stream, eventNumber, resolveLinkTos, settings.requireMaster, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<StreamEventsSlice> readStreamEventsForward(String stream,
                                                                        int start,
                                                                        int count,
                                                                        boolean resolveLinkTos,
                                                                        UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkArgument(start >= 0, "start should be non negative.");
        checkArgument(count > 0, "count should be positive.");
        checkArgument(count < MAX_READ_SIZE, String.format("Count should be less than %d. For larger reads you should page.", MAX_READ_SIZE));

        CompletableFuture<StreamEventsSlice> result = new CompletableFuture<>();
        enqueue(new ReadStreamEventsForwardOperation(result, stream, start, count, resolveLinkTos, settings.requireMaster, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<StreamEventsSlice> readStreamEventsBackward(String stream,
                                                                         int start,
                                                                         int count,
                                                                         boolean resolveLinkTos,
                                                                         UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkArgument(count > 0, "count should be positive.");
        checkArgument(count < MAX_READ_SIZE, String.format("Count should be less than %d. For larger reads you should page.", MAX_READ_SIZE));

        CompletableFuture<StreamEventsSlice> result = new CompletableFuture<>();
        enqueue(new ReadStreamEventsBackwardOperation(result, stream, start, count, resolveLinkTos, settings.requireMaster, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<AllEventsSlice> readAllEventsForward(Position position,
                                                                  int maxCount,
                                                                  boolean resolveLinkTos,
                                                                  UserCredentials userCredentials) {
        checkArgument(maxCount > 0, "Count should be positive.");
        checkArgument(maxCount < MAX_READ_SIZE, String.format("Count should be less than %d. For larger reads you should page.", MAX_READ_SIZE));

        CompletableFuture<AllEventsSlice> result = new CompletableFuture<>();
        enqueue(new ReadAllEventsForwardOperation(result, position, maxCount, resolveLinkTos, settings.requireMaster, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<AllEventsSlice> readAllEventsBackward(Position position,
                                                                   int maxCount,
                                                                   boolean resolveLinkTos,
                                                                   UserCredentials userCredentials) {
        checkArgument(maxCount > 0, "Count should be positive.");
        checkArgument(maxCount < MAX_READ_SIZE, String.format("Count should be less than %d. For larger reads you should page.", MAX_READ_SIZE));

        CompletableFuture<AllEventsSlice> result = new CompletableFuture<>();
        enqueue(new ReadAllEventsBackwardOperation(result, position, maxCount, resolveLinkTos, settings.requireMaster, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<VolatileSubscription> subscribeToStream(String stream,
                                                                     boolean resolveLinkTos,
                                                                     SubscriptionListener listener,
                                                                     UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkNotNull(listener, "listener");

        CompletableFuture<VolatileSubscription> result = new CompletableFuture<>();
        enqueue(new StartSubscription(result, stream, resolveLinkTos, userCredentials, listener, settings.maxOperationRetries, settings.operationTimeout));
        return result;
    }

    @Override
    public CompletableFuture<VolatileSubscription> subscribeToAll(boolean resolveLinkTos,
                                                                  SubscriptionListener listener,
                                                                  UserCredentials userCredentials) {
        checkNotNull(listener, "listener");

        CompletableFuture<VolatileSubscription> result = new CompletableFuture<>();
        enqueue(new StartSubscription(result, Strings.EMPTY, resolveLinkTos, userCredentials, listener, settings.maxOperationRetries, settings.operationTimeout));
        return result;
    }

    @Override
    public CatchUpSubscription subscribeToStreamFrom(String stream,
                                                     Integer lastCheckpoint,
                                                     boolean resolveLinkTos,
                                                     CatchUpSubscriptionListener listener,
                                                     UserCredentials userCredentials,
                                                     int readBatchSize) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkNotNull(listener, "listener");

        CatchUpSubscription subscription = new StreamCatchUpSubscription(this,
            stream, lastCheckpoint, resolveLinkTos, listener, userCredentials, readBatchSize, settings.maxPushQueueSize, executor);

        subscription.start();

        return subscription;
    }

    @Override
    public CatchUpSubscription subscribeToAllFrom(Position fromPositionExclusive,
                                                  boolean resolveLinkTos,
                                                  CatchUpSubscriptionListener listener,
                                                  UserCredentials userCredentials,
                                                  int readBatchSize) {
        checkNotNull(listener, "listener");

        CatchUpSubscription subscription = new AllCatchUpSubscription(this,
            fromPositionExclusive, resolveLinkTos, listener, userCredentials, readBatchSize, settings.maxPushQueueSize, executor);

        subscription.start();

        return subscription;
    }

    @Override
    protected void onAuthenticationCompleted(AuthenticationStatus status) {
        if (status == AuthenticationStatus.SUCCESS || status == AuthenticationStatus.IGNORED) {
            gotoConnectedPhase();
        } else {
            fireEvent(Events.authenticationFailed());
        }
    }

    @Override
    protected void onBadRequest(TcpPackage tcpPackage) {
        handle(new CloseConnection("Connection-wide BadRequest received. Too dangerous to continue.",
            new EventStoreException("Bad request received from server. Error: " + defaultIfEmpty(newString(tcpPackage.data), "<no message>"))));
    }

    @Override
    protected void onReconnect(NodeEndPoints nodeEndPoints) {
        reconnectTo(nodeEndPoints);
    }

    public void connect() {
        if (!isTimerTicking()) {
            timer = group.scheduleAtFixedRate(this::timerTick, 200, 200, MILLISECONDS);
            reconnectionInfo.reset();
        }
        CompletableFuture<Void> result = new CompletableFuture<>();
        result.whenComplete((value, throwable) -> {
            if (throwable != null) {
                logger.error("Unable to connect: {}", throwable.getMessage());
            }
        });
        tasks.enqueue(new StartConnection(result, discoverer));
    }

    public void disconnect() {
        disconnect("exit");
    }

    private void disconnect(String reason) {
        if (isTimerTicking()) {
            timer.cancel(true);
            timer = null;
            operationManager.cleanUp();
            subscriptionManager.cleanUp();
            closeTcpConnection(reason);
            fireEvent(Events.clientDisconnected());
            logger.info("Disconnected, reason: {}", reason);
        }
    }

    private boolean isTimerTicking() {
        return timer != null && !timer.isDone();
    }

    private void timerTick() {
        switch (connectionState()) {
            case INIT:
                if (connectingPhase == ConnectingPhase.RECONNECTING && between(reconnectionInfo.timestamp, now()).compareTo(settings.reconnectionDelay) > 0) {
                    logger.debug("Checking reconnection...");

                    reconnectionInfo.inc();

                    if (settings.maxReconnections >= 0 && reconnectionInfo.reconnectionAttempt > settings.maxReconnections) {
                        handle(new CloseConnection("Reconnection limit reached"));
                    } else {
                        fireEvent(Events.clientReconnecting());
                        discoverEndPoint(Optional.empty());
                    }
                }
                break;
            case CONNECTED:
                if (connectingPhase == ConnectingPhase.CONNECTED) {
                    checkOperationTimeout();
                }
                break;
        }
    }

    private void checkOperationTimeout() {
        if (between(lastOperationTimeoutCheck, now()).compareTo(settings.operationTimeoutCheckInterval) > 0) {
            operationManager.checkTimeoutsAndRetry(connection);
            subscriptionManager.checkTimeoutsAndRetry(connection);
            lastOperationTimeoutCheck = now();
        }
    }

    private void gotoConnectedPhase() {
        checkNotNull(connection, "connection");
        connectingPhase = ConnectingPhase.CONNECTED;
        reconnectionInfo.reset();
        fireEvent(Events.clientConnected((InetSocketAddress) connection.remoteAddress()));
        checkOperationTimeout();
    }

    private void reconnectTo(NodeEndPoints endPoints) {
        InetSocketAddress endpoint = (settings.ssl && endPoints.secureTcpEndPoint != null) ?
            endPoints.secureTcpEndPoint : endPoints.tcpEndPoint;

        if (endpoint == null) {
            handle(new CloseConnection("No endpoint is specified while trying to reconnect."));
        } else if (connectionState() == ConnectionState.CONNECTED && !connection.remoteAddress().equals(endpoint)) {
            String message = String.format("Connection '%s': going to reconnect to [%s]. Current endpoint: [%s, L%s].",
                connection.hashCode(), endpoint, connection.remoteAddress(), connection.localAddress());

            logger.trace(message);

            closeTcpConnection(message);

            connectingPhase = ConnectingPhase.ENDPOINT_DISCOVERY;
            handle(new EstablishTcpConnection(endPoints));
        }
    }

    private void discoverEndPoint(Optional<CompletableFuture<Void>> result) {
        logger.debug("Discovering endpoint...");

        if (connectionState() == ConnectionState.INIT && connectingPhase == ConnectingPhase.RECONNECTING) {
            connectingPhase = ConnectingPhase.ENDPOINT_DISCOVERY;

            discoverer.discover(connection != null ? (InetSocketAddress) connection.remoteAddress() : null)
                .whenComplete((nodeEndPoints, throwable) -> {
                    if (throwable == null) {
                        tasks.enqueue(new EstablishTcpConnection(nodeEndPoints));
                        result.ifPresent(r -> r.complete(null));
                    } else {
                        result.ifPresent(r -> r.completeExceptionally(new CannotEstablishConnectionException("Cannot resolve target end point.", throwable)));
                    }
                });
        }
    }

    private void closeTcpConnection(String reason) {
        if (connection != null) {
            logger.debug("Closing TCP connection, reason: {}", reason);
            try {
                connection.close().await(settings.tcpSettings.closeTimeout.toMillis());
            } catch (Exception e) {
                logger.warn("Unable to close connection gracefully", e);
            }
        } else {
            onTcpConnectionClosed();
        }
    }

    private void onTcpConnectionClosed() {
        if (connection != null) {
            subscriptionManager.purgeSubscribedAndDropped(ChannelId.of(connection));
            fireEvent(Events.connectionClosed());
        }

        connection = null;
        connectingPhase = ConnectingPhase.RECONNECTING;
        reconnectionInfo.touch();
    }

    private void handle(StartConnection task) {
        logger.debug("StartConnection");

        switch (connectionState()) {
            case INIT:
                connectingPhase = ConnectingPhase.RECONNECTING;
                discoverEndPoint(Optional.of(task.result));
                break;
            case CONNECTING:
            case CONNECTED:
                task.result.completeExceptionally(new InvalidOperationException(String.format("Connection %s is already active.", connection)));
                break;
            case CLOSED:
                task.result.completeExceptionally(new ConnectionClosedException("Connection is closed"));
                break;
            default:
                throw new IllegalStateException("Unknown connection state");
        }
    }

    private void handle(EstablishTcpConnection task) {
        InetSocketAddress endpoint = (settings.ssl && task.endPoints.secureTcpEndPoint != null) ?
            task.endPoints.secureTcpEndPoint : task.endPoints.tcpEndPoint;

        if (endpoint == null) {
            handle(new CloseConnection("No endpoint to node specified."));
        } else {
            logger.debug("Connecting to [{}]...", endpoint);

            if (connectionState() == ConnectionState.INIT && connectingPhase == ConnectingPhase.ENDPOINT_DISCOVERY) {
                connectingPhase = ConnectingPhase.CONNECTION_ESTABLISHING;

                bootstrap.connect(endpoint).addListener((ChannelFuture connectFuture) -> {
                    if (connectFuture.isSuccess()) {
                        logger.info("Connection to [{}, L{}] established.", connectFuture.channel().remoteAddress(), connectFuture.channel().localAddress());

                        connectingPhase = ConnectingPhase.AUTHENTICATION;

                        connection = connectFuture.channel();

                        connection.closeFuture().addListener((ChannelFuture closeFuture) -> {
                            logger.info("Connection to [{}, L{}] closed.", closeFuture.channel().remoteAddress(), closeFuture.channel().localAddress());
                            onTcpConnectionClosed();
                        });
                    } else {
                        closeTcpConnection("unable to connect");
                    }
                });
            }
        }
    }

    private void handle(CloseConnection task) {
        if (connectionState() == ConnectionState.CLOSED) {
            logger.debug("CloseConnection IGNORED because connection is CLOSED, reason: " + task.reason, task.exception);
        } else {
            logger.debug("CloseConnection, reason: " + task.reason, task.exception);

            if (task.exception != null) {
                fireEvent(Events.errorOccurred(task.exception));
            }

            disconnect(task.reason);
        }
    }

    private void handle(StartOperation task) {
        Operation operation = task.operation;

        switch (connectionState()) {
            case INIT:
                if (connectingPhase == ConnectingPhase.INVALID) {
                    operation.fail(new InvalidOperationException("No connection"));
                    break;
                }
            case CONNECTING:
                logger.debug("StartOperation enqueue {}, {}, {}, {}.", operation.getClass().getSimpleName(), operation, settings.maxOperationRetries, settings.operationTimeout);
                operationManager.enqueueOperation(new OperationItem(operation, settings.maxOperationRetries, settings.operationTimeout));
                break;
            case CONNECTED:
                logger.debug("StartOperation schedule {}, {}, {}, {}.", operation.getClass().getSimpleName(), operation, settings.maxOperationRetries, settings.operationTimeout);
                operationManager.scheduleOperation(new OperationItem(operation, settings.maxOperationRetries, settings.operationTimeout), connection);
                break;
            case CLOSED:
                operation.fail(new ConnectionClosedException("Connection is closed"));
                break;
            default:
                throw new IllegalStateException("Unknown connection state");
        }
    }

    private void handle(StartSubscription task) {
        ConnectionState state = connectionState();

        switch (state) {
            case INIT:
                task.result.completeExceptionally(new InvalidOperationException("No connection"));
                break;
            case CONNECTING:
            case CONNECTED:
                VolatileSubscriptionOperation operation = new VolatileSubscriptionOperation(
                    (CompletableFuture<VolatileSubscription>) task.result,
                    task.streamId, task.resolveLinkTos, task.userCredentials, task.listener,
                    () -> connection, executor);

                logger.debug("StartSubscription {} {}, {}, {}, {}.",
                    state == ConnectionState.CONNECTED ? "fire" : "enqueue",
                    operation.getClass().getSimpleName(), operation, task.maxRetries, task.timeout);

                SubscriptionItem item = new SubscriptionItem(operation, task.maxRetries, task.timeout);

                if (state == ConnectionState.CONNECTING) {
                    subscriptionManager.enqueueSubscription(item);
                } else {
                    subscriptionManager.startSubscription(item, connection);
                }
                break;
            case CLOSED:
                task.result.completeExceptionally(new ConnectionClosedException("Connection is closed"));
                break;
            default:
                throw new IllegalStateException("Unknown connection state");
        }
    }

    private void enqueue(Operation operation) {
        while (operationManager.totalOperationCount() >= settings.maxQueueSize) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        enqueue(new StartOperation(operation));
    }

    private void enqueue(Task task) {
        logger.trace("enqueueing task {}.", task.getClass().getSimpleName());
        tasks.enqueue(task);
    }

    private class TransactionManagerImpl implements TransactionManager {

        @Override
        public CompletableFuture<Void> write(Transaction transaction, Iterable<EventData> events, UserCredentials userCredentials) {
            checkNotNull(transaction, "transaction");
            checkNotNull(events, "events");

            CompletableFuture<Void> result = new CompletableFuture<>();
            enqueue(new TransactionalWriteOperation(result, settings.requireMaster, transaction.transactionId, events, userCredentials));
            return result;
        }

        @Override
        public CompletableFuture<WriteResult> commit(Transaction transaction, UserCredentials userCredentials) {
            checkNotNull(transaction, "transaction");

            CompletableFuture<WriteResult> result = new CompletableFuture<>();
            enqueue(new CommitTransactionOperation(result, settings.requireMaster, transaction.transactionId, userCredentials));
            return result;
        }
    }

    private static class ReconnectionInfo {
        int reconnectionAttempt;
        Instant timestamp;

        void inc() {
            reconnectionAttempt++;
            touch();
        }

        void reset() {
            reconnectionAttempt = 0;
            touch();
        }

        void touch() {
            timestamp = now();
        }
    }

}
