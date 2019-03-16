package com.github.msemys.esjc.util;

import com.github.msemys.esjc.ResolvedEvent;
import com.github.msemys.esjc.RetryableResolvedEvent;
import com.github.msemys.esjc.StreamPosition;
import com.github.msemys.esjc.SubscriptionDropReason;
import com.github.msemys.esjc.proto.EventStoreClientMessages.EventRecord;
import com.google.protobuf.ByteString;

import java.util.UUID;

import static com.github.msemys.esjc.util.UUIDConverter.toBytes;

public class Subscriptions {
    private static final EventRecord DUMMY_EVENT_RECORD = EventRecord.newBuilder()
        .setEventId(ByteString.copyFrom(toBytes(new UUID(0, 0))))
        .setEventStreamId("dummy")
        .setEventNumber(StreamPosition.END)
        .setEventType("dummy")
        .setDataContentType(0)
        .setMetadataContentType(0)
        .setData(ByteString.EMPTY)
        .build();

    public static final ResolvedEvent DROP_SUBSCRIPTION_EVENT =
        new ResolvedEvent(com.github.msemys.esjc.proto.EventStoreClientMessages.ResolvedEvent.newBuilder()
            .setEvent(DUMMY_EVENT_RECORD)
            .setCommitPosition(-1)
            .setPreparePosition(-1)
            .build());

    public static final RetryableResolvedEvent DROP_PERSISTENT_SUBSCRIPTION_EVENT = new RetryableResolvedEvent(com.github.msemys.esjc.proto.EventStoreClientMessages.ResolvedIndexedEvent.newBuilder()
        .setEvent(DUMMY_EVENT_RECORD)
        .build(),
        -1);

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
