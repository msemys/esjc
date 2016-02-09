package lt.msemys.esjc.operation;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import lt.msemys.esjc.proto.EventStoreClientMessages.NotHandled;
import lt.msemys.esjc.proto.EventStoreClientMessages.NotHandled.MasterInfo;
import lt.msemys.esjc.tcp.TcpCommand;
import lt.msemys.esjc.tcp.TcpFlag;
import lt.msemys.esjc.tcp.TcpPackage;
import lt.msemys.esjc.util.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.netty.buffer.ByteBufUtil.prettyHexDump;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static lt.msemys.esjc.util.Preconditions.checkArgument;
import static lt.msemys.esjc.util.Strings.defaultIfEmpty;
import static lt.msemys.esjc.util.Strings.newString;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.ClientAPI/ClientOperations/OperationBase.cs">EventStore.ClientAPI/ClientOperations/OperationBase.cs</a>
 */
public abstract class AbstractOperation<T, R extends MessageLite> implements Operation {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final TcpCommand requestCommand;
    private final TcpCommand responseCommand;
    protected final UserCredentials userCredentials;

    private final CompletableFuture<T> result;
    private R responseMessage;
    private final AtomicBoolean completed = new AtomicBoolean(false);

    protected AbstractOperation(CompletableFuture<T> result,
                                TcpCommand requestCommand,
                                TcpCommand responseCommand,
                                UserCredentials userCredentials) {
        this.result = result;
        this.requestCommand = requestCommand;
        this.responseCommand = responseCommand;
        this.userCredentials = userCredentials;
    }

    protected abstract MessageLite createRequestMessage();

    protected abstract R createResponseMessage();

    protected abstract InspectionResult inspectResponseMessage(R response);

    protected abstract T transformResponseMessage(R response);

    @Override
    public TcpPackage create(UUID correlationId) {
        return TcpPackage.newBuilder()
                .command(requestCommand)
                .flag(userCredentials != null ? TcpFlag.Authenticated : TcpFlag.None)
                .correlationId(correlationId)
                .login(userCredentials != null ? userCredentials.username : null)
                .password(userCredentials != null ? userCredentials.password : null)
                .data(createRequestMessage().toByteArray())
                .build();
    }

    @Override
    public InspectionResult inspect(TcpPackage tcpPackage) {
        try {
            if (tcpPackage.command == responseCommand) {
                responseMessage = (R) createResponseMessage().getParserForType().parseFrom(tcpPackage.data);
                return inspectResponseMessage(responseMessage);
            } else {
                switch (tcpPackage.command) {
                    case NotAuthenticated:
                        return inspectNotAuthenticated(tcpPackage);
                    case BadRequest:
                        return inspectBadRequest(tcpPackage);
                    case NotHandled:
                        return inspectNotHandled(tcpPackage);
                    default:
                        return inspectUnexpectedCommand(tcpPackage, responseCommand);
                }
            }
        } catch (Exception e) {
            fail(e);
            return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("Exception - " + e.getMessage())
                    .build();
        }
    }

    private InspectionResult inspectNotAuthenticated(TcpPackage tcpPackage) {
        String message = newString(tcpPackage.data);
        fail(new NotAuthenticatedException(defaultIfEmpty(message, "Authentication error")));
        return InspectionResult.newBuilder()
                .decision(InspectionDecision.EndOperation)
                .description("NotAuthenticated")
                .build();
    }

    private InspectionResult inspectBadRequest(TcpPackage tcpPackage) {
        String message = newString(tcpPackage.data);
        fail(new ServerErrorException(defaultIfEmpty(message, "<no message>")));
        return InspectionResult.newBuilder()
                .decision(InspectionDecision.EndOperation)
                .description("BadRequest - " + message)
                .build();
    }

    private InspectionResult inspectNotHandled(TcpPackage tcpPackage) {
        try {
            NotHandled message = NotHandled.parseFrom(tcpPackage.data);

            switch (message.getReason()) {
                case NotReady:
                    return InspectionResult.newBuilder()
                            .decision(InspectionDecision.Retry)
                            .description("NotHandled - NotReady")
                            .build();
                case TooBusy:
                    return InspectionResult.newBuilder()
                            .decision(InspectionDecision.Retry)
                            .description("NotHandled - TooBusy")
                            .build();
                case NotMaster:
                    MasterInfo masterInfo = MasterInfo.parseFrom(message.getAdditionalInfo().toByteArray());
                    return InspectionResult.newBuilder()
                            .decision(InspectionDecision.Reconnect)
                            .description("NotHandled - NotMaster")
                            .address(masterInfo.getExternalTcpAddress(), masterInfo.getExternalTcpPort())
                            .secureAddress(masterInfo.getExternalSecureTcpAddress(), masterInfo.getExternalSecureTcpPort())
                            .build();
                default:
                    logger.error("Unknown NotHandledReason: {}.", message.getReason());
                    return InspectionResult.newBuilder()
                            .decision(InspectionDecision.Retry)
                            .description("NotHandled - <unknown>")
                            .build();
            }
        } catch (InvalidProtocolBufferException e) {
            throw Throwables.propagate(e);
        }
    }

    private InspectionResult inspectUnexpectedCommand(TcpPackage tcpPackage, TcpCommand expectedCommand) {
        checkArgument(tcpPackage.command != expectedCommand, "Command should not be %s.", tcpPackage.command);

        logger.error("Unexpected TcpCommand received.\n" +
                        "Expected: {}, Actual: {}, Flags: {}, CorrelationId: {}\n" +
                        "Operation ({}): {}\n" +
                        "TcpPackage Data Dump:\n{}",
                expectedCommand, tcpPackage.command, tcpPackage.flag, tcpPackage.correlationId,
                getClass().getSimpleName(), this, prettyHexDump(wrappedBuffer(tcpPackage.data)));

        fail(new CommandNotExpectedException(expectedCommand, tcpPackage.command));

        return InspectionResult.newBuilder()
                .decision(InspectionDecision.EndOperation)
                .description("Unexpected command - " + tcpPackage.command)
                .build();
    }

    protected void succeed() {
        if (completed.compareAndSet(false, true)) {
            if (responseMessage != null) {
                result.complete(transformResponseMessage(responseMessage));
            } else {
                result.completeExceptionally(new NoResultException());
            }
        }
    }

    @Override
    public void fail(Exception exception) {
        if (completed.compareAndSet(false, true)) {
            result.completeExceptionally(exception);
        }
    }

}
