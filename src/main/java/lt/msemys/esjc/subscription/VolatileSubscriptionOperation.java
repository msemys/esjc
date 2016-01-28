package lt.msemys.esjc.subscription;

import com.google.protobuf.MessageLite;
import io.netty.channel.Channel;
import lt.msemys.esjc.ResolvedEvent;
import lt.msemys.esjc.SubscriptionListener;
import lt.msemys.esjc.operation.InspectionDecision;
import lt.msemys.esjc.operation.InspectionResult;
import lt.msemys.esjc.operation.UserCredentials;
import lt.msemys.esjc.proto.EventStoreClientMessages.StreamEventAppeared;
import lt.msemys.esjc.proto.EventStoreClientMessages.SubscribeToStream;
import lt.msemys.esjc.proto.EventStoreClientMessages.SubscriptionConfirmation;
import lt.msemys.esjc.tcp.TcpCommand;
import lt.msemys.esjc.tcp.TcpPackage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class VolatileSubscriptionOperation extends AbstractSubscriptionOperation<VolatileSubscription> {

    public VolatileSubscriptionOperation(CompletableFuture<VolatileSubscription> result,
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
    protected VolatileSubscription createSubscription(long lastCommitPosition, Integer lastEventNumber) {
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
