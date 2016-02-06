package lt.msemys.esjc.util;

import com.google.protobuf.ByteString;
import lt.msemys.esjc.ResolvedEvent;
import lt.msemys.esjc.SubscriptionDropReason;
import lt.msemys.esjc.proto.EventStoreClientMessages;

import java.util.UUID;

import static lt.msemys.esjc.util.UUIDConverter.toBytes;

public class Subscriptions {
    public static final ResolvedEvent DROP_SUBSCRIPTION_EVENT =
        new ResolvedEvent(lt.msemys.esjc.proto.EventStoreClientMessages.ResolvedEvent.newBuilder()
            .setEvent(EventStoreClientMessages.EventRecord.newBuilder()
                .setEventId(ByteString.copyFrom(toBytes(new UUID(0, 0))))
                .setEventStreamId("dummy")
                .setEventNumber(-1)
                .setEventType("dummy")
                .setDataContentType(0)
                .setMetadataContentType(0)
                .setData(ByteString.EMPTY)
                .build())
            .setCommitPosition(-1)
            .setPreparePosition(-1)
            .build());

    public static final DropData UNKNOWN_DROP_DATA = new DropData(SubscriptionDropReason.Unknown, new Exception("Drop reason not specified."));

    private Subscriptions() {
    }

    public static class DropData {
        public final SubscriptionDropReason reason;
        public final Exception exception;

        public DropData(SubscriptionDropReason reason, Exception exception) {
            this.reason = reason;
            this.exception = exception;
        }
    }

}
