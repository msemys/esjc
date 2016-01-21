package lt.msemys.esjc.tcp.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lt.msemys.esjc.operation.UserCredentials;
import lt.msemys.esjc.tcp.TcpCommand;
import lt.msemys.esjc.tcp.TcpFlag;
import lt.msemys.esjc.tcp.TcpPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class AuthenticationHandler extends SimpleChannelInboundHandler<TcpPackage> {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationHandler.class);

    public enum AuthenticationStatus {
        SUCCESS, FAILED, TIMEOUT, IGNORED
    }

    private final Optional<UserCredentials> userCredentials;
    private final long timeoutMillis;
    private volatile ScheduledFuture<?> timeoutTask;
    private UUID correlationId;
    private Optional<Consumer<AuthenticationStatus>> completionConsumer = Optional.empty();

    public AuthenticationHandler(Optional<UserCredentials> userCredentials, Duration timeout) {
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
        if (userCredentials.isPresent()) {
            correlationId = UUID.randomUUID();

            ctx.writeAndFlush(TcpPackage.newBuilder()
                .command(TcpCommand.Authenticate)
                .flag(TcpFlag.Authenticated)
                .correlationId(correlationId)
                .login(userCredentials.get().username)
                .password(userCredentials.get().password)
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

    public AuthenticationHandler whenComplete(Consumer<AuthenticationStatus> consumer) {
        completionConsumer = Optional.of(consumer);
        return this;
    }

    private void complete(ChannelHandlerContext ctx, AuthenticationStatus status) {
        logger.debug("Authentication {}", status);
        ctx.channel().pipeline().remove(this);
        completionConsumer.ifPresent(c -> c.accept(status));
    }

    private void cancelTimeoutTask() {
        if (timeoutTask != null) {
            timeoutTask.cancel(true);
            timeoutTask = null;
        }
    }

}
