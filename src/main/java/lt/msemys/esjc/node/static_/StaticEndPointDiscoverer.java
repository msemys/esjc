package lt.msemys.esjc.node.static_;

import lt.msemys.esjc.node.EndPointDiscoverer;
import lt.msemys.esjc.node.NodeEndPoints;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;

public class StaticEndPointDiscoverer implements EndPointDiscoverer {

    private final Future<NodeEndPoints> result;

    public StaticEndPointDiscoverer(InetSocketAddress endPoint, boolean ssl) {
        checkNotNull(endPoint, "Endpoint is null");
        result = CompletableFuture.completedFuture(new NodeEndPoints(ssl ? null : endPoint, ssl ? endPoint : null));
    }

    @Override
    public Future<NodeEndPoints> discover(InetSocketAddress failedTcpEndPoint) {
        return result;
    }

}
