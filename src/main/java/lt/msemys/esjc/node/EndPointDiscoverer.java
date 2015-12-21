package lt.msemys.esjc.node;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public interface EndPointDiscoverer {

    CompletableFuture<NodeEndPoints> discover(InetSocketAddress failedTcpEndPoint);

}
