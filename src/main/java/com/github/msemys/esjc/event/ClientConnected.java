package com.github.msemys.esjc.event;

import java.net.InetSocketAddress;

/**
 * Fired when a client connects to an Event Store server.
 */
public class ClientConnected implements Event {

    public final InetSocketAddress address;

    public ClientConnected(InetSocketAddress address) {
        this.address = address;
    }

}
