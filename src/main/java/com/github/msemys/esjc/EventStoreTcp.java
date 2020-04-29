package com.github.msemys.esjc;

import com.github.msemys.esjc.event.Event;
import com.github.msemys.esjc.event.EventQueue;
import com.github.msemys.esjc.event.Events;
import com.github.msemys.esjc.node.EndpointDiscoverer;
import com.github.msemys.esjc.node.NodeEndpoints;
import com.github.msemys.esjc.operation.*;
import com.github.msemys.esjc.operation.manager.OperationItem;
import com.github.msemys.esjc.operation.manager.OperationManager;
import com.github.msemys.esjc.ssl.CommonNameTrustManagerFactory;
import com.github.msemys.esjc.subscription.*;
import com.github.msemys.esjc.subscription.manager.SubscriptionItem;
import com.github.msemys.esjc.subscription.manager.SubscriptionManager;
import com.github.msemys.esjc.system.SystemEventTypes;
import com.github.msemys.esjc.system.SystemStreams;
import com.github.msemys.esjc.task.*;
import com.github.msemys.esjc.tcp.TcpPackage;
import com.github.msemys.esjc.tcp.TcpPackageDecoder;
import com.github.msemys.esjc.tcp.TcpPackageEncoder;
import com.github.msemys.esjc.tcp.handler.AuthenticationHandler;
import com.github.msemys.esjc.tcp.handler.AuthenticationHandler.AuthenticationStatus;
import com.github.msemys.esjc.tcp.handler.HeartbeatHandler;
import com.github.msemys.esjc.tcp.handler.IdentificationHandler;
import com.github.msemys.esjc.tcp.handler.IdentificationHandler.IdentificationStatus;
import com.github.msemys.esjc.tcp.handler.OperationHandler;
import com.github.msemys.esjc.transaction.TransactionManager;
import com.github.msemys.esjc.util.Strings;
import com.github.msemys.esjc.util.SystemTime;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static com.github.msemys.esjc.system.SystemStreams.isMetastream;
import static com.github.msemys.esjc.util.EmptyArrays.EMPTY_BYTES;
import static com.github.msemys.esjc.util.Numbers.isNegative;
import static com.github.msemys.esjc.util.Numbers.isPositive;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static com.github.msemys.esjc.util.Ranges.BATCH_SIZE_RANGE;
import static com.github.msemys.esjc.util.Strings.*;
import static com.github.msemys.esjc.util.Threads.sleepUninterruptibly;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.StreamSupport.stream;

public class EventStoreTcp implements EventStore {
    private static final Logger logger = LoggerFactory.getLogger(EventStore.class);

    private static final int MAX_FRAME_LENGTH = 64 * 1024 * 1024;

    private enum ConnectionState {INIT, CONNECTING, CONNECTED, CLOSED}

    private enum ConnectingPhase {INVALID, RECONNECTING, ENDPOINT_DISCOVERY, CONNECTION_ESTABLISHING, AUTHENTICATION, IDENTIFICATION, CONNECTED}

    private final EventLoopGroup group = new NioEventLoopGroup(0, new DefaultThreadFactory("esio"));
    private final Bootstrap bootstrap;
    private final OperationManager operationManager;
    private final SubscriptionManager subscriptionManager;
    private final Settings settings;

    private volatile Channel connection;
    private volatile ConnectingPhase connectingPhase = ConnectingPhase.INVALID;

    private volatile ScheduledFuture timer;
    private final TransactionManager transactionManager = new TransactionManagerImpl();
    private final TaskQueue tasks;
    private final EndpointDiscoverer discoverer;
    private final ReconnectionInfo reconnectionInfo = new ReconnectionInfo();
    private final SystemTime lastOperationTimeoutCheck = SystemTime.zero();

    private final EventQueue events;

    private final Object mutex = new Object();

    protected EventStoreTcp(Settings settings) {
        checkNotNull(settings, "settings is null");

        bootstrap = new Bootstrap()
            .option(ChannelOption.SO_KEEPALIVE, settings.tcpSettings.keepAlive)
            .option(ChannelOption.TCP_NODELAY, settings.tcpSettings.noDelay)
            .option(ChannelOption.SO_SNDBUF, settings.tcpSettings.sendBufferSize)
            .option(ChannelOption.SO_RCVBUF, settings.tcpSettings.receiveBufferSize)
            .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(
                settings.tcpSettings.writeBufferLowWaterMark,
                settings.tcpSettings.writeBufferHighWaterMark))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) settings.tcpSettings.connectTimeout.toMillis())
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();

                    if (settings.sslSettings.useSslConnection) {
                        SslContextBuilder builder = SslContextBuilder.forClient();
                        switch (settings.sslSettings.validationMode) {
                            case NONE:
                                builder = builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
                                break;
                            case COMMON_NAME:
                                builder = builder.trustManager(new CommonNameTrustManagerFactory(settings.sslSettings.certificateCommonName));
                                break;
                            case CERTIFICATE:
                                builder = builder.trustManager(settings.sslSettings.certificateFile);
                                break;
                        }
                        SslContext sslContext = builder.build();
                        pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));
                    }

                    // decoder
                    pipeline.addLast("frame-decoder", new LengthFieldBasedFrameDecoder(LITTLE_ENDIAN, MAX_FRAME_LENGTH, 0, 4, 0, 4, true));
                    pipeline.addLast("package-decoder", new TcpPackageDecoder());

                    // encoder
                    pipeline.addLast("frame-encoder", new LengthFieldPrepender(LITTLE_ENDIAN, 4, 0, false));
                    pipeline.addLast("package-encoder", new TcpPackageEncoder());

                    // logic
                    pipeline.addLast("idle-state-handler", new IdleStateHandler(0, settings.heartbeatInterval.toMillis(), 0, MILLISECONDS));
                    pipeline.addLast("heartbeat-handler", new HeartbeatHandler(settings.heartbeatTimeout));
                    pipeline.addLast("authentication-handler", new AuthenticationHandler(settings.userCredentials, settings.operationTimeout)
                        .whenComplete(EventStoreTcp.this::onAuthenticationCompleted));
                    pipeline.addLast("identification-handler", new IdentificationHandler(settings.connectionName, settings.operationTimeout)
                        .whenComplete(EventStoreTcp.this::onIdentificationCompleted));
                    pipeline.addLast("operation-handler", new OperationHandler(operationManager, subscriptionManager)
                        .whenBadRequest(EventStoreTcp.this::onBadRequest)
                        .whenChannelError(EventStoreTcp.this::onChannelError)
                        .whenReconnect(EventStoreTcp.this::onReconnect));
                }
            });

        operationManager = new OperationManager(settings);
        subscriptionManager = new SubscriptionManager(settings);

        this.settings = settings;

        discoverer = settings.endpointDiscovererFactory.create(settings, group);
        checkNotNull(discoverer, "endpoint discoverer cannot be null");

        tasks = new TaskQueue(executor());
        tasks.register(StartConnection.class, this::handle);
        tasks.register(CloseConnection.class, this::handle);
        tasks.register(EstablishTcpConnection.class, this::handle);
        tasks.register(StartOperation.class, this::handle);
        tasks.register(StartSubscription.class, this::handle);
        tasks.register(StartPersistentSubscription.class, this::handle);

        events = new EventQueue(executor());
    }

    @Override
    public CompletableFuture<DeleteResult> deleteStream(String stream,
                                                        long expectedVersion,
                                                        boolean hardDelete,
                                                        UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");

        CompletableFuture<DeleteResult> result = new CompletableFuture<>();
        enqueue(new DeleteStreamOperation(result, settings.requireMaster, stream, expectedVersion, hardDelete, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<WriteResult> appendToStream(String stream,
                                                         long expectedVersion,
                                                         Iterable<EventData> events,
                                                         UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkNotNull(events, "events is null");

        CompletableFuture<WriteResult> result = new CompletableFuture<>();
        enqueue(new AppendToStreamOperation(result, settings.requireMaster, stream, expectedVersion, events, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<WriteAttemptResult> tryAppendToStream(String stream,
                                                                   long expectedVersion,
                                                                   Iterable<EventData> events,
                                                                   UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkNotNull(events, "events is null");

        CompletableFuture<WriteAttemptResult> result = new CompletableFuture<>();
        enqueue(new TryAppendToStreamOperation(result, settings.requireMaster, stream, expectedVersion, events, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<Transaction> startTransaction(String stream,
                                                           long expectedVersion,
                                                           UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");

        CompletableFuture<Transaction> result = new CompletableFuture<>();
        enqueue(new StartTransactionOperation(result, settings.requireMaster, stream, expectedVersion, transactionManager, userCredentials));
        return result;
    }

    @Override
    public Transaction continueTransaction(long transactionId, UserCredentials userCredentials) {
        return new Transaction(transactionId, userCredentials, transactionManager);
    }

    @Override
    public CompletableFuture<EventReadResult> readEvent(String stream,
                                                        long eventNumber,
                                                        boolean resolveLinkTos,
                                                        UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkArgument(eventNumber >= StreamPosition.END, "eventNumber out of range");

        CompletableFuture<EventReadResult> result = new CompletableFuture<>();
        enqueue(new ReadEventOperation(result, stream, eventNumber, resolveLinkTos, settings.requireMaster, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<StreamEventsSlice> readStreamEventsForward(String stream,
                                                                        long eventNumber,
                                                                        int maxCount,
                                                                        boolean resolveLinkTos,
                                                                        UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkArgument(!isNegative(eventNumber), "eventNumber should not be negative");
        checkArgument(BATCH_SIZE_RANGE.contains(maxCount), "maxCount is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());

        CompletableFuture<StreamEventsSlice> result = new CompletableFuture<>();
        enqueue(new ReadStreamEventsForwardOperation(result, stream, eventNumber, maxCount, resolveLinkTos, settings.requireMaster, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<StreamEventsSlice> readStreamEventsBackward(String stream,
                                                                         long eventNumber,
                                                                         int maxCount,
                                                                         boolean resolveLinkTos,
                                                                         UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkArgument(eventNumber >= StreamPosition.END, "eventNumber out of range");
        checkArgument(BATCH_SIZE_RANGE.contains(maxCount), "maxCount is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());

        CompletableFuture<StreamEventsSlice> result = new CompletableFuture<>();
        enqueue(new ReadStreamEventsBackwardOperation(result, stream, eventNumber, maxCount, resolveLinkTos, settings.requireMaster, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<AllEventsSlice> readAllEventsForward(Position position,
                                                                  int maxCount,
                                                                  boolean resolveLinkTos,
                                                                  UserCredentials userCredentials) {
        checkArgument(BATCH_SIZE_RANGE.contains(maxCount), "maxCount is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());

        CompletableFuture<AllEventsSlice> result = new CompletableFuture<>();
        enqueue(new ReadAllEventsForwardOperation(result, position, maxCount, resolveLinkTos, settings.requireMaster, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<AllEventsSlice> readAllEventsBackward(Position position,
                                                                   int maxCount,
                                                                   boolean resolveLinkTos,
                                                                   UserCredentials userCredentials) {
        checkArgument(BATCH_SIZE_RANGE.contains(maxCount), "maxCount is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());

        CompletableFuture<AllEventsSlice> result = new CompletableFuture<>();
        enqueue(new ReadAllEventsBackwardOperation(result, position, maxCount, resolveLinkTos, settings.requireMaster, userCredentials));
        return result;
    }

    @Override
    public Iterator<ResolvedEvent> iterateStreamEventsForward(String stream,
                                                              long eventNumber,
                                                              int batchSize,
                                                              boolean resolveLinkTos,
                                                              UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkArgument(!isNegative(eventNumber), "eventNumber should not be negative");
        checkArgument(BATCH_SIZE_RANGE.contains(batchSize), "batchSize is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());
        return new StreamEventsIterator(eventNumber, i -> readStreamEventsForward(stream, i, batchSize, resolveLinkTos, userCredentials));
    }

    @Override
    public Iterator<ResolvedEvent> iterateStreamEventsBackward(String stream,
                                                               long eventNumber,
                                                               int batchSize,
                                                               boolean resolveLinkTos,
                                                               UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkArgument(BATCH_SIZE_RANGE.contains(batchSize), "batchSize is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());
        return new StreamEventsIterator(eventNumber, i -> readStreamEventsBackward(stream, i, batchSize, resolveLinkTos, userCredentials));
    }

    @Override
    public Iterator<ResolvedEvent> iterateAllEventsForward(Position position,
                                                           int batchSize,
                                                           boolean resolveLinkTos,
                                                           UserCredentials userCredentials) {
        checkArgument(BATCH_SIZE_RANGE.contains(batchSize), "batchSize is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());
        return new AllEventsIterator(position, p -> readAllEventsForward(p, batchSize, resolveLinkTos, userCredentials));
    }

    @Override
    public Iterator<ResolvedEvent> iterateAllEventsBackward(Position position,
                                                            int batchSize,
                                                            boolean resolveLinkTos,
                                                            UserCredentials userCredentials) {
        checkArgument(BATCH_SIZE_RANGE.contains(batchSize), "batchSize is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());
        return new AllEventsIterator(position, p -> readAllEventsBackward(p, batchSize, resolveLinkTos, userCredentials));
    }

    @Override
    public Stream<ResolvedEvent> streamEventsForward(String stream,
                                                     long eventNumber,
                                                     int batchSize,
                                                     boolean resolveLinkTos,
                                                     UserCredentials userCredentials) {
        checkArgument(BATCH_SIZE_RANGE.contains(batchSize), "batchSize is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());
        return stream(new StreamEventsSpliterator(eventNumber, i -> readStreamEventsForward(stream, i, batchSize, resolveLinkTos, userCredentials)), false);
    }

    @Override
    public Stream<ResolvedEvent> streamEventsBackward(String stream,
                                                      long eventNumber,
                                                      int batchSize,
                                                      boolean resolveLinkTos,
                                                      UserCredentials userCredentials) {
        checkArgument(BATCH_SIZE_RANGE.contains(batchSize), "batchSize is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());
        return stream(new StreamEventsSpliterator(eventNumber, i -> readStreamEventsBackward(stream, i, batchSize, resolveLinkTos, userCredentials)), false);
    }

    @Override
    public Stream<ResolvedEvent> streamAllEventsForward(Position position,
                                                        int batchSize,
                                                        boolean resolveLinkTos,
                                                        UserCredentials userCredentials) {
        checkArgument(BATCH_SIZE_RANGE.contains(batchSize), "batchSize is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());
        return stream(new AllEventsSpliterator(position, p -> readAllEventsForward(p, batchSize, resolveLinkTos, userCredentials)), false);
    }

    @Override
    public Stream<ResolvedEvent> streamAllEventsBackward(Position position,
                                                         int batchSize,
                                                         boolean resolveLinkTos,
                                                         UserCredentials userCredentials) {
        checkArgument(BATCH_SIZE_RANGE.contains(batchSize), "batchSize is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());
        return stream(new AllEventsSpliterator(position, p -> readAllEventsBackward(p, batchSize, resolveLinkTos, userCredentials)), false);
    }

    @Override
    public CompletableFuture<Subscription> subscribeToStream(String stream,
                                                             boolean resolveLinkTos,
                                                             VolatileSubscriptionListener listener,
                                                             UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkNotNull(listener, "listener is null");

        CompletableFuture<Subscription> result = new CompletableFuture<>();
        enqueue(new StartSubscription(result, stream, resolveLinkTos, userCredentials, listener, settings.maxOperationRetries, settings.operationTimeout));
        return result;
    }

    @Override
    public CompletableFuture<Subscription> subscribeToAll(boolean resolveLinkTos,
                                                          VolatileSubscriptionListener listener,
                                                          UserCredentials userCredentials) {
        checkNotNull(listener, "listener is null");

        CompletableFuture<Subscription> result = new CompletableFuture<>();
        enqueue(new StartSubscription(result, Strings.EMPTY, resolveLinkTos, userCredentials, listener, settings.maxOperationRetries, settings.operationTimeout));
        return result;
    }

    @Override
    public CatchUpSubscription subscribeToStreamFrom(String stream,
                                                     Long eventNumber,
                                                     CatchUpSubscriptionSettings settings,
                                                     CatchUpSubscriptionListener listener,
                                                     UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkNotNull(listener, "listener is null");
        checkNotNull(settings, "settings is null");

        CatchUpSubscription subscription = new StreamCatchUpSubscription(this,
            stream, eventNumber, settings.resolveLinkTos, listener, userCredentials, settings.readBatchSize, settings.maxLiveQueueSize, executor());

        subscription.start();

        return subscription;
    }

    @Override
    public CatchUpSubscription subscribeToAllFrom(Position position,
                                                  CatchUpSubscriptionSettings settings,
                                                  CatchUpSubscriptionListener listener,
                                                  UserCredentials userCredentials) {
        checkNotNull(listener, "listener is null");
        checkNotNull(settings, "settings is null");

        CatchUpSubscription subscription = new AllCatchUpSubscription(this,
            position, settings.resolveLinkTos, listener, userCredentials, settings.readBatchSize, settings.maxLiveQueueSize, executor());

        subscription.start();

        return subscription;
    }

    @Override
    public CompletableFuture<PersistentSubscription> subscribeToPersistent(String stream,
                                                                           String groupName,
                                                                           PersistentSubscriptionListener listener,
                                                                           UserCredentials userCredentials,
                                                                           int bufferSize,
                                                                           boolean autoAck) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkArgument(!isNullOrEmpty(groupName), "groupName is null or empty");
        checkNotNull(listener, "listener is null");
        checkArgument(isPositive(bufferSize), "bufferSize should be positive");

        PersistentSubscription subscription = new PersistentSubscription(groupName, stream, listener, userCredentials, bufferSize, autoAck, executor()) {
            @Override
            protected CompletableFuture<Subscription> startSubscription(String subscriptionId,
                                                                        String streamId,
                                                                        int bufferSize,
                                                                        SubscriptionListener<PersistentSubscriptionChannel, RetryableResolvedEvent> listener,
                                                                        UserCredentials userCredentials) {
                CompletableFuture<Subscription> result = new CompletableFuture<>();
                enqueue(new StartPersistentSubscription(result, subscriptionId, streamId, bufferSize,
                    userCredentials, listener, settings.maxOperationRetries, settings.operationTimeout));
                return result;
            }
        };

        return subscription.start();
    }

    @Override
    public CompletableFuture<PersistentSubscriptionCreateResult> createPersistentSubscription(String stream,
                                                                                              String groupName,
                                                                                              PersistentSubscriptionSettings settings,
                                                                                              UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkArgument(!isNullOrEmpty(groupName), "groupName is null or empty");
        checkNotNull(settings, "settings is null");

        CompletableFuture<PersistentSubscriptionCreateResult> result = new CompletableFuture<>();
        enqueue(new CreatePersistentSubscriptionOperation(result, stream, groupName, settings, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<PersistentSubscriptionUpdateResult> updatePersistentSubscription(String stream,
                                                                                              String groupName,
                                                                                              PersistentSubscriptionSettings settings,
                                                                                              UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkArgument(!isNullOrEmpty(groupName), "groupName is null or empty");
        checkNotNull(settings, "settings is null");

        CompletableFuture<PersistentSubscriptionUpdateResult> result = new CompletableFuture<>();
        enqueue(new UpdatePersistentSubscriptionOperation(result, stream, groupName, settings, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<PersistentSubscriptionDeleteResult> deletePersistentSubscription(String stream,
                                                                                              String groupName,
                                                                                              UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkArgument(!isNullOrEmpty(groupName), "groupName is null or empty");

        CompletableFuture<PersistentSubscriptionDeleteResult> result = new CompletableFuture<>();
        enqueue(new DeletePersistentSubscriptionOperation(result, stream, groupName, userCredentials));
        return result;
    }

    @Override
    public CompletableFuture<WriteResult> setStreamMetadata(String stream,
                                                            long expectedMetastreamVersion,
                                                            byte[] metadata,
                                                            UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        checkArgument(!isMetastream(stream), "Setting metadata for metastream '%s' is not supported", stream);

        CompletableFuture<WriteResult> result = new CompletableFuture<>();

        EventData metaevent = EventData.newBuilder()
            .type(SystemEventTypes.STREAM_METADATA)
            .jsonData(metadata)
            .build();

        enqueue(new AppendToStreamOperation(result, settings.requireMaster, SystemStreams.metastreamOf(stream),
            expectedMetastreamVersion, singletonList(metaevent), userCredentials));

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
                try {
                    result.complete(new StreamMetadataResult(r.stream, r.isStreamDeleted, r.metastreamVersion, StreamMetadata.fromJson(r.streamMetadata)));
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            }
        });

        return result;
    }

    @Override
    public CompletableFuture<RawStreamMetadataResult> getStreamMetadataAsRawBytes(String stream, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");

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
                        result.complete(new RawStreamMetadataResult(stream, true, Long.MAX_VALUE, EMPTY_BYTES));
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
        checkNotNull(settings, "settings is null");
        return appendToStream(SystemStreams.SETTINGS_STREAM,
            ExpectedVersion.ANY,
            singletonList(EventData.newBuilder()
                .type(SystemEventTypes.SETTINGS)
                .jsonData(settings.toJson())
                .build()),
            userCredentials);
    }

    @Override
    public void connect() {
        synchronized (mutex) {
            if (!isRunning()) {
                reconnectionInfo.reset();
                timer = group.scheduleAtFixedRate(this::timerTick, 200, 200, MILLISECONDS);
            }
        }
        CompletableFuture<Void> result = new CompletableFuture<>();
        result.whenComplete((value, throwable) -> {
            if (throwable != null) {
                logger.error("Unable to connect: {}", throwable.getMessage());
            }
        });
        tasks.enqueue(new StartConnection(result, discoverer));
    }

    @Override
    public void disconnect() {
        disconnect("user initiated", null);
    }

    @Override
    public void shutdown() {
        disconnect("shutdown", null);

        if (executor() instanceof ExecutorService) {
            ((ExecutorService) executor()).shutdown();
        }

        group.shutdownGracefully();
    }

    private void disconnect(String reason, Throwable cause) {
        synchronized (mutex) {
            if (isRunning()) {
                timer.cancel(true);
                timer = null;
                operationManager.cleanUp(cause);
                subscriptionManager.cleanUp(cause);
                closeTcpConnection(reason);
                connectingPhase = ConnectingPhase.INVALID;
                fireEvent(Events.clientDisconnected());
                logger.info("Disconnected, reason: {}", reason);
            }
        }
    }

    private boolean isRunning() {
        return timer != null && !timer.isDone();
    }

    @Override
    public Settings settings() {
        return settings;
    }

    @Override
    public void addListener(EventStoreListener listener) {
        checkNotNull(listener, "listener is null");
        events.register(listener);
    }

    @Override
    public void removeListener(EventStoreListener listener) {
        checkNotNull(listener, "listener is null");
        events.unregister(listener);
    }

    private Executor executor() {
        return settings.executor;
    }

    private void fireEvent(Event event) {
        events.enqueue(event);
    }

    private void onAuthenticationCompleted(AuthenticationStatus status) {
        if (status == AuthenticationStatus.SUCCESS || status == AuthenticationStatus.IGNORED) {
            gotoIdentificationPhase();
        } else {
            fireEvent(Events.authenticationFailed());
        }
    }

    private void onIdentificationCompleted(IdentificationStatus status) {
        if (status == IdentificationStatus.SUCCESS) {
            gotoConnectedPhase();
        }
    }

    private void onBadRequest(TcpPackage tcpPackage) {
        handle(new CloseConnection("Connection-wide BadRequest received. Too dangerous to continue.",
            new EventStoreException("Bad request received from server. Error: " + defaultIfEmpty(newString(tcpPackage.data), "<no message>"))));
    }

    private void onChannelError(Throwable throwable) {
        if (settings().disconnectOnTcpChannelError) {
            handle(new CloseConnection("Error when processing TCP package", throwable));
        } else {
            logger.error("Failed processing TCP package", throwable);
            fireEvent(Events.errorOccurred(throwable));
        }
    }

    private void onReconnect(NodeEndpoints nodeEndpoints) {
        reconnectTo(nodeEndpoints);
    }

    private void timerTick() {
        try {
            switch (connectionState()) {
                case INIT:
                    if (connectingPhase == ConnectingPhase.RECONNECTING && reconnectionInfo.timestamp.isElapsed(settings.reconnectionDelay)) {
                        logger.debug("Checking reconnection...");

                        reconnectionInfo.inc();

                        if (settings.maxReconnections >= 0 && reconnectionInfo.reconnectionAttempt > settings.maxReconnections) {
                            handle(new CloseConnection("Reconnection limit reached"));
                        } else {
                            fireEvent(Events.clientReconnecting());
                            operationManager.checkTimeoutsAndRetry(connection);
                            discoverEndpoint(null);
                        }
                    }
                    break;
                case CONNECTED:
                    checkOperationTimeout();
                    break;
            }
        } catch (Exception e) {
            logger.error("Error occurred in timer thread", e);
        }
    }

    private void checkOperationTimeout() {
        if (lastOperationTimeoutCheck.isElapsed(settings.operationTimeoutCheckInterval)) {
            operationManager.checkTimeoutsAndRetry(connection);
            subscriptionManager.checkTimeoutsAndRetry(connection);
            lastOperationTimeoutCheck.update();
        }
    }

    private void gotoIdentificationPhase() {
        if (connection == null) {
            logger.debug("connection was null when going to Identification Phase, going to Reconnecting Phase instead");
            gotoReconnectingPhase();
        } else {
            connectingPhase = ConnectingPhase.IDENTIFICATION;
        }
    }

    private void gotoConnectedPhase() {
        if (connection == null) {
            logger.debug("connection was null when going to Connected Phase, going to Reconnecting Phase instead");
            gotoReconnectingPhase();
        } else {
            connectingPhase = ConnectingPhase.CONNECTED;
            reconnectionInfo.reset();
            fireEvent(Events.clientConnected((InetSocketAddress) connection.remoteAddress()));
            checkOperationTimeout();
        }
    }

    private void gotoReconnectingPhase() {
        connectingPhase = ConnectingPhase.RECONNECTING;
        reconnectionInfo.touch();
    }

    private void reconnectTo(NodeEndpoints endpoints) {
        InetSocketAddress endpoint = (settings.sslSettings.useSslConnection && endpoints.secureTcpEndpoint != null) ?
            endpoints.secureTcpEndpoint : endpoints.tcpEndpoint;

        if (endpoint == null) {
            handle(new CloseConnection("No endpoint is specified while trying to reconnect."));
        } else if (connectionState() == ConnectionState.CONNECTED && !connection.remoteAddress().equals(endpoint)) {
            String message = String.format("Connection '%s': going to reconnect to [%s]. Current endpoint: [%s, L%s].",
                connection.id(), endpoint, connection.remoteAddress(), connection.localAddress());

            logger.trace(message);

            closeTcpConnection(message);

            connectingPhase = ConnectingPhase.ENDPOINT_DISCOVERY;
            handle(new EstablishTcpConnection(endpoints));
        }
    }

    private void discoverEndpoint(CompletableFuture<Void> result) {
        logger.debug("Discovering endpoint...");

        if (connectionState() == ConnectionState.INIT && connectingPhase == ConnectingPhase.RECONNECTING) {
            connectingPhase = ConnectingPhase.ENDPOINT_DISCOVERY;

            discoverer.discover(connection != null ? (InetSocketAddress) connection.remoteAddress() : null)
                .whenComplete((nodeEndpoints, throwable) -> {
                    if (throwable == null) {
                        tasks.enqueue(new EstablishTcpConnection(nodeEndpoints));
                        if (result != null) {
                            result.complete(null);
                        }
                    } else {
                        tasks.enqueue(new CloseConnection("Failed to resolve TCP endpoint to which to connect.", throwable));
                        if (result != null) {
                            result.completeExceptionally(new CannotEstablishConnectionException("Cannot resolve target end point.", throwable));
                        }
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
            gotoReconnectingPhase();
        }
    }

    private void onTcpConnectionClosed() {
        if (connection != null) {
            subscriptionManager.purgeSubscribedAndDropped(connection.id());
            fireEvent(Events.connectionClosed());
        }

        connection = null;
        gotoReconnectingPhase();
    }

    private void handle(StartConnection task) {
        logger.debug("StartConnection");

        switch (connectionState()) {
            case INIT:
                if (connectingPhase != ConnectingPhase.ENDPOINT_DISCOVERY) {
                    connectingPhase = ConnectingPhase.RECONNECTING;
                }
                discoverEndpoint(task.result);
                break;
            case CONNECTING:
            case CONNECTED:
                task.result.completeExceptionally(new IllegalStateException(String.format("Connection %s is already active.", connection)));
                break;
            case CLOSED:
                task.result.completeExceptionally(new ConnectionClosedException("Connection is closed"));
                break;
            default:
                throw new IllegalStateException("Unknown connection state");
        }
    }

    private void handle(EstablishTcpConnection task) {
        InetSocketAddress endpoint = (settings.sslSettings.useSslConnection && task.endpoints.secureTcpEndpoint != null) ?
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
        if (task.throwable != null) {
            logger.error(task.reason, task.throwable);
        }

        if (connectionState() == ConnectionState.CLOSED) {
            logger.debug("CloseConnection IGNORED because connection is CLOSED, reason: {}", task.reason);
        } else {
            logger.debug("CloseConnection, reason: {}", task.reason);

            if (task.throwable != null) {
                fireEvent(Events.errorOccurred(task.throwable));
            }

            disconnect(task.reason, task.throwable);
        }
    }

    private void handle(StartOperation task) {
        Operation operation = task.operation;

        switch (connectionState()) {
            case INIT:
                if (connectingPhase == ConnectingPhase.INVALID) {
                    operation.fail(new IllegalStateException("No connection"));
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
                    task.result.completeExceptionally(new IllegalStateException("No connection"));
                    break;
                }
            case CONNECTING:
            case CONNECTED:
                VolatileSubscriptionOperation operation = new VolatileSubscriptionOperation(
                    task.result,
                    task.streamId, task.resolveLinkTos, task.userCredentials, task.listener,
                    () -> connection, executor());

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
                    task.result.completeExceptionally(new IllegalStateException("No connection"));
                    break;
                }
            case CONNECTING:
            case CONNECTED:
                PersistentSubscriptionOperation operation = new PersistentSubscriptionOperation(
                    task.result,
                    task.subscriptionId, task.streamId, task.bufferSize, task.userCredentials, task.listener,
                    () -> connection, executor());

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
        synchronized (mutex) {
            if (!isRunning()) {
                connect();
            }
        }
        logger.trace("enqueueing task {}.", task.getClass().getSimpleName());
        tasks.enqueue(task);
    }

    private ConnectionState connectionState() {
        if (connection == null) {
            return ConnectionState.INIT;
        } else if (connection.isOpen()) {
            return (connection.isActive() && (connectingPhase == ConnectingPhase.CONNECTED)) ?
                ConnectionState.CONNECTED : ConnectionState.CONNECTING;
        } else {
            return ConnectionState.CLOSED;
        }
    }

    private class TransactionManagerImpl implements TransactionManager {

        @Override
        public CompletableFuture<Void> write(Transaction transaction, Iterable<EventData> events, UserCredentials userCredentials) {
            checkNotNull(transaction, "transaction is null");
            checkNotNull(events, "events is null");

            CompletableFuture<Void> result = new CompletableFuture<>();
            enqueue(new TransactionalWriteOperation(result, settings.requireMaster, transaction.transactionId, events, userCredentials));
            return result;
        }

        @Override
        public CompletableFuture<WriteResult> commit(Transaction transaction, UserCredentials userCredentials) {
            checkNotNull(transaction, "transaction is null");

            CompletableFuture<WriteResult> result = new CompletableFuture<>();
            enqueue(new CommitTransactionOperation(result, settings.requireMaster, transaction.transactionId, userCredentials));
            return result;
        }
    }

    private static class ReconnectionInfo {
        int reconnectionAttempt;
        final SystemTime timestamp = SystemTime.zero();

        void inc() {
            reconnectionAttempt++;
            touch();
        }

        void reset() {
            reconnectionAttempt = 0;
            touch();
        }

        void touch() {
            timestamp.update();
        }
    }
}
