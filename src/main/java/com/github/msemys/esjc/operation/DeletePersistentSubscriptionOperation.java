package com.github.msemys.esjc.operation;

import com.google.protobuf.MessageLite;
import com.github.msemys.esjc.InvalidOperationException;
import com.github.msemys.esjc.PersistentSubscriptionDeleteResult;
import com.github.msemys.esjc.PersistentSubscriptionDeleteStatus;
import com.github.msemys.esjc.proto.EventStoreClientMessages.DeletePersistentSubscription;
import com.github.msemys.esjc.proto.EventStoreClientMessages.DeletePersistentSubscriptionCompleted;
import com.github.msemys.esjc.tcp.TcpCommand;

import java.util.concurrent.CompletableFuture;

public class DeletePersistentSubscriptionOperation extends AbstractOperation<PersistentSubscriptionDeleteResult, DeletePersistentSubscriptionCompleted> {
    private final String stream;
    private final String groupName;

    public DeletePersistentSubscriptionOperation(CompletableFuture<PersistentSubscriptionDeleteResult> result,
                                                 String stream,
                                                 String groupName,
                                                 UserCredentials userCredentials) {
        super(result, TcpCommand.DeletePersistentSubscription, TcpCommand.DeletePersistentSubscriptionCompleted, userCredentials);
        this.stream = stream;
        this.groupName = groupName;
    }

    @Override
    protected MessageLite createRequestMessage() {
        return DeletePersistentSubscription.newBuilder()
            .setEventStreamId(stream)
            .setSubscriptionGroupName(groupName)
            .build();
    }

    @Override
    protected DeletePersistentSubscriptionCompleted createResponseMessage() {
        return DeletePersistentSubscriptionCompleted.getDefaultInstance();
    }

    @Override
    protected InspectionResult inspectResponseMessage(DeletePersistentSubscriptionCompleted response) {
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
    protected PersistentSubscriptionDeleteResult transformResponseMessage(DeletePersistentSubscriptionCompleted response) {
        return new PersistentSubscriptionDeleteResult(PersistentSubscriptionDeleteStatus.Success);
    }

    @Override
    public String toString() {
        return String.format("Stream: %s, Group Name: %s", stream, groupName);
    }

}
