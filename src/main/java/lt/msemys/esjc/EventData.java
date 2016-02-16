package lt.msemys.esjc;

import lt.msemys.esjc.util.EmptyArrays;

import java.util.UUID;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;
import static lt.msemys.esjc.util.Strings.toBytes;

public class EventData {
    public final UUID eventId;
    public final String type;
    public final boolean isJsonData;
    public final byte[] data;
    public final boolean isJsonMetadata;
    public final byte[] metadata;

    private EventData(Builder builder) {
        eventId = builder.eventId;
        type = builder.type;
        isJsonData = builder.isJsonData;
        data = builder.data;
        isJsonMetadata = builder.isJsonMetadata;
        metadata = builder.metadata;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private UUID eventId;
        private String type;
        private boolean isJsonData;
        private byte[] data;
        private boolean isJsonMetadata;
        private byte[] metadata;

        private Builder() {
        }

        public Builder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder data(String data) {
            return data(toBytes(data));
        }

        public Builder data(byte[] data) {
            this.data = data;
            this.isJsonData = false;
            return this;
        }

        public Builder jsonData(String data) {
            return jsonData(toBytes(data));
        }

        public Builder jsonData(byte[] data) {
            this.data = data;
            this.isJsonData = true;
            return this;
        }

        public Builder metadata(String metadata) {
            return metadata(toBytes(metadata));
        }

        public Builder metadata(byte[] metadata) {
            this.metadata = metadata;
            this.isJsonMetadata = false;
            return this;
        }

        public Builder jsonMetadata(String metadata) {
            return jsonMetadata(toBytes(metadata));
        }

        public Builder jsonMetadata(byte[] metadata) {
            this.metadata = metadata;
            this.isJsonMetadata = true;
            return this;
        }

        public EventData build() {
            if (eventId == null) {
                eventId = UUID.randomUUID();
            }

            checkNotNull(type, "type");

            if (data == null) {
                data(EmptyArrays.EMPTY_BYTES);
            }

            if (metadata == null) {
                metadata(EmptyArrays.EMPTY_BYTES);
            }

            return new EventData(this);
        }
    }
}
