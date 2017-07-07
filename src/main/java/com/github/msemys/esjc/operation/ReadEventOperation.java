package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventReadResult;
import com.github.msemys.esjc.EventReadStatus;
import com.github.msemys.esjc.UserCredentials;
import com.github.msemys.esjc.proto.EventStoreClientMessages.ReadEvent;
import com.github.msemys.esjc.proto.EventStoreClientMessages.ReadEventCompleted;
import com.github.msemys.esjc.proto.EventStoreClientMessages.ReadEventCompleted.ReadEventResult;
import com.github.msemys.esjc.tcp.TcpCommand;
import com.google.protobuf.MessageLite;

import java.util.concurrent.CompletableFuture;

import static com.github.msemys.esjc.util.Strings.defaultIfEmpty;

public class ReadEventOperation extends AbstractOperation<EventReadResult, ReadEventCompleted> {

    private final String stream;
    private final long eventNumber;
    private final boolean resolveLinkTo;
    private final boolean requireMaster;

    public ReadEventOperation(CompletableFuture<EventReadResult> result,
                              String stream,
                              long eventNumber,
                              boolean resolveLinkTo,
                              boolean requireMaster,
                              UserCredentials userCredentials) {
        super(result, TcpCommand.ReadEvent, TcpCommand.ReadEventCompleted, userCredentials);
        this.stream = stream;
        this.eventNumber = eventNumber;
        this.resolveLinkTo = resolveLinkTo;
        this.requireMaster = requireMaster;
    }

    @Override
    protected MessageLite createRequestMessage() {
        return ReadEvent.newBuilder()
            .setEventStreamId(stream)
            .setEventNumber(eventNumber)
            .setResolveLinkTos(resolveLinkTo)
            .setRequireMaster(requireMaster)
            .build();
    }

    @Override
    protected ReadEventCompleted createResponseMessage() {
        return ReadEventCompleted.getDefaultInstance();
    }

    @Override
    protected InspectionResult inspectResponseMessage(ReadEventCompleted response) {
        switch (response.getResult()) {
            case Success:
                succeed();
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("Success")
                    .build();
            case NotFound:
                succeed();
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("NotFound")
                    .build();
            case NoStream:
                succeed();
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("NoStream")
                    .build();
            case StreamDeleted:
                succeed();
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("StreamDeleted")
                    .build();
            case Error:
                fail(new ServerErrorException(defaultIfEmpty(response.getError(), "<no message>")));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("Error")
                    .build();
            case AccessDenied:
                fail(new AccessDeniedException(String.format("Read access denied for stream '%s'.", stream)));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("AccessDenied")
                    .build();
            default:
                throw new IllegalArgumentException(String.format("Unexpected OperationResult: %s.", response.getResult()));
        }
    }

    @Override
    protected EventReadResult transformResponseMessage(ReadEventCompleted response) {
        return new EventReadResult(asEventReadStatus(response.getResult()), stream, eventNumber, response.getEvent());
    }

    @Override
    public String toString() {
        return String.format("Stream: %s, EventNumber: %d, ResolveLinkTo: %s, RequireMaster: %s",
            stream, eventNumber, resolveLinkTo, requireMaster);
    }

    private static EventReadStatus asEventReadStatus(ReadEventResult result) {
        switch (result) {
            case Success:
                return EventReadStatus.Success;
            case NotFound:
                return EventReadStatus.NotFound;
            case NoStream:
                return EventReadStatus.NoStream;
            case StreamDeleted:
                return EventReadStatus.StreamDeleted;
            default:
                throw new IllegalArgumentException(String.format("Unexpected ReadEventResult: %s.", result));
        }
    }
}
