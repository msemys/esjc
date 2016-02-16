package com.github.msemys.esjc.util;

import com.google.protobuf.ByteString;
import com.github.msemys.esjc.ResolvedEvent;
import com.github.msemys.esjc.StreamPosition;
import com.github.msemys.esjc.SubscriptionDropReason;
import com.github.msemys.esjc.proto.EventStoreClientMessages;

import java.util.UUID;

import static com.github.msemys.esjc.util.UUIDConverter.toBytes;

public class Subscriptions {
    public static final ResolvedEvent DROP_SUBSCRIPTION_EVENT =
        new ResolvedEvent(com.github.msemys.esjc.proto.EventStoreClientMessages.ResolvedEvent.newBuilder()
            .setEvent(EventStoreClientMessages.EventRecord.newBuilder()
                .setEventId(ByteString.copyFrom(toBytes(new UUID(0, 0))))
                .setEventStreamId("dummy")
                .setEventNumber(StreamPosition.END)
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
