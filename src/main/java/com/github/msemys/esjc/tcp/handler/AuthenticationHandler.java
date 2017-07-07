package com.github.msemys.esjc.tcp.handler;

import com.github.msemys.esjc.UserCredentials;
import com.github.msemys.esjc.tcp.TcpCommand;
import com.github.msemys.esjc.tcp.TcpFlag;
import com.github.msemys.esjc.tcp.TcpPackage;
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

public class AuthenticationHandler extends SimpleChannelInboundHandler<TcpPackage> {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationHandler.class);

    public enum AuthenticationStatus {
        SUCCESS, FAILED, TIMEOUT, IGNORED
    }

    private final UserCredentials userCredentials;
    private final long timeoutMillis;
    private volatile ScheduledFuture<?> timeoutTask;
    private UUID correlationId;
    private Consumer<AuthenticationStatus> completionConsumer;

    public AuthenticationHandler(UserCredentials userCredentials, Duration timeout) {
        this.userCredentials = userCredentials;
        this.timeoutMillis = timeout.toMillis();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpPackage msg) throws Exception {
        if (timeoutTask != null && !timeoutTask.isDone() && msg.correlationId.equals(correlationId)) {
            switch (msg.command) {
                case Authenticated:
                    cancelTimeoutTask();
                    complete(ctx, AuthenticationStatus.SUCCESS);
                    break;
                case NotAuthenticated:
                    cancelTimeoutTask();
                    complete(ctx, AuthenticationStatus.FAILED);
                    ctx.close();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (userCredentials != null) {
            correlationId = UUID.randomUUID();

            ctx.writeAndFlush(TcpPackage.newBuilder()
                .command(TcpCommand.Authenticate)
                .flag(TcpFlag.Authenticated)
                .correlationId(correlationId)
                .login(userCredentials.username)
                .password(userCredentials.password)
                .build());

            timeoutTask = ctx.executor().schedule(() -> {
                complete(ctx, AuthenticationStatus.TIMEOUT);
                ctx.close();
            }, timeoutMillis, MILLISECONDS);
        } else {
            ctx.fireChannelActive();
            complete(ctx, AuthenticationStatus.IGNORED);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        cancelTimeoutTask();
    }

    public AuthenticationHandler whenComplete(Consumer<AuthenticationStatus> consumer) {
        checkNotNull(consumer, "consumer is null");
        completionConsumer = consumer;
        return this;
    }

    private void complete(ChannelHandlerContext ctx, AuthenticationStatus status) {
        logger.info("Authentication {}", status);
        ctx.channel().pipeline().remove(this);

        if (completionConsumer != null) {
            completionConsumer.accept(status);
        }

        ctx.fireUserEventTriggered(new AuthenticationEvent(status));
    }

    private void cancelTimeoutTask() {
        if (timeoutTask != null) {
            timeoutTask.cancel(true);
            timeoutTask = null;
        }
    }

}
