package com.github.msemys.esjc.node;

import java.net.InetSocketAddress;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;

public class NodeEndpoints {
    public final InetSocketAddress tcpEndpoint;
    public final InetSocketAddress secureTcpEndpoint;

    public NodeEndpoints(InetSocketAddress tcpEndpoint, InetSocketAddress secureTcpEndpoint) {
        checkArgument(tcpEndpoint != null || secureTcpEndpoint != null, "Both endpoints are null.");
        this.tcpEndpoint = tcpEndpoint;
        this.secureTcpEndpoint = secureTcpEndpoint;
    }

    @Override
    public String toString() {
        return String.format("[%s, %s]",
            tcpEndpoint == null ? "n/a" : tcpEndpoint,
            secureTcpEndpoint == null ? "n/a" : secureTcpEndpoint);
    }
}
