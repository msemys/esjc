package lt.msemys.esjc.tcp;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class TcpChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final int MAX_FRAME_LENGTH = 64 * 1024 * 1024;

    private final TcpConnectionSupplier tcpConnectionSupplier;

    public TcpChannelInitializer(TcpConnectionSupplier tcpConnectionSupplier) {
        this.tcpConnectionSupplier = tcpConnectionSupplier;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // decoder
        pipeline.addLast("frame-decoder", new LengthFieldBasedFrameDecoder(LITTLE_ENDIAN, MAX_FRAME_LENGTH, 0, 4, 0, 4, true));
        pipeline.addLast("package-decoder", new TcpPackageDecoder());

        // encoder
        pipeline.addLast("frame-encoder", new LengthFieldPrepender(LITTLE_ENDIAN, 4, 0, false));
        pipeline.addLast("package-encoder", new TcpPackageEncoder());

        pipeline.addLast(new TcpChannelHandler(tcpConnectionSupplier));
    }

}
