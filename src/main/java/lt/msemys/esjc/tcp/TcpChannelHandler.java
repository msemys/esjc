package lt.msemys.esjc.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class TcpChannelHandler extends SimpleChannelInboundHandler<TcpPackage> {

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

    private static void doHeartbeatResponse(ChannelHandlerContext ctx, TcpPackage msg) {
        ctx.writeAndFlush(new TcpPackage.Builder()
                .withCommand(TcpCommand.HeartbeatResponseCommand)
                .withCorrelationId(msg.correlationId)
                .build());
    }

}
