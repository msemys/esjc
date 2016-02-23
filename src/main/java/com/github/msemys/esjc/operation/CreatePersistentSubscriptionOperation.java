package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.PersistentSubscriptionCreateResult;
import com.github.msemys.esjc.PersistentSubscriptionCreateStatus;
import com.github.msemys.esjc.PersistentSubscriptionSettings;
import com.github.msemys.esjc.proto.EventStoreClientMessages.CreatePersistentSubscription;
import com.github.msemys.esjc.proto.EventStoreClientMessages.CreatePersistentSubscriptionCompleted;
import com.github.msemys.esjc.system.SystemConsumerStrategies;
import com.github.msemys.esjc.tcp.TcpCommand;
import com.google.protobuf.MessageLite;

import java.util.concurrent.CompletableFuture;

public class CreatePersistentSubscriptionOperation extends AbstractOperation<PersistentSubscriptionCreateResult, CreatePersistentSubscriptionCompleted> {
    private final String stream;
    private final String groupName;
    private final PersistentSubscriptionSettings settings;

    public CreatePersistentSubscriptionOperation(CompletableFuture<PersistentSubscriptionCreateResult> result,
                                                 String stream,
                                                 String groupName,
                                                 PersistentSubscriptionSettings settings,
                                                 UserCredentials userCredentials) {
        super(result, TcpCommand.CreatePersistentSubscription, TcpCommand.CreatePersistentSubscriptionCompleted, userCredentials);
        this.stream = stream;
        this.groupName = groupName;
        this.settings = settings;
    }

    @Override
    protected MessageLite createRequestMessage() {
        return CreatePersistentSubscription.newBuilder()
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
    protected CreatePersistentSubscriptionCompleted createResponseMessage() {
        return CreatePersistentSubscriptionCompleted.getDefaultInstance();
    }

    @Override
    protected InspectionResult inspectResponseMessage(CreatePersistentSubscriptionCompleted response) {
        switch (response.getResult()) {
            case Success:
                succeed();
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("Success")
                    .build();
            case Fail:
                fail(new IllegalStateException(String.format("Subscription group %s on stream %s failed '%s'", groupName, stream, response.getReason())));
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
            case AlreadyExists:
                fail(new IllegalStateException(String.format("Subscription group %s on stream %s already exists", groupName, stream)));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("AlreadyExists")
                    .build();
            default:
                throw new IllegalArgumentException("Unexpected OperationResult: " + response.getResult());
        }
    }

    @Override
    protected PersistentSubscriptionCreateResult transformResponseMessage(CreatePersistentSubscriptionCompleted response) {
        return new PersistentSubscriptionCreateResult(PersistentSubscriptionCreateStatus.Success);
    }

    @Override
    public String toString() {
        return String.format("Stream: %s, Group Name: %s", stream, groupName);
    }

}
