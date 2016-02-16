package com.github.msemys.esjc.event;

import java.net.InetSocketAddress;

public class Events {
    private static final AuthenticationFailed AUTHENTICATION_FAILED_EVENT = new AuthenticationFailed();
    private static final ClientDisconnected CLIENT_DISCONNECTED_EVENT = new ClientDisconnected();
    private static final ClientReconnecting CLIENT_RECONNECTING_EVENT = new ClientReconnecting();
    private static final ConnectionClosed CONNECTION_CLOSED_EVENT = new ConnectionClosed();

    private Events() {
    }

    public static AuthenticationFailed authenticationFailed() {
        return AUTHENTICATION_FAILED_EVENT;
    }

    public static ClientConnected clientConnected(InetSocketAddress address) {
        return new ClientConnected(address);
    }

    public static ClientDisconnected clientDisconnected() {
        return CLIENT_DISCONNECTED_EVENT;
    }

    public static ClientReconnecting clientReconnecting() {
        return CLIENT_RECONNECTING_EVENT;
    }

    public static ConnectionClosed connectionClosed() {
        return CONNECTION_CLOSED_EVENT;
    }

    public static ErrorOccurred errorOccurred(Throwable throwable) {
        return new ErrorOccurred(throwable);
    }

}
