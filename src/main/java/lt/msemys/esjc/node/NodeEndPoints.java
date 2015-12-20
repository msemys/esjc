package lt.msemys.esjc.node;

import java.net.InetSocketAddress;

import static lt.msemys.esjc.util.Preconditions.checkArgument;

public class NodeEndPoints {
    public final InetSocketAddress tcpEndPoint;
    public final InetSocketAddress secureTcpEndPoint;

    public NodeEndPoints(InetSocketAddress tcpEndPoint, InetSocketAddress secureTcpEndPoint) {
        checkArgument(tcpEndPoint != null || secureTcpEndPoint != null, "Both endpoints are null.");
        this.tcpEndPoint = tcpEndPoint;
        this.secureTcpEndPoint = secureTcpEndPoint;
    }

    @Override
    public String toString() {
        return String.format("[%s, %s]",
            tcpEndPoint == null ? "n/a" : tcpEndPoint,
            secureTcpEndPoint == null ? "n/a" : secureTcpEndPoint);
    }
}
