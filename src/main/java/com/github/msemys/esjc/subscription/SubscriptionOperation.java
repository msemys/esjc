package com.github.msemys.esjc.subscription;

import io.netty.channel.Channel;
import com.github.msemys.esjc.SubscriptionDropReason;
import com.github.msemys.esjc.operation.InspectionResult;
import com.github.msemys.esjc.tcp.TcpPackage;

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
