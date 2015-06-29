package lt.msemys.esjc.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lt.msemys.esjc.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class TcpConnectionSupplier implements Supplier<ChannelFuture> {

    private final static Logger logger = LoggerFactory.getLogger(TcpConnectionSupplier.class);

    private final Settings settings;
    private final EventLoopGroup eventLoopGroup;
    private final TcpChannelInitializer tcpChannelInitializer;

    public TcpConnectionSupplier(Settings settings, EventLoopGroup eventLoopGroup) {
        this.settings = settings;
        this.eventLoopGroup = eventLoopGroup;
        this.tcpChannelInitializer = new TcpChannelInitializer(this);
    }

    @Override
    public ChannelFuture get() {
        return get(eventLoopGroup);
    }

    public ChannelFuture get(EventLoopGroup eventLoopGroup) {
        logger.info("Connecting to {}", settings.address);
        return newBootstrap(eventLoopGroup)
                .connect()
                .addListener((ChannelFuture future) -> {
                    if (!future.isSuccess()) {
                        logger.warn(future.cause().getMessage());

                        final EventLoop loop = future.channel().eventLoop();
                        loop.schedule(() -> get(loop), settings.reconnectionDelay.toMillis(), TimeUnit.MILLISECONDS);
                    }
                });
    }

    private Bootstrap newBootstrap(EventLoopGroup eventLoopGroup) {
        return new Bootstrap()
                .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, settings.writeBufferLowWaterMark)
                .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, settings.writeBufferHighWaterMark)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) settings.reconnectionDelay.toMillis())
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(tcpChannelInitializer)
                .remoteAddress(settings.address);
    }

}
