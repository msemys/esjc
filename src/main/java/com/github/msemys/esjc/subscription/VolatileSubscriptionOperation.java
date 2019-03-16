package com.github.msemys.esjc.subscription;

import com.github.msemys.esjc.ResolvedEvent;
import com.github.msemys.esjc.Subscription;
import com.github.msemys.esjc.SubscriptionListener;
import com.github.msemys.esjc.UserCredentials;
import com.github.msemys.esjc.operation.InspectionDecision;
import com.github.msemys.esjc.operation.InspectionResult;
import com.github.msemys.esjc.proto.EventStoreClientMessages.StreamEventAppeared;
import com.github.msemys.esjc.proto.EventStoreClientMessages.SubscribeToStream;
import com.github.msemys.esjc.proto.EventStoreClientMessages.SubscriptionConfirmation;
import com.github.msemys.esjc.tcp.TcpCommand;
import com.github.msemys.esjc.tcp.TcpPackage;
import com.google.protobuf.MessageLite;
import io.netty.channel.Channel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class VolatileSubscriptionOperation extends AbstractSubscriptionOperation<VolatileSubscription, ResolvedEvent> {

    @SuppressWarnings("unchecked")
    public VolatileSubscriptionOperation(CompletableFuture<Subscription> result,
                                         String streamId,
                                         boolean resolveLinkTos,
                                         UserCredentials userCredentials,
                                         SubscriptionListener listener,
                                         Supplier<Channel> connectionSupplier,
                                         Executor executor) {
        super(result, TcpCommand.SubscribeToStream, streamId, resolveLinkTos, userCredentials, listener, connectionSupplier, executor);
    }

    @Override
    protected MessageLite createSubscribeMessage() {
        return SubscribeToStream.newBuilder()
            .setEventStreamId(streamId)
            .setResolveLinkTos(resolveLinkTos)
            .build();
    }

    @Override
    protected VolatileSubscription createSubscription(long lastCommitPosition, Long lastEventNumber) {
        return new VolatileSubscription(this, streamId, lastCommitPosition, lastEventNumber);
    }

    @Override
    protected boolean inspect(TcpPackage tcpPackage, InspectionResult.Builder builder) {
        switch (tcpPackage.command) {
            case SubscriptionConfirmation:
                SubscriptionConfirmation confirmation = newInstance(SubscriptionConfirmation.getDefaultInstance(), tcpPackage.data);
                confirmSubscription(confirmation.getLastCommitPosition(), confirmation.hasLastEventNumber() ? confirmation.getLastEventNumber() : null);
                builder.decision(InspectionDecision.Subscribed).description("SubscriptionConfirmation");
                return true;
            case StreamEventAppeared:
                StreamEventAppeared streamEventAppeared = newInstance(StreamEventAppeared.getDefaultInstance(), tcpPackage.data);
                eventAppeared(new ResolvedEvent(streamEventAppeared.getEvent()));
                builder.decision(InspectionDecision.DoNothing).description("StreamEventAppeared");
                return true;
            default:
                return false;
        }
    }
}
