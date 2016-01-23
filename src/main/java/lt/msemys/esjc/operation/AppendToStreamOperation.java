package lt.msemys.esjc.operation;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import lt.msemys.esjc.EventData;
import lt.msemys.esjc.Position;
import lt.msemys.esjc.WriteResult;
import lt.msemys.esjc.proto.EventStoreClientMessages.NewEvent;
import lt.msemys.esjc.proto.EventStoreClientMessages.WriteEvents;
import lt.msemys.esjc.proto.EventStoreClientMessages.WriteEventsCompleted;
import lt.msemys.esjc.tcp.TcpCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static lt.msemys.esjc.util.UUIDConverter.toBytes;

public class AppendToStreamOperation extends AbstractOperation<WriteResult, WriteEventsCompleted> {
    private static final Logger logger = LoggerFactory.getLogger(AppendToStreamOperation.class);

    private final boolean requireMaster;
    private final String stream;
    private final int expectedVersion;
    private final Iterable<EventData> events;

    private boolean wasCommitTimeout;

    public AppendToStreamOperation(CompletableFuture<WriteResult> result,
                                   boolean requireMaster,
                                   String stream,
                                   int expectedVersion,
                                   Iterable<EventData> events,
                                   UserCredentials userCredentials) {
        super(result, TcpCommand.WriteEvents, TcpCommand.WriteEventsCompleted, userCredentials);
        this.requireMaster = requireMaster;
        this.stream = stream;
        this.expectedVersion = expectedVersion;
        this.events = events;
    }

    @Override
    protected MessageLite createRequestMessage() {
        List<NewEvent> newEvents = new ArrayList<>();
        events.forEach(e -> newEvents.add(NewEvent.newBuilder()
            .setEventId(ByteString.copyFrom(toBytes(e.eventId)))
            .setEventType(e.type)
            .setDataContentType(e.isJson ? 1 : 0)
            .setMetadataContentType(0)
            .setData(ByteString.copyFrom(e.data))
            .setMetadata(ByteString.copyFrom(e.metadata))
            .build()));

        return WriteEvents.newBuilder()
            .setEventStreamId(stream)
            .setExpectedVersion(expectedVersion)
            .setRequireMaster(requireMaster)
            .addAllEvents(newEvents)
            .build();
    }

    @Override
    protected WriteEventsCompleted createResponseMessage() {
        return WriteEventsCompleted.getDefaultInstance();
    }

    @Override
    protected InspectionResult inspectResponseMessage(WriteEventsCompleted response) {
        switch (response.getResult()) {
            case Success:
                if (wasCommitTimeout) {
                    logger.debug("IDEMPOTENT WRITE SUCCEEDED FOR {}.", this);
                }
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
            case ForwardTimeout:
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.Retry)
                    .description("ForwardTimeout")
                    .build();
            case CommitTimeout:
                wasCommitTimeout = true;
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.Retry)
                    .description("CommitTimeout")
                    .build();
            case WrongExpectedVersion:
                fail(new WrongExpectedVersionException(String.format("Append failed due to WrongExpectedVersion. Stream: %s, Expected version: %d", stream, expectedVersion)));
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
    protected WriteResult transformResponseMessage(WriteEventsCompleted response) {
        long preparePosition = response.hasPreparePosition() ? response.getPreparePosition() : -1;
        long commitPosition = response.hasCommitPosition() ? response.getCommitPosition() : -1;
        return new WriteResult(response.getLastEventNumber(), new Position(preparePosition, commitPosition));
    }

    @Override
    public String toString() {
        return String.format("Stream: %s, ExpectedVersion: %d", stream, expectedVersion);
    }
}
