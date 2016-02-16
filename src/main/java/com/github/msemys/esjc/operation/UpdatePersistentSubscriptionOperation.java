package com.github.msemys.esjc.operation;

import com.google.protobuf.MessageLite;
import com.github.msemys.esjc.InvalidOperationException;
import com.github.msemys.esjc.PersistentSubscriptionSettings;
import com.github.msemys.esjc.PersistentSubscriptionUpdateResult;
import com.github.msemys.esjc.PersistentSubscriptionUpdateStatus;
import com.github.msemys.esjc.proto.EventStoreClientMessages.UpdatePersistentSubscription;
import com.github.msemys.esjc.proto.EventStoreClientMessages.UpdatePersistentSubscriptionCompleted;
import com.github.msemys.esjc.system.SystemConsumerStrategies;
import com.github.msemys.esjc.tcp.TcpCommand;

import java.util.concurrent.CompletableFuture;

public class UpdatePersistentSubscriptionOperation extends AbstractOperation<PersistentSubscriptionUpdateResult, UpdatePersistentSubscriptionCompleted> {
    private final String stream;
    private final String groupName;
    private final PersistentSubscriptionSettings settings;

    public UpdatePersistentSubscriptionOperation(CompletableFuture<PersistentSubscriptionUpdateResult> result,
                                                 String stream,
                                                 String groupName,
                                                 PersistentSubscriptionSettings settings,
                                                 UserCredentials userCredentials) {
        super(result, TcpCommand.UpdatePersistentSubscription, TcpCommand.UpdatePersistentSubscriptionCompleted, userCredentials);
        this.stream = stream;
        this.groupName = groupName;
        this.settings = settings;
    }

    @Override
    protected MessageLite createRequestMessage() {
        return UpdatePersistentSubscription.newBuilder()
            .setSubscriptionGroupName(groupName)
            .setEventStreamId(stream)
            .setResolveLinkTos(settings.resolveLinkTos)
            .setStartFrom(settings.startFrom)
            .setMessageTimeoutMilliseconds((int) settings.messageTimeout.toMillis())
            .setRecordStatistics(settings.timingStatistics)
            .setReadBatchSize(settings.readBatchSize)
            .setMaxRetryCount(settings.maxRetryCount)
            .setLiveBufferSize(settings.liveBufferSize)
            .setBufferSize(settings.historyBufferSize)
            .setPreferRoundRobin(settings.namedConsumerStrategies.equals(SystemConsumerStrategies.ROUND_ROBIN))
            .setCheckpointAfterTime((int) settings.checkPointAfter.toMillis())
            .setCheckpointMinCount(settings.minCheckPointCount)
            .setCheckpointMaxCount(settings.maxCheckPointCount)
            .setSubscriberMaxCount(settings.maxSubscriberCount)
            .setNamedConsumerStrategy(settings.namedConsumerStrategies)
            .build();
    }

    @Override
    protected UpdatePersistentSubscriptionCompleted createResponseMessage() {
        return UpdatePersistentSubscriptionCompleted.getDefaultInstance();
    }

    @Override
    protected InspectionResult inspectResponseMessage(UpdatePersistentSubscriptionCompleted response) {
        switch (response.getResult()) {
            case Success:
                succeed();
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("Success")
                    .build();
            case Fail:
                fail(new InvalidOperationException(String.format("Subscription group %s on stream %s failed '%s'", groupName, stream, response.getReason())));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("Fail")
                    .build();
            case AccessDenied:
                fail(new AccessDeniedException(String.format("Write access denied for stream '%s'.", stream)));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("AccessDenied")
                    .build();
            case DoesNotExist:
                fail(new InvalidOperationException(String.format("Subscription group %s on stream %s does not exist", groupName, stream)));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("DoesNotExist")
                    .build();
            default:
                throw new IllegalArgumentException("Unexpected OperationResult: " + response.getResult());
        }
    }

    @Override
    protected PersistentSubscriptionUpdateResult transformResponseMessage(UpdatePersistentSubscriptionCompleted response) {
        return new PersistentSubscriptionUpdateResult(PersistentSubscriptionUpdateStatus.Success);
    }

    @Override
    public String toString() {
        return String.format("Stream: %s, Group Name: %s", stream, groupName);
    }

}
