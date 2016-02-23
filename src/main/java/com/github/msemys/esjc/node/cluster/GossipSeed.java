package com.github.msemys.esjc.node.cluster;

import java.net.InetSocketAddress;

/**
 * Represents a source of cluster gossip.
 */
public class GossipSeed {

    /**
     * The endpoint for the External HTTP endpoint of the gossip seed.
     * <p>
     * The HTTP endpoint is used rather than the TCP endpoint because it is required
     * for the client to exchange gossip with the server. The standard port which should be
     * used here is 2113.
     * </p>
     */
    public final InetSocketAddress endpoint;

    /**
     * The host header to be sent when requesting gossip.
     */
    public final String hostHeader;

    /**
     * Creates a new instance with the specified endpoint and empty host header.
     *
     * @param endpoint the endpoint for the External HTTP endpoint of the gossip seed.
     */
    public GossipSeed(InetSocketAddress endpoint) {
        this(endpoint, "");
    }

    /**
     * Creates a new instance with the specified endpoint and host header.
     *
     * @param endpoint   the endpoint for the External HTTP endpoint of the gossip seed.
     * @param hostHeader the host header to be sent when requesting gossip.
     */
    public GossipSeed(InetSocketAddress endpoint, String hostHeader) {
        this.endpoint = endpoint;
        this.hostHeader = hostHeader;
    }
}
