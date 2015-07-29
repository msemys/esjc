package lt.msemys.esjc.operation;

import lt.msemys.esjc.tcp.TcpPackage;

import java.util.UUID;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.ClientAPI/ClientOperations/IClientOperation.cs">EventStore.ClientAPI/ClientOperations/IClientOperation.cs</a>
 */
public interface Operation {

    TcpPackage create(UUID correlationId);

    InspectionResult inspect(TcpPackage tcpPackage);

    void fail(Exception exception);

}
