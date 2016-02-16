package com.github.msemys.esjc.event;

import java.net.InetSocketAddress;

public class ClientConnected implements Event {

    public final InetSocketAddress address;

    public ClientConnected(InetSocketAddress address) {
        this.address = address;
    }

}
