package lt.msemys.esjc.node.static_;

import lt.msemys.esjc.node.EndpointDiscoverer;
import lt.msemys.esjc.node.NodeEndpoints;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;

public class StaticEndpointDiscoverer implements EndpointDiscoverer {

    private final CompletableFuture<NodeEndpoints> result;

    public StaticEndpointDiscoverer(StaticNodeSettings settings, boolean ssl) {
        checkNotNull(settings, "settings is null");
        result = CompletableFuture.completedFuture(new NodeEndpoints(
            ssl ? null : settings.address,
            ssl ? settings.address : null));
    }

    @Override
    public CompletableFuture<NodeEndpoints> discover(InetSocketAddress failedTcpEndpoint) {
        return result;
    }

}
