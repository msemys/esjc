package lt.msemys.esjc.tcp;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpChannelHandler extends SimpleChannelInboundHandler<TcpPackage> {

    private final static Logger logger = LoggerFactory.getLogger(TcpChannelHandler.class);

    private final TcpConnectionSupplier tcpConnectionSupplier;

    public TcpChannelHandler(TcpConnectionSupplier tcpConnectionSupplier) {
        this.tcpConnectionSupplier = tcpConnectionSupplier;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpPackage msg) throws Exception {
        switch (msg.command) {
            case HeartbeatRequestCommand:
                doHeartbeatResponse(ctx, msg);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported command received '%s'", msg.command));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("[{}] Connected to {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("[{}] Disconnected from {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());

        final EventLoop loop = ctx.channel().eventLoop();

        if (!loop.isShuttingDown()) {
            tcpConnectionSupplier.get(ctx.channel().eventLoop());
        }

        super.channelInactive(ctx);
    }

    private static ChannelFuture doHeartbeatResponse(ChannelHandlerContext ctx, TcpPackage msg) {
        return ctx.writeAndFlush(TcpPackage.newBuilder()
                .command(TcpCommand.HeartbeatResponseCommand)
                .correlationId(msg.correlationId)
                .build());
    }

}
