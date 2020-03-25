package com.github.msemys.esjc.node.single;

import com.github.msemys.esjc.node.EndpointDiscoverer;
import com.github.msemys.esjc.node.NodeEndpoints;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

public class SingleEndpointDiscoverer implements EndpointDiscoverer {

    private final SingleNodeSettings settings;
    private final boolean ssl;

    public SingleEndpointDiscoverer(SingleNodeSettings settings, boolean ssl) {
        checkNotNull(settings, "settings is null");
        this.settings = settings;
        this.ssl = ssl;
    }

    @Override
    public CompletableFuture<NodeEndpoints> discover(InetSocketAddress failedTcpEndpoint) {
        return CompletableFuture.completedFuture(new NodeEndpoints(
                ssl ? null : settings.address,
                ssl ? settings.address : null));
    }

}
