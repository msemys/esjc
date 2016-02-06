package lt.msemys.esjc;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lt.msemys.esjc.event.Event;
import lt.msemys.esjc.node.NodeEndPoints;
import lt.msemys.esjc.operation.UserCredentials;
import lt.msemys.esjc.operation.manager.OperationManager;
import lt.msemys.esjc.subscription.VolatileSubscription;
import lt.msemys.esjc.subscription.manager.SubscriptionManager;
import lt.msemys.esjc.tcp.TcpPackage;
import lt.msemys.esjc.tcp.TcpPackageDecoder;
import lt.msemys.esjc.tcp.TcpPackageEncoder;
import lt.msemys.esjc.tcp.handler.AuthenticationHandler;
import lt.msemys.esjc.tcp.handler.AuthenticationHandler.AuthenticationStatus;
import lt.msemys.esjc.tcp.handler.HeartbeatHandler;
import lt.msemys.esjc.tcp.handler.OperationHandler;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static lt.msemys.esjc.util.Preconditions.checkNotNull;

abstract class AbstractEventStore {
    private static final int MAX_FRAME_LENGTH = 64 * 1024 * 1024;

    protected enum ConnectionState {INIT, CONNECTING, CONNECTED, CLOSED}

    protected final Executor executor = Executors.newCachedThreadPool(new EventStoreThreadFactory());
    protected final EventLoopGroup group = new NioEventLoopGroup(0, new DefaultThreadFactory("esio"));
    protected final Bootstrap bootstrap;
    protected final OperationManager operationManager;
    protected final SubscriptionManager subscriptionManager;
    protected final Settings settings;

    protected volatile Channel connection;

    private final Set<EventStoreListener> listeners = new CopyOnWriteArraySet<>();

    protected AbstractEventStore(Settings settings) {
        checkNotNull(settings, "settings");

        bootstrap = new Bootstrap()
            .option(ChannelOption.SO_KEEPALIVE, settings.tcpSettings.keepAlive)
            .option(ChannelOption.TCP_NODELAY, settings.tcpSettings.tcpNoDelay)
            .option(ChannelOption.SO_SNDBUF, settings.tcpSettings.sendBufferSize)
            .option(ChannelOption.SO_RCVBUF, settings.tcpSettings.receiveBufferSize)
            .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, settings.tcpSettings.writeBufferLowWaterMark)
            .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, settings.tcpSettings.writeBufferHighWaterMark)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) settings.tcpSettings.connectTimeout.toMillis())
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();

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
                        .whenComplete(AbstractEventStore.this::onAuthenticationCompleted));
                    pipeline.addLast("operation-handler", new OperationHandler(operationManager, subscriptionManager)
                        .whenBadRequest(AbstractEventStore.this::onBadRequest)
                        .whenReconnect(AbstractEventStore.this::onReconnect));
                }
            });

        operationManager = new OperationManager(settings);
        subscriptionManager = new SubscriptionManager(settings);

        this.settings = settings;
    }

    public CompletableFuture<DeleteResult> deleteStream(String stream,
                                                        ExpectedVersion expectedVersion) {
        return deleteStream(stream, expectedVersion, false, null);
    }

    public CompletableFuture<DeleteResult> deleteStream(String stream,
                                                        ExpectedVersion expectedVersion,
                                                        UserCredentials userCredentials) {
        return deleteStream(stream, expectedVersion, false, userCredentials);
    }

    public CompletableFuture<DeleteResult> deleteStream(String stream,
                                                        ExpectedVersion expectedVersion,
                                                        boolean hardDelete) {
        return deleteStream(stream, expectedVersion, hardDelete, null);
    }

    public abstract CompletableFuture<DeleteResult> deleteStream(String stream,
                                                                 ExpectedVersion expectedVersion,
                                                                 boolean hardDelete,
                                                                 UserCredentials userCredentials);

    public CompletableFuture<WriteResult> appendToStream(String stream,
                                                         ExpectedVersion expectedVersion,
                                                         Iterable<EventData> events) {
        return appendToStream(stream, expectedVersion, events, null);
    }

    public abstract CompletableFuture<WriteResult> appendToStream(String stream,
                                                                  ExpectedVersion expectedVersion,
                                                                  Iterable<EventData> events,
                                                                  UserCredentials userCredentials);

    public CompletableFuture<Transaction> startTransaction(String stream,
                                                           ExpectedVersion expectedVersion) {
        return startTransaction(stream, expectedVersion, null);
    }

    public abstract CompletableFuture<Transaction> startTransaction(String stream,
                                                                    ExpectedVersion expectedVersion,
                                                                    UserCredentials userCredentials);

    public Transaction continueTransaction(long transactionId) {
        return continueTransaction(transactionId, null);
    }

    public abstract Transaction continueTransaction(long transactionId,
                                                    UserCredentials userCredentials);

    public CompletableFuture<EventReadResult> readEvent(String stream,
                                                        int eventNumber,
                                                        boolean resolveLinkTos) {
        return readEvent(stream, eventNumber, resolveLinkTos, null);
    }

    public abstract CompletableFuture<EventReadResult> readEvent(String stream,
                                                                 int eventNumber,
                                                                 boolean resolveLinkTos,
                                                                 UserCredentials userCredentials);

    public CompletableFuture<StreamEventsSlice> readStreamEventsForward(String stream,
                                                                        int start,
                                                                        int count,
                                                                        boolean resolveLinkTos) {
        return readStreamEventsForward(stream, start, count, resolveLinkTos, null);
    }

    public abstract CompletableFuture<StreamEventsSlice> readStreamEventsForward(String stream,
                                                                                 int start,
                                                                                 int count,
                                                                                 boolean resolveLinkTos,
                                                                                 UserCredentials userCredentials);

    public CompletableFuture<StreamEventsSlice> readStreamEventsBackward(String stream,
                                                                         int start,
                                                                         int count,
                                                                         boolean resolveLinkTos) {
        return readStreamEventsBackward(stream, start, count, resolveLinkTos, null);
    }

    public abstract CompletableFuture<StreamEventsSlice> readStreamEventsBackward(String stream,
                                                                                  int start,
                                                                                  int count,
                                                                                  boolean resolveLinkTos,
                                                                                  UserCredentials userCredentials);

    public CompletableFuture<AllEventsSlice> readAllEventsForward(Position position,
                                                                  int maxCount,
                                                                  boolean resolveLinkTos) {
        return readAllEventsForward(position, maxCount, resolveLinkTos, null);
    }

    public abstract CompletableFuture<AllEventsSlice> readAllEventsForward(Position position,
                                                                           int maxCount,
                                                                           boolean resolveLinkTos,
                                                                           UserCredentials userCredentials);

    public CompletableFuture<AllEventsSlice> readAllEventsBackward(Position position,
                                                                   int maxCount,
                                                                   boolean resolveLinkTos) {
        return readAllEventsBackward(position, maxCount, resolveLinkTos, null);
    }

    public abstract CompletableFuture<AllEventsSlice> readAllEventsBackward(Position position,
                                                                            int maxCount,
                                                                            boolean resolveLinkTos,
                                                                            UserCredentials userCredentials);

    public CompletableFuture<VolatileSubscription> subscribeToStream(String stream,
                                                                     boolean resolveLinkTos,
                                                                     SubscriptionListener listener) {
        return subscribeToStream(stream, resolveLinkTos, listener, null);
    }

    public abstract CompletableFuture<VolatileSubscription> subscribeToStream(String stream,
                                                                              boolean resolveLinkTos,
                                                                              SubscriptionListener listener,
                                                                              UserCredentials userCredentials);

    public CompletableFuture<VolatileSubscription> subscribeToAll(boolean resolveLinkTos,
                                                                  SubscriptionListener listener) {
        return subscribeToAll(resolveLinkTos, listener, null);
    }

    public abstract CompletableFuture<VolatileSubscription> subscribeToAll(boolean resolveLinkTos,
                                                                           SubscriptionListener listener,
                                                                           UserCredentials userCredentials);

    public CatchUpSubscription subscribeToStreamFrom(String stream,
                                                     Integer lastCheckpoint,
                                                     boolean resolveLinkTos,
                                                     CatchUpSubscriptionListener listener,
                                                     int readBatchSize) {
        return subscribeToStreamFrom(stream, lastCheckpoint, resolveLinkTos, listener, null, readBatchSize);
    }

    public CatchUpSubscription subscribeToStreamFrom(String stream,
                                                     Integer lastCheckpoint,
                                                     boolean resolveLinkTos,
                                                     CatchUpSubscriptionListener listener) {
        return subscribeToStreamFrom(stream, lastCheckpoint, resolveLinkTos, listener, null, settings.readBatchSize);
    }

    public CatchUpSubscription subscribeToStreamFrom(String stream,
                                                     Integer lastCheckpoint,
                                                     boolean resolveLinkTos,
                                                     CatchUpSubscriptionListener listener,
                                                     UserCredentials userCredentials) {
        return subscribeToStreamFrom(stream, lastCheckpoint, resolveLinkTos, listener, userCredentials, settings.readBatchSize);
    }

    public abstract CatchUpSubscription subscribeToStreamFrom(String stream,
                                                              Integer lastCheckpoint,
                                                              boolean resolveLinkTos,
                                                              CatchUpSubscriptionListener listener,
                                                              UserCredentials userCredentials,
                                                              int readBatchSize);

    public CatchUpSubscription subscribeToAllFrom(Position fromPositionExclusive,
                                                  boolean resolveLinkTos,
                                                  CatchUpSubscriptionListener listener) {
        return subscribeToAllFrom(fromPositionExclusive, resolveLinkTos, listener, null, settings.readBatchSize);
    }

    public CatchUpSubscription subscribeToAllFrom(Position fromPositionExclusive,
                                                  boolean resolveLinkTos,
                                                  CatchUpSubscriptionListener listener,
                                                  int readBatchSize) {
        return subscribeToAllFrom(fromPositionExclusive, resolveLinkTos, listener, null, readBatchSize);
    }

    public CatchUpSubscription subscribeToAllFrom(Position fromPositionExclusive,
                                                  boolean resolveLinkTos,
                                                  CatchUpSubscriptionListener listener,
                                                  UserCredentials userCredentials) {
        return subscribeToAllFrom(fromPositionExclusive, resolveLinkTos, listener, userCredentials, settings.readBatchSize);
    }

    public abstract CatchUpSubscription subscribeToAllFrom(Position fromPositionExclusive,
                                                           boolean resolveLinkTos,
                                                           CatchUpSubscriptionListener listener,
                                                           UserCredentials userCredentials,
                                                           int readBatchSize);

    public void addListener(EventStoreListener listener) {
        listeners.add(listener);
    }

    public void removeListener(EventStoreListener listener) {
        listeners.remove(listener);
    }

    protected void fireEvent(Event event) {
        executor.execute(() -> listeners.forEach(l -> l.onEvent(event)));
    }

    protected abstract void onAuthenticationCompleted(AuthenticationStatus status);

    protected abstract void onBadRequest(TcpPackage tcpPackage);

    protected abstract void onReconnect(NodeEndPoints nodeEndPoints);

    protected ConnectionState connectionState() {
        if (connection == null) {
            return ConnectionState.INIT;
        } else if (connection.isOpen()) {
            return connection.isActive() ? ConnectionState.CONNECTED : ConnectionState.CONNECTING;
        } else {
            return ConnectionState.CLOSED;
        }
    }

    private static class EventStoreThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        private EventStoreThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "es-" + poolNumber.getAndIncrement() + "-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);

            if (t.isDaemon()) {
                t.setDaemon(false);
            }

            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }

            return t;
        }
    }

}
