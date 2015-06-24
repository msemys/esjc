package lt.msemys.esjc;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import lt.msemys.esjc.tcp.TcpChannelInitializer;
import lt.msemys.esjc.util.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventStoreClient {

    private final static Logger logger = LoggerFactory.getLogger(EventStoreClient.class);

    private final Settings settings;
    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private ChannelFuture connection;

    public EventStoreClient(String host, int port) {
        this(new Settings.Builder()
                .withHost(host)
                .withPort(port)
                .build());
    }

    public EventStoreClient(Settings settings) {
        this.settings = settings;
        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap()
                .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
                .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new TcpChannelInitializer());
    }

    public EventStoreClient connect() {
        try {
            connection().sync();
        } catch (InterruptedException e) {
            Throwables.propagate(e);
        }
        return this;
    }

    private ChannelFuture connection() {
        if (connection == null) {
            logger.debug("connecting: {}", settings);
            connection = bootstrap.connect(settings.host, settings.port);
        }
        return connection;
    }

    private Channel channel() {
        return connection().channel();
    }

    public Future shutdownGracefully() {
        logger.debug("shutting down...");
        return group.shutdownGracefully();
    }

}
