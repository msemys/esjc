package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.Transaction;
import com.github.msemys.esjc.UserCredentials;
import com.github.msemys.esjc.proto.EventStoreClientMessages.TransactionStart;
import com.github.msemys.esjc.proto.EventStoreClientMessages.TransactionStartCompleted;
import com.github.msemys.esjc.tcp.TcpCommand;
import com.github.msemys.esjc.transaction.TransactionManager;
import com.google.protobuf.MessageLite;

import java.util.concurrent.CompletableFuture;

public class StartTransactionOperation extends AbstractOperation<Transaction, TransactionStartCompleted> {

    private final boolean requireMaster;
    private final String stream;
    private final int expectedVersion;
    private final TransactionManager transactionManager;

    public StartTransactionOperation(CompletableFuture<Transaction> result,
                                     boolean requireMaster,
                                     String stream,
                                     int expectedVersion,
                                     TransactionManager transactionManager,
                                     UserCredentials userCredentials) {
        super(result, TcpCommand.TransactionStart, TcpCommand.TransactionStartCompleted, userCredentials);
        this.requireMaster = requireMaster;
        this.stream = stream;
        this.expectedVersion = expectedVersion;
        this.transactionManager = transactionManager;
    }

    @Override
    protected MessageLite createRequestMessage() {
        return TransactionStart.newBuilder()
            .setEventStreamId(stream)
            .setExpectedVersion(expectedVersion)
            .setRequireMaster(requireMaster)
            .build();
    }

    @Override
    protected TransactionStartCompleted createResponseMessage() {
        return TransactionStartCompleted.getDefaultInstance();
    }

    @Override
    protected InspectionResult inspectResponseMessage(TransactionStartCompleted response) {
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
            case WrongExpectedVersion:
                fail(new WrongExpectedVersionException(String.format("Start transaction failed due to WrongExpectedVersion. Stream: %s, Expected version: %d.", stream, expectedVersion)));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("WrongExpectedVersion")
                    .build();
            case StreamDeleted:
                fail(new StreamDeletedException(stream));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("StreamDeleted")
                    .build();
            case InvalidTransaction:
                fail(new InvalidTransactionException());
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("InvalidTransaction")
                    .build();
            case AccessDenied:
                fail(new AccessDeniedException(String.format("Write access denied for stream '%s'.", stream)));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("AccessDenied")
                    .build();
            default:
                throw new IllegalArgumentException(String.format("Unexpected OperationResult: %s.", response.getResult()));
        }
    }

    @Override
    protected Transaction transformResponseMessage(TransactionStartCompleted response) {
        return new Transaction(response.getTransactionId(), userCredentials, transactionManager);
    }

    @Override
    public String toString() {
        return String.format("Stream: %s, ExpectedVersion: %d", stream, expectedVersion);
    }
}
