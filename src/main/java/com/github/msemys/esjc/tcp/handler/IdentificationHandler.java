package com.github.msemys.esjc.tcp.handler;

import com.github.msemys.esjc.proto.EventStoreClientMessages.IdentifyClient;
import com.github.msemys.esjc.tcp.TcpCommand;
import com.github.msemys.esjc.tcp.TcpPackage;
import com.github.msemys.esjc.tcp.handler.AuthenticationHandler.AuthenticationStatus;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class IdentificationHandler extends SimpleChannelInboundHandler<TcpPackage> {
    private static final Logger logger = LoggerFactory.getLogger(IdentificationHandler.class);

    private static final int CLIENT_VERSION = 1;

    public enum IdentificationStatus {
        SUCCESS, FAILED, TIMEOUT
    }

    private final String connectionName;
    private final long timeoutMillis;
    private ScheduledFuture<?> timeoutTask;
    private final Object timeoutTaskLock = new Object();
    private UUID correlationId;
    private Consumer<IdentificationStatus> completionConsumer;

    public IdentificationHandler(String connectionName, Duration timeout) {
        this.connectionName = connectionName;
        this.timeoutMillis = timeout.toMillis();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpPackage msg) throws Exception {
        if (timeoutTask != null && !timeoutTask.isDone() && msg.correlationId.equals(correlationId)) {
            switch (msg.command) {
                case ClientIdentified:
                    cancelTimeoutTask();
                    complete(ctx, IdentificationStatus.SUCCESS);
                    break;
                default:
                    cancelTimeoutTask();
                    complete(ctx, IdentificationStatus.FAILED);
                    ctx.close();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        cancelTimeoutTask();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof AuthenticationEvent) {
            AuthenticationStatus authenticationStatus = ((AuthenticationEvent) evt).status;

            if (authenticationStatus == AuthenticationStatus.SUCCESS || authenticationStatus == AuthenticationStatus.IGNORED) {
                synchronized (timeoutTaskLock) {
                    if (timeoutTask == null) {
                        correlationId = UUID.randomUUID();

                        ctx.writeAndFlush(TcpPackage.newBuilder()
                            .command(TcpCommand.IdentifyClient)
                            .correlationId(correlationId)
                            .data(IdentifyClient.newBuilder()
                                .setVersion(CLIENT_VERSION)
                                .setConnectionName(connectionName)
                                .build()
                                .toByteArray())
                            .build());

                        timeoutTask = ctx.executor().schedule(() -> {
                            complete(ctx, IdentificationStatus.TIMEOUT);
                            ctx.close();
                        }, timeoutMillis, MILLISECONDS);
                    }
                }
            }
        }
    }

    public IdentificationHandler whenComplete(Consumer<IdentificationStatus> consumer) {
        checkNotNull(consumer, "consumer is null");
        completionConsumer = consumer;
        return this;
    }

    private void complete(ChannelHandlerContext ctx, IdentificationStatus status) {
        logger.info("Identification [{}] {}", connectionName, status);
        ctx.channel().pipeline().remove(this);

        if (completionConsumer != null) {
            completionConsumer.accept(status);
        }
    }

    private void cancelTimeoutTask() {
        synchronized (timeoutTaskLock) {
            if (timeoutTask != null) {
                timeoutTask.cancel(true);
                timeoutTask = null;
            }
        }
    }

}
