package com.github.msemys.esjc.tcp.handler;

import com.github.msemys.esjc.tcp.handler.AuthenticationHandler.AuthenticationStatus;

public class AuthenticationEvent {
    public final AuthenticationStatus status;

    public AuthenticationEvent(AuthenticationStatus status) {
        this.status = status;
    }

}
