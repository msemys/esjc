package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.tcp.TcpPackage;

import java.util.UUID;

public interface Operation {

    TcpPackage create(UUID correlationId);

    InspectionResult inspect(TcpPackage tcpPackage);

    void fail(Exception exception);

}
