package lt.msemys.esjc.operation;

import com.google.protobuf.MessageLite;
import lt.msemys.esjc.WriteResult;
import lt.msemys.esjc.proto.EventStoreClientMessages.TransactionCommit;
import lt.msemys.esjc.proto.EventStoreClientMessages.TransactionCommitCompleted;
import lt.msemys.esjc.tcp.TcpCommand;

import java.util.concurrent.CompletableFuture;

public class CommitTransactionOperation extends AbstractOperation<WriteResult, TransactionCommitCompleted> {

    private final boolean requireMaster;
    private final long transactionId;

    public CommitTransactionOperation(CompletableFuture<WriteResult> result,
                                      boolean requireMaster,
                                      long transactionId,
                                      UserCredentials userCredentials) {
        super(result, TcpCommand.TransactionCommit, TcpCommand.TransactionCommitCompleted, userCredentials);
        this.requireMaster = requireMaster;
        this.transactionId = transactionId;
    }

    @Override
    protected MessageLite createRequestMessage() {
        return TransactionCommit.newBuilder()
            .setTransactionId(transactionId)
            .setRequireMaster(requireMaster)
            .build();
    }

    @Override
    protected TransactionCommitCompleted createResponseMessage() {
        return TransactionCommitCompleted.getDefaultInstance();
    }

    @Override
    protected InspectionResult inspectResponseMessage(TransactionCommitCompleted response) {
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
                fail(new WrongExpectedVersionException(String.format("Commit transaction failed due to WrongExpectedVersion. TransactionID: %s.", transactionId)));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("WrongExpectedVersion")
                    .build();
            case StreamDeleted:
                fail(new StreamDeletedException());
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
    protected WriteResult transformResponseMessage(TransactionCommitCompleted response) {
        return null;
    }

    @Override
    public String toString() {
        return String.format("TransactionId: %s", transactionId);
    }
}
