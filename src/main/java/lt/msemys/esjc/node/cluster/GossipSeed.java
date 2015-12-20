package lt.msemys.esjc.node.cluster;

import java.net.InetSocketAddress;

public class GossipSeed {
    public final InetSocketAddress endPoint;
    public final String hostHeader;

    public GossipSeed(InetSocketAddress endPoint) {
        this(endPoint, "");
    }

    public GossipSeed(InetSocketAddress endPoint, String hostHeader) {
        this.endPoint = endPoint;
        this.hostHeader = hostHeader;
    }
}
