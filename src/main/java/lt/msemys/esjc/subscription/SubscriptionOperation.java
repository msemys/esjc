package lt.msemys.esjc.subscription;

import io.netty.channel.Channel;
import lt.msemys.esjc.SubscriptionDropReason;
import lt.msemys.esjc.operation.InspectionResult;
import lt.msemys.esjc.tcp.TcpPackage;

import java.util.UUID;

public interface SubscriptionOperation {

    boolean subscribe(UUID correlationId, Channel connection);

    default void drop(SubscriptionDropReason reason, Exception exception) {
        drop(reason, exception, null);
    }

    void drop(SubscriptionDropReason reason, Exception exception, Channel connection);

    InspectionResult inspect(TcpPackage tcpPackage);

    void connectionClosed();

}
