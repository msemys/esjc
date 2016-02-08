package lt.msemys.esjc.task;

import lt.msemys.esjc.node.NodeEndpoints;

public class EstablishTcpConnection implements Task {
    public final NodeEndpoints endpoints;

    public EstablishTcpConnection(NodeEndpoints endpoints) {
        this.endpoints = endpoints;
    }
}
