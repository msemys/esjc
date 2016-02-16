package com.github.msemys.esjc.task;

import com.github.msemys.esjc.node.NodeEndpoints;

public class EstablishTcpConnection implements Task {
    public final NodeEndpoints endpoints;

    public EstablishTcpConnection(NodeEndpoints endpoints) {
        this.endpoints = endpoints;
    }
}
