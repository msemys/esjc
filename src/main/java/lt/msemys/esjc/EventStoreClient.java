package lt.msemys.esjc;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import lt.msemys.esjc.tcp.TcpConnectionSupplier;
import lt.msemys.esjc.util.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventStoreClient {

    private final static Logger logger = LoggerFactory.getLogger(EventStoreClient.class);

    private final EventLoopGroup group;
    private final TcpConnectionSupplier tcpConnectionSupplier;
    private ChannelFuture connection;

    public EventStoreClient(String host, int port) {
        this(new Settings.Builder()
                .withAddress(host, port)
                .build());
    }

    public EventStoreClient(Settings settings) {
        logger.debug(settings.toString());
        this.group = new NioEventLoopGroup();
        this.tcpConnectionSupplier = new TcpConnectionSupplier(settings, group);
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
            connection = tcpConnectionSupplier.get();
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
