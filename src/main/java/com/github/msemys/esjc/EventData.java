package com.github.msemys.esjc;

import com.github.msemys.esjc.system.SystemEventType;
import com.github.msemys.esjc.util.EmptyArrays;

import java.util.UUID;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static com.github.msemys.esjc.util.Strings.toBytes;

/**
 * Represents an event to be written.
 */
public class EventData {

    /**
     * The ID of the event (used as part of the idempotent write check).
     */
    public final UUID eventId;

    /**
     * The name of the event type.
     */
    public final String type;

    /**
     * Flag indicating whether the data is JSON.
     */
    public final boolean isJsonData;

    /**
     * The raw bytes of the event data.
     */
    public final byte[] data;

    /**
     * Flag indicating whether the metadata is JSON.
     */
    public final boolean isJsonMetadata;

    /**
     * The raw bytes of the event metadata.
     */
    public final byte[] metadata;

    private EventData(Builder builder) {
        eventId = builder.eventId;
        type = builder.type;
        isJsonData = builder.isJsonData;
        data = builder.data;
        isJsonMetadata = builder.isJsonMetadata;
        metadata = builder.metadata;
    }

    /**
     * Creates a new event data builder.
     *
     * @return event data builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Event data builder.
     */
    public static class Builder {
        private UUID eventId;
        private String type;
        private boolean isJsonData;
        private byte[] data;
        private boolean isJsonMetadata;
        private byte[] metadata;

        private Builder() {
        }

        /**
         * Sets event id (used as part of the idempotent write check).
         *
         * @param eventId event id.
         * @return the builder reference
         */
        public Builder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }

        /**
         * Sets name of the event type.
         * <p>
         * It is strongly recommended that these use lowerCamelCase if projections are to be used.
         * </p>
         *
         * @param type the name of the event type.
         * @return the builder reference
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets event data.
         *
         * @param data event data.
         * @return the builder reference
         */
        public Builder data(String data) {
            return data(toBytes(data));
        }

        /**
         * Sets event data.
         *
         * @param data event data.
         * @return the builder reference
         */
        public Builder data(byte[] data) {
            this.data = data;
            this.isJsonData = false;
            return this;
        }

        /**
         * Sets event data and marks it as JSON.
         *
         * @param data json event data.
         * @return the builder reference
         */
        public Builder jsonData(String data) {
            return jsonData(toBytes(data));
        }

        /**
         * Sets event data and marks it as JSON.
         *
         * @param data json event data.
         * @return the builder reference
         */
        public Builder jsonData(byte[] data) {
            this.data = data;
            this.isJsonData = true;
            return this;
        }

        /**
         * Sets event metadata.
         *
         * @param metadata event metadata.
         * @return the builder reference
         */
        public Builder metadata(String metadata) {
            return metadata(toBytes(metadata));
        }

        /**
         * Sets event metadata.
         *
         * @param metadata event metadata.
         * @return the builder reference
         */
        public Builder metadata(byte[] metadata) {
            this.metadata = metadata;
            this.isJsonMetadata = false;
            return this;
        }

        /**
         * Sets event metadata and marks it as JSON.
         *
         * @param metadata json event metadata.
         * @return the builder reference
         */
        public Builder jsonMetadata(String metadata) {
            return jsonMetadata(toBytes(metadata));
        }

        /**
         * Sets event metadata and marks it as JSON.
         *
         * @param metadata json event metadata.
         * @return the builder reference
         */
        public Builder jsonMetadata(byte[] metadata) {
            this.metadata = metadata;
            this.isJsonMetadata = true;
            return this;
        }

        /**
         * Sets event data as link to the specified event in target stream.
         *
         * @param eventNumber event number in target stream.
         * @param stream      target stream name.
         * @return the builder reference
         */
        public Builder linkTo(int eventNumber, String stream) {
            checkArgument(!isNullOrEmpty(stream), "stream");
            return type(SystemEventType.LINK_TO.value).data(eventNumber + "@" + stream);
        }

        /**
         * Builds an event data.
         * <p>If event id is not specified, the random id will be generated.</p>
         *
         * @return an event data
         */
        public EventData build() {
            if (eventId == null) {
                eventId = UUID.randomUUID();
            }

            checkNotNull(type, "type");

            if (data == null) {
                data = EmptyArrays.EMPTY_BYTES;
            }

            if (metadata == null) {
                metadata = EmptyArrays.EMPTY_BYTES;
            }

            return new EventData(this);
        }
    }
}
