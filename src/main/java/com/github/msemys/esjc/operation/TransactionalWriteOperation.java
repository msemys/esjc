package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventData;
import com.github.msemys.esjc.UserCredentials;
import com.github.msemys.esjc.proto.EventStoreClientMessages.NewEvent;
import com.github.msemys.esjc.proto.EventStoreClientMessages.TransactionWrite;
import com.github.msemys.esjc.proto.EventStoreClientMessages.TransactionWriteCompleted;
import com.github.msemys.esjc.tcp.TcpCommand;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.github.msemys.esjc.util.UUIDConverter.toBytes;

public class TransactionalWriteOperation extends AbstractOperation<Void, TransactionWriteCompleted> {

    private final boolean requireMaster;
    private final long transactionId;
    private final Iterable<EventData> events;

    public TransactionalWriteOperation(CompletableFuture<Void> result,
                                       boolean requireMaster,
                                       long transactionId,
                                       Iterable<EventData> events,
                                       UserCredentials userCredentials) {
        super(result, TcpCommand.TransactionWrite, TcpCommand.TransactionWriteCompleted, userCredentials);
        this.requireMaster = requireMaster;
        this.transactionId = transactionId;
        this.events = events;
    }

    @Override
    protected MessageLite createRequestMessage() {
        List<NewEvent> newEvents = new ArrayList<>();
        events.forEach(e -> newEvents.add(NewEvent.newBuilder()
            .setEventId(ByteString.copyFrom(toBytes(e.eventId)))
            .setEventType(e.type)
            .setDataContentType(e.isJsonData ? 1 : 0)
            .setMetadataContentType(e.isJsonMetadata ? 1 : 0)
            .setData(ByteString.copyFrom(e.data))
            .setMetadata(ByteString.copyFrom(e.metadata))
            .build()));

        return TransactionWrite.newBuilder()
            .setTransactionId(transactionId)
            .setRequireMaster(requireMaster)
            .addAllEvents(newEvents)
            .build();
    }

    @Override
    protected TransactionWriteCompleted createResponseMessage() {
        return TransactionWriteCompleted.getDefaultInstance();
    }

    @Override
    protected InspectionResult inspectResponseMessage(TransactionWriteCompleted response) {
        switch (response.getResult()) {
            case Success:
                succeed();
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("Success")
                    .build();
            case PrepareTimeout:
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.Retry)
                    .description("PrepareTimeout")
                    .build();
            case CommitTimeout:
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.Retry)
                    .description("CommitTimeout")
                    .build();
            case ForwardTimeout:
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.Retry)
                    .description("ForwardTimeout")
                    .build();
            case AccessDenied:
                fail(new AccessDeniedException("Write access denied."));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("AccessDenied")
                    .build();
            default:
                throw new IllegalArgumentException(String.format("Unexpected OperationResult: %s.", response.getResult()));
        }
    }

    @Override
    protected Void transformResponseMessage(TransactionWriteCompleted response) {
        return null;
    }

    @Override
    public String toString() {
        return "TransactionId: " + transactionId;
    }
}
