package lt.msemys.esjc.node.cluster;

import java.net.InetSocketAddress;

public class GossipSeed {
    public final InetSocketAddress endpoint;
    public final String hostHeader;

    public GossipSeed(InetSocketAddress endpoint) {
        this(endpoint, "");
    }

    public GossipSeed(InetSocketAddress endpoint, String hostHeader) {
        this.endpoint = endpoint;
        this.hostHeader = hostHeader;
    }
}
