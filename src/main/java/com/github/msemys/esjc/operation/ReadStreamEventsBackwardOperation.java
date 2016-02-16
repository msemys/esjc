package com.github.msemys.esjc.operation;

import com.google.protobuf.MessageLite;
import com.github.msemys.esjc.ReadDirection;
import com.github.msemys.esjc.SliceReadStatus;
import com.github.msemys.esjc.StreamEventsSlice;
import com.github.msemys.esjc.proto.EventStoreClientMessages.ReadStreamEvents;
import com.github.msemys.esjc.proto.EventStoreClientMessages.ReadStreamEventsCompleted;
import com.github.msemys.esjc.tcp.TcpCommand;

import java.util.concurrent.CompletableFuture;

import static com.github.msemys.esjc.util.Strings.defaultIfEmpty;

public class ReadStreamEventsBackwardOperation extends AbstractOperation<StreamEventsSlice, ReadStreamEventsCompleted> {

    private final String stream;
    private final int fromEventNumber;
    private final int maxCount;
    private final boolean resolveLinkTos;
    private final boolean requireMaster;

    public ReadStreamEventsBackwardOperation(CompletableFuture<StreamEventsSlice> result,
                                             String stream,
                                             int fromEventNumber,
                                             int maxCount,
                                             boolean resolveLinkTos,
                                             boolean requireMaster,
                                             UserCredentials userCredentials) {
        super(result, TcpCommand.ReadStreamEventsBackward, TcpCommand.ReadStreamEventsBackwardCompleted, userCredentials);
        this.stream = stream;
        this.fromEventNumber = fromEventNumber;
        this.maxCount = maxCount;
        this.resolveLinkTos = resolveLinkTos;
        this.requireMaster = requireMaster;
    }

    @Override
    protected MessageLite createRequestMessage() {
        return ReadStreamEvents.newBuilder()
            .setEventStreamId(stream)
            .setFromEventNumber(fromEventNumber)
            .setMaxCount(maxCount)
            .setResolveLinkTos(resolveLinkTos)
            .setRequireMaster(requireMaster)
            .build();
    }

    @Override
    protected ReadStreamEventsCompleted createResponseMessage() {
        return ReadStreamEventsCompleted.getDefaultInstance();
    }

    @Override
    protected InspectionResult inspectResponseMessage(ReadStreamEventsCompleted response) {
        switch (response.getResult()) {
            case Success:
                succeed();
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("Success")
                    .build();
            case StreamDeleted:
                succeed();
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("StreamDeleted")
                    .build();
            case NoStream:
                succeed();
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("NoStream")
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
                throw new IllegalArgumentException(String.format("Unexpected ReadStreamResult: %s.", response.getResult()));
        }
    }

    @Override
    protected StreamEventsSlice transformResponseMessage(ReadStreamEventsCompleted response) {
        return new StreamEventsSlice(asSliceReadStatus(response.getResult()),
            stream,
            fromEventNumber,
            ReadDirection.Backward,
            response.getEventsList(),
            response.getNextEventNumber(),
            response.getLastEventNumber(),
            response.getIsEndOfStream());
    }

    @Override
    public String toString() {
        return String.format("Stream: %s, FromEventNumber: %s, MaxCount: %d, ResolveLinkTos: %s, RequireMaster: %s",
            stream, fromEventNumber, maxCount, resolveLinkTos, requireMaster);
    }

    private static SliceReadStatus asSliceReadStatus(ReadStreamEventsCompleted.ReadStreamResult result) {
        switch (result) {
            case Success:
                return SliceReadStatus.Success;
            case NoStream:
                return SliceReadStatus.StreamNotFound;
            case StreamDeleted:
                return SliceReadStatus.StreamDeleted;
            default:
                throw new IllegalArgumentException(String.format("Unexpected ReadStreamResult: %s.", result));
        }
    }
}
