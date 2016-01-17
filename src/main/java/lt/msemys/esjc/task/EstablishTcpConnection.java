package lt.msemys.esjc.task;

import lt.msemys.esjc.node.NodeEndPoints;

public class EstablishTcpConnection implements Task {
    public final NodeEndPoints endPoints;

    public EstablishTcpConnection(NodeEndPoints endPoints) {
        this.endPoints = endPoints;
    }
}
