package lt.msemys.esjc;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.ScheduledFuture;
import lt.msemys.esjc.event.Events;
import lt.msemys.esjc.node.EndpointDiscoverer;
import lt.msemys.esjc.node.NodeEndpoints;
import lt.msemys.esjc.node.cluster.ClusterDnsEndpointDiscoverer;
import lt.msemys.esjc.node.static_.StaticEndpointDiscoverer;
import lt.msemys.esjc.operation.*;
import lt.msemys.esjc.operation.manager.OperationItem;
import lt.msemys.esjc.subscription.AllCatchUpSubscription;
import lt.msemys.esjc.subscription.PersistentSubscriptionOperation;
import lt.msemys.esjc.subscription.StreamCatchUpSubscription;
import lt.msemys.esjc.subscription.VolatileSubscriptionOperation;
import lt.msemys.esjc.subscription.manager.SubscriptionItem;
import lt.msemys.esjc.system.SystemEventTypes;
import lt.msemys.esjc.system.SystemStreams;
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
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static lt.msemys.esjc.system.SystemStreams.isMetastream;
import static lt.msemys.esjc.tcp.handler.AuthenticationHandler.AuthenticationStatus;
import static lt.msemys.esjc.util.EmptyArrays.EMPTY_BYTES;
import static lt.msemys.esjc.util.Numbers.isNegative;
import static lt.msemys.esjc.util.Numbers.isPositive;
import static lt.msemys.esjc.util.Preconditions.checkArgument;
import static lt.msemys.esjc.util.Preconditions.checkNotNull;
import static lt.msemys.esjc.util.Strings.*;
import static lt.msemys.esjc.util.Threads.sleepUninterruptibly;

public class EventStore extends AbstractEventStore {
    private static final Logger logger = LoggerFactory.getLogger(EventStore.class);

    protected static final int MAX_READ_SIZE = 4 * 1024;

    private volatile ScheduledFuture timer;
    private final TransactionManager transactionManager = new TransactionManagerImpl();
    private final TaskQueue tasks;
    private final EndpointDiscoverer discoverer;
    private final ReconnectionInfo reconnectionInfo = new ReconnectionInfo();
    private Instant lastOperationTimeoutCheck = Instant.MIN;

    public EventStore(Settings settings) {
        super(settings);

        if (settings.staticNodeSettings.isPresent()) {
            discoverer = new StaticEndpointDiscoverer(settings.staticNodeSettings.get(), settings.ssl);
        } else if (settings.clusterNodeSettings.isPresent()) {
            discoverer = new ClusterDnsEndpointDiscoverer(settings.clusterNodeSettings.get(), group);
        } else {
            throw new IllegalStateException("Node settings not found");
        }

        tasks = new TaskQueue(executor);
        tasks.register(StartConnection.class, this::handle);
        tasks.register(CloseConnection.class, this::handle);
        tasks.register(EstablishTcpConnection.class, this::handle);
        tasks.register(StartOperation.class, this::handle);
        tasks.register(StartSubscription.class, this::handle);
        tasks.register(StartPersistentSubscription.class, this::handle);
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
        checkArgument(eventNumber >= -1, "Event number out of range");

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
        checkArgument(!isNegative(start), "start should not be negative.");
        checkArgument(isPositive(count), "count should be positive.");
        checkArgument(count < MAX_READ_SIZE, "Count should be less than %d. For larger reads you should page.", MAX_READ_SIZE);

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
        checkArgument(isPositive(count), "count should be positive.");
        checkArgument(count < MAX_READ_SIZE, "Count should be less than %d. For larger reads you should page.", MAX_READ_SIZE);

        CompletableFuture<StreamEventsSlice> result = new CompletableFuture<>();
        enqueue(new ReadStreamEventsBackwardOperation(result, stream, start, count, resolveLinkTos, settings.requireMaster, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<AllEventsSlice> readAllEventsForward(Position position,
                                                                  int maxCount,
                                                                  boolean resolveLinkTos,
                                                                  UserCredentials userCredentials) {
        checkArgument(isPositive(maxCount), "count should be positive.");
        checkArgument(maxCount < MAX_READ_SIZE, "Count should be less than %d. For larger reads you should page.", MAX_READ_SIZE);

        CompletableFuture<AllEventsSlice> result = new CompletableFuture<>();
        enqueue(new ReadAllEventsForwardOperation(result, position, maxCount, resolveLinkTos, settings.requireMaster, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<AllEventsSlice> readAllEventsBackward(Position position,
                                                                   int maxCount,
                                                                   boolean resolveLinkTos,
                                                                   UserCredentials userCredentials) {
        checkArgument(isPositive(maxCount), "count should be positive.");
        checkArgument(maxCount < MAX_READ_SIZE, "Count should be less than %d. For larger reads you should page.", MAX_READ_SIZE);

        CompletableFuture<AllEventsSlice> result = new CompletableFuture<>();
        enqueue(new ReadAllEventsBackwardOperation(result, position, maxCount, resolveLinkTos, settings.requireMaster, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<Subscription> subscribeToStream(String stream,
                                                             boolean resolveLinkTos,
                                                             SubscriptionListener listener,
                                                             UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkNotNull(listener, "listener");

        CompletableFuture<Subscription> result = new CompletableFuture<>();
        enqueue(new StartSubscription(result, stream, resolveLinkTos, userCredentials, listener, settings.maxOperationRetries, settings.operationTimeout));
        return result;
    }

    @Override
    public CompletableFuture<Subscription> subscribeToAll(boolean resolveLinkTos,
                                                          SubscriptionListener listener,
                                                          UserCredentials userCredentials) {
        checkNotNull(listener, "listener");

        CompletableFuture<Subscription> result = new CompletableFuture<>();
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
    public PersistentSubscription subscribeToPersistent(String stream,
                                                        String groupName,
                                                        SubscriptionListener listener,
                                                        UserCredentials userCredentials,
                                                        int bufferSize,
                                                        boolean autoAck) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkArgument(!isNullOrEmpty(groupName), "groupName");
        checkNotNull(listener, "listener");
        checkArgument(isPositive(bufferSize), "bufferSize should be positive");

        PersistentSubscription subscription = new PersistentSubscription(groupName, stream, listener, userCredentials, bufferSize, autoAck, executor) {
            @Override
            protected CompletableFuture<Subscription> startSubscription(String subscriptionId,
                                                                        String streamId,
                                                                        int bufferSize,
                                                                        SubscriptionListener listener,
                                                                        UserCredentials userCredentials) {
                CompletableFuture<Subscription> result = new CompletableFuture<>();
                enqueue(new StartPersistentSubscription(result, subscriptionId, streamId, bufferSize,
                    userCredentials, listener, settings.maxOperationRetries, settings.operationTimeout));
                return result;
            }
        };

        subscription.start();

        return subscription;
    }

    @Override
    public CompletableFuture<PersistentSubscriptionCreateResult> createPersistentSubscription(String stream,
                                                                                              String groupName,
                                                                                              PersistentSubscriptionSettings settings,
                                                                                              UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkArgument(!isNullOrEmpty(groupName), "groupName");
        checkNotNull(settings, "settings");

        CompletableFuture<PersistentSubscriptionCreateResult> result = new CompletableFuture<>();
        enqueue(new CreatePersistentSubscriptionOperation(result, stream, groupName, settings, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<PersistentSubscriptionUpdateResult> updatePersistentSubscription(String stream,
                                                                                              String groupName,
                                                                                              PersistentSubscriptionSettings settings,
                                                                                              UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkArgument(!isNullOrEmpty(groupName), "groupName");
        checkNotNull(settings, "settings");

        CompletableFuture<PersistentSubscriptionUpdateResult> result = new CompletableFuture<>();
        enqueue(new UpdatePersistentSubscriptionOperation(result, stream, groupName, settings, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<PersistentSubscriptionDeleteResult> deletePersistentSubscription(String stream,
                                                                                              String groupName,
                                                                                              UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkArgument(!isNullOrEmpty(groupName), "groupName");

        CompletableFuture<PersistentSubscriptionDeleteResult> result = new CompletableFuture<>();
        enqueue(new DeletePersistentSubscriptionOperation(result, stream, groupName, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<WriteResult> setStreamMetadata(String stream,
                                                            ExpectedVersion expectedMetastreamVersion,
                                                            byte[] metadata,
                                                            UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        checkArgument(!isMetastream(stream), "Setting metadata for metastream '%s' is not supported.", stream);
        checkNotNull(expectedMetastreamVersion, "expectedMetastreamVersion");
        checkNotNull(metadata, "metadata");

        CompletableFuture<WriteResult> result = new CompletableFuture<>();

        EventData metaevent = EventData.newBuilder()
            .type(SystemEventTypes.STREAM_METADATA)
            .jsonData(metadata)
            .build();

        enqueue(new AppendToStreamOperation(result, settings.requireMaster, SystemStreams.metastreamOf(stream),
            expectedMetastreamVersion.value, asList(metaevent), userCredentials));

        return result;
    }

    @Override
    public CompletableFuture<StreamMetadataResult> getStreamMetadata(String stream, UserCredentials userCredentials) {
        CompletableFuture<StreamMetadataResult> result = new CompletableFuture<>();

        getStreamMetadataAsRawBytes(stream, userCredentials).whenComplete((r, t) -> {
            if (t != null) {
                result.completeExceptionally(t);
            } else if (r.streamMetadata == null || r.streamMetadata.length == 0) {
                result.complete(new StreamMetadataResult(r.stream, r.isStreamDeleted, r.metastreamVersion, StreamMetadata.empty()));
            } else {
                result.complete(new StreamMetadataResult(r.stream, r.isStreamDeleted, r.metastreamVersion, StreamMetadata.fromJson(r.streamMetadata)));
            }
        });

        return result;
    }

    @Override
    public CompletableFuture<RawStreamMetadataResult> getStreamMetadataAsRawBytes(String stream, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream");

        CompletableFuture<RawStreamMetadataResult> result = new CompletableFuture<>();

        readEvent(SystemStreams.metastreamOf(stream), StreamPosition.END, false, userCredentials).whenComplete((r, t) -> {
            if (t != null) {
                result.completeExceptionally(t);
            } else {
                switch (r.status) {
                    case Success:
                        if (r.event == null) {
                            result.completeExceptionally(new Exception("Event is null while operation result is Success."));
                        } else {
                            RecordedEvent event = r.event.originalEvent();
                            result.complete((event == null) ?
                                new RawStreamMetadataResult(stream, false, -1, EMPTY_BYTES) :
                                new RawStreamMetadataResult(stream, false, event.eventNumber, event.data));
                        }
                        break;
                    case NotFound:
                    case NoStream:
                        result.complete(new RawStreamMetadataResult(stream, false, -1, EMPTY_BYTES));
                        break;
                    case StreamDeleted:
                        result.complete(new RawStreamMetadataResult(stream, true, Integer.MAX_VALUE, EMPTY_BYTES));
                        break;
                    default:
                        result.completeExceptionally(new IllegalStateException("Unexpected ReadEventResult: " + r.status));
                }
            }
        });

        return result;
    }

    @Override
    public CompletableFuture<WriteResult> setSystemSettings(SystemSettings settings, UserCredentials userCredentials) {
        checkNotNull(settings, "settings");
        return appendToStream(SystemStreams.SETTINGS_STREAM,
            ExpectedVersion.any(),
            asList(EventData.newBuilder()
                .type(SystemEventTypes.SETTINGS)
                .jsonData(settings.toJson())
                .build()),
            userCredentials);
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
    protected void onReconnect(NodeEndpoints nodeEndpoints) {
        reconnectTo(nodeEndpoints);
    }

    public void connect() {
        if (!isRunning()) {
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
        if (isRunning()) {
            timer.cancel(true);
            timer = null;
            operationManager.cleanUp();
            subscriptionManager.cleanUp();
            closeTcpConnection(reason);
            connectingPhase = ConnectingPhase.INVALID;
            fireEvent(Events.clientDisconnected());
            logger.info("Disconnected, reason: {}", reason);
        }
    }

    public boolean isRunning() {
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
                checkOperationTimeout();
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

    private void reconnectTo(NodeEndpoints endpoints) {
        InetSocketAddress endpoint = (settings.ssl && endpoints.secureTcpEndpoint != null) ?
            endpoints.secureTcpEndpoint : endpoints.tcpEndpoint;

        if (endpoint == null) {
            handle(new CloseConnection("No endpoint is specified while trying to reconnect."));
        } else if (connectionState() == ConnectionState.CONNECTED && !connection.remoteAddress().equals(endpoint)) {
            String message = String.format("Connection '%s': going to reconnect to [%s]. Current endpoint: [%s, L%s].",
                ChannelId.of(connection), endpoint, connection.remoteAddress(), connection.localAddress());

            logger.trace(message);

            closeTcpConnection(message);

            connectingPhase = ConnectingPhase.ENDPOINT_DISCOVERY;
            handle(new EstablishTcpConnection(endpoints));
        }
    }

    private void discoverEndPoint(Optional<CompletableFuture<Void>> result) {
        logger.debug("Discovering endpoint...");

        if (connectionState() == ConnectionState.INIT && connectingPhase == ConnectingPhase.RECONNECTING) {
            connectingPhase = ConnectingPhase.ENDPOINT_DISCOVERY;

            discoverer.discover(connection != null ? (InetSocketAddress) connection.remoteAddress() : null)
                .whenComplete((nodeEndpoints, throwable) -> {
                    if (throwable == null) {
                        tasks.enqueue(new EstablishTcpConnection(nodeEndpoints));
                        result.ifPresent(r -> r.complete(null));
                    } else {
                        tasks.enqueue(new CloseConnection("Failed to resolve TCP endpoint to which to connect.", throwable));
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
        InetSocketAddress endpoint = (settings.ssl && task.endpoints.secureTcpEndpoint != null) ?
            task.endpoints.secureTcpEndpoint : task.endpoints.tcpEndpoint;

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
            logger.debug("CloseConnection IGNORED because connection is CLOSED, reason: " + task.reason, task.throwable);
        } else {
            logger.debug("CloseConnection, reason: " + task.reason, task.throwable);

            if (task.throwable != null) {
                fireEvent(Events.errorOccurred(task.throwable));
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
                if (connectingPhase == ConnectingPhase.INVALID) {
                    task.result.completeExceptionally(new InvalidOperationException("No connection"));
                    break;
                }
            case CONNECTING:
            case CONNECTED:
                VolatileSubscriptionOperation operation = new VolatileSubscriptionOperation(
                    task.result,
                    task.streamId, task.resolveLinkTos, task.userCredentials, task.listener,
                    () -> connection, executor);

                logger.debug("StartSubscription {} {}, {}, {}, {}.",
                    state == ConnectionState.CONNECTED ? "fire" : "enqueue",
                    operation.getClass().getSimpleName(), operation, task.maxRetries, task.timeout);

                SubscriptionItem item = new SubscriptionItem(operation, task.maxRetries, task.timeout);

                if (state == ConnectionState.CONNECTED) {
                    subscriptionManager.startSubscription(item, connection);
                } else {
                    subscriptionManager.enqueueSubscription(item);
                }
                break;
            case CLOSED:
                task.result.completeExceptionally(new ConnectionClosedException("Connection is closed"));
                break;
            default:
                throw new IllegalStateException("Unknown connection state");
        }
    }

    private void handle(StartPersistentSubscription task) {
        ConnectionState state = connectionState();

        switch (state) {
            case INIT:
                if (connectingPhase == ConnectingPhase.INVALID) {
                    task.result.completeExceptionally(new InvalidOperationException("No connection"));
                    break;
                }
            case CONNECTING:
            case CONNECTED:
                PersistentSubscriptionOperation operation = new PersistentSubscriptionOperation(
                    task.result,
                    task.subscriptionId, task.streamId, task.bufferSize, task.userCredentials, task.listener,
                    () -> connection, executor);

                logger.debug("StartSubscription {} {}, {}, {}, {}.",
                    state == ConnectionState.CONNECTED ? "fire" : "enqueue",
                    operation.getClass().getSimpleName(), operation, task.maxRetries, task.timeout);

                SubscriptionItem item = new SubscriptionItem(operation, task.maxRetries, task.timeout);

                if (state == ConnectionState.CONNECTED) {
                    subscriptionManager.startSubscription(item, connection);
                } else {
                    subscriptionManager.enqueueSubscription(item);
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
        while (operationManager.totalOperationCount() >= settings.maxOperationQueueSize) {
            sleepUninterruptibly(1);
        }
        enqueue(new StartOperation(operation));
    }

    private void enqueue(Task task) {
        if (!isRunning()) {
            connect();
        }
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
