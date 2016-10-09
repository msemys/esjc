package com.github.msemys.esjc;

import com.github.msemys.esjc.event.Event;
import com.github.msemys.esjc.node.NodeEndpoints;
import com.github.msemys.esjc.operation.manager.OperationManager;
import com.github.msemys.esjc.ssl.CommonNameTrustManagerFactory;
import com.github.msemys.esjc.subscription.manager.SubscriptionManager;
import com.github.msemys.esjc.tcp.TcpPackage;
import com.github.msemys.esjc.tcp.TcpPackageDecoder;
import com.github.msemys.esjc.tcp.TcpPackageEncoder;
import com.github.msemys.esjc.tcp.handler.AuthenticationHandler;
import com.github.msemys.esjc.tcp.handler.AuthenticationHandler.AuthenticationStatus;
import com.github.msemys.esjc.tcp.handler.HeartbeatHandler;
import com.github.msemys.esjc.tcp.handler.OperationHandler;
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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

abstract class AbstractEventStore implements IEventStore {
    private static final int MAX_FRAME_LENGTH = 64 * 1024 * 1024;

    protected enum ConnectionState {INIT, CONNECTING, CONNECTED, CLOSED}

    protected enum ConnectingPhase {INVALID, RECONNECTING, ENDPOINT_DISCOVERY, CONNECTION_ESTABLISHING, AUTHENTICATION, CONNECTED}

    protected final EventLoopGroup group = new NioEventLoopGroup(0, new DefaultThreadFactory("esio"));
    protected final Bootstrap bootstrap;
    protected final OperationManager operationManager;
    protected final SubscriptionManager subscriptionManager;
    public final Settings settings;

    protected volatile Channel connection;
    protected volatile ConnectingPhase connectingPhase = ConnectingPhase.INVALID;

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

                    if (settings.sslSettings.useSslConnection) {
                        SslContext sslContext = SslContextBuilder.forClient()
                            .trustManager(settings.sslSettings.validateServerCertificate ?
                                new CommonNameTrustManagerFactory(settings.sslSettings.certificateCommonName) :
                                InsecureTrustManagerFactory.INSTANCE)
                            .build();
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
                        .whenComplete(AbstractEventStore.this::onAuthenticationCompleted));
                    pipeline.addLast("operation-handler", new OperationHandler(operationManager, subscriptionManager)
                        .whenBadRequest(AbstractEventStore.this::onBadRequest)
                        .whenChannelError(AbstractEventStore.this::onChannelError)
                        .whenReconnect(AbstractEventStore.this::onReconnect));
                }
            });

        operationManager = new OperationManager(settings);
        subscriptionManager = new SubscriptionManager(settings);

        this.settings = settings;
    }

    @Override
    public Settings settings() {
        return settings;
    }

    @Override
    public CompletableFuture<PersistentSubscription> subscribeToPersistent(String stream,
                                                                           String groupName,
                                                                           PersistentSubscriptionListener listener) {
        return subscribeToPersistent(stream, groupName, listener, null, settings.persistentSubscriptionBufferSize, settings.persistentSubscriptionAutoAckEnabled);
    }

    @Override
    public CompletableFuture<PersistentSubscription> subscribeToPersistent(String stream,
                                                                           String groupName,
                                                                           PersistentSubscriptionListener listener,
                                                                           UserCredentials userCredentials) {
        return subscribeToPersistent(stream, groupName, listener, userCredentials, settings.persistentSubscriptionBufferSize, settings.persistentSubscriptionAutoAckEnabled);
    }

    @Override
    public void addListener(EventStoreListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(EventStoreListener listener) {
        listeners.remove(listener);
    }

    protected Executor executor() {
        return settings.executor;
    }

    protected void fireEvent(Event event) {
        executor().execute(() -> listeners.forEach(l -> l.onEvent(event)));
    }

    protected abstract void onAuthenticationCompleted(AuthenticationStatus status);

    protected abstract void onBadRequest(TcpPackage tcpPackage);

    protected abstract void onChannelError(Throwable throwable);

    protected abstract void onReconnect(NodeEndpoints nodeEndpoints);

    protected ConnectionState connectionState() {
        if (connection == null) {
            return ConnectionState.INIT;
        } else if (connection.isOpen()) {
            return (connection.isActive() && (connectingPhase == ConnectingPhase.CONNECTED)) ?
                ConnectionState.CONNECTED : ConnectionState.CONNECTING;
        } else {
            return ConnectionState.CLOSED;
        }
    }

}
