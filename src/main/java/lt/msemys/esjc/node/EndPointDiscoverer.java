package lt.msemys.esjc.node;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;

public interface EndPointDiscoverer {

    Future<NodeEndPoints> discover(InetSocketAddress failedTcpEndPoint);

}
