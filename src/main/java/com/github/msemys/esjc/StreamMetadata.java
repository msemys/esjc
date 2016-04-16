package com.github.msemys.esjc;

import com.github.msemys.esjc.util.Throwables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.github.msemys.esjc.util.Numbers.isNegative;
import static com.github.msemys.esjc.util.Numbers.isPositive;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static com.github.msemys.esjc.util.Strings.toBytes;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * Represents stream metadata with strongly typed properties for system values and
 * a text based properties for custom values.
 */
public class StreamMetadata {
    private static final StreamMetadata EMPTY = newBuilder().build();

    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(StreamMetadata.class, new StreamMetadataJsonAdapter())
        .create();

    /**
     * The maximum number of events allowed in the stream.
     */
    public final Integer maxCount;

    /**
     * The maximum age of events allowed in the stream.
     */
    public final Duration maxAge;

    /**
     * The event number from which previous events can be scavenged.
     * This is used to implement soft-deletion of streams.
     */
    public final Integer truncateBefore;

    /**
     * The amount of time for which the stream head is cachable.
     */
    public final Duration cacheControl;

    /**
     * The access control list for the stream.
     */
    public final StreamAcl acl;

    /**
     * Custom properties.
     */
    public final List<Property> customProperties;

    private StreamMetadata(Builder builder) {
        maxCount = builder.maxCount;
        maxAge = builder.maxAge;
        truncateBefore = builder.truncateBefore;
        cacheControl = builder.cacheControl;
        acl = builder.acl;
        customProperties = (builder.customProperties != null) ? unmodifiableList(builder.customProperties) : emptyList();
    }

    /**
     * Converts to JSON representation.
     *
     * @return JSON representation
     */
    public String toJson() {
        return gson.toJson(this);
    }

    /**
     * Creates a new stream metadata from the specified JSON.
     *
     * @param json stream metadata.
     * @return stream metadata
     */
    public static StreamMetadata fromJson(String json) {
        checkArgument(!isNullOrEmpty(json), "json");
        return fromJson(toBytes(json));
    }

    /**
     * Creates a new stream metadata from the specified JSON.
     *
     * @param bytes stream metadata.
     * @return stream metadata
     */
    public static StreamMetadata fromJson(byte[] bytes) {
        checkNotNull(bytes, "bytes");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)))) {
            return gson.fromJson(new JsonReader(reader), StreamMetadata.class);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Empty stream metadata.
     *
     * @return stream metadata.
     */
    public static StreamMetadata empty() {
        return EMPTY;
    }

    /**
     * Creates a new stream metadata builder.
     *
     * @return stream metadata builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Stream metadata builder.
     */
    public static class Builder {
        private Integer maxCount;
        private Duration maxAge;
        private Integer truncateBefore;
        private Duration cacheControl;
        private StreamAcl acl;
        private List<String> aclReadRoles;
        private List<String> aclWriteRoles;
        private List<String> aclDeleteRoles;
        private List<String> aclMetaReadRoles;
        private List<String> aclMetaWriteRoles;
        private List<Property> customProperties = new ArrayList<>();

        private Builder() {
        }

        /**
         * Sets the maximum number of events allowed in the stream.
         *
         * @param maxCount the maximum number of events allowed in the stream.
         * @return the builder reference
         */
        public Builder maxCount(int maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        /**
         * Sets the maximum age of events allowed in the stream.
         *
         * @param maxAge the maximum age of events allowed in the stream.
         * @return the builder reference
         */
        public Builder maxAge(Duration maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        /**
         * Sets the event number from which previous events can be scavenged.
         *
         * @param truncateBefore the event number from which previous events can be scavenged.
         * @return the builder reference
         */
        public Builder truncateBefore(Integer truncateBefore) {
            this.truncateBefore = truncateBefore;
            return this;
        }

        /**
         * Sets the amount of time for which the stream head is cachable.
         *
         * @param cacheControl the amount of time for which the stream head is cachable.
         * @return the builder reference
         */
        public Builder cacheControl(Duration cacheControl) {
            this.cacheControl = cacheControl;
            return this;
        }

        /**
         * Sets role names with read permission for the stream.
         *
         * @param aclReadRoles role names.
         * @return the builder reference
         */
        public Builder aclReadRoles(List<String> aclReadRoles) {
            this.aclReadRoles = aclReadRoles;
            return this;
        }

        /**
         * Sets role names with write permission for the stream.
         *
         * @param aclWriteRoles role names.
         * @return the builder reference
         */
        public Builder aclWriteRoles(List<String> aclWriteRoles) {
            this.aclWriteRoles = aclWriteRoles;
            return this;
        }

        /**
         * Sets role names with delete permission for the stream.
         *
         * @param aclDeleteRoles role names.
         * @return the builder reference
         */
        public Builder aclDeleteRoles(List<String> aclDeleteRoles) {
            this.aclDeleteRoles = aclDeleteRoles;
            return this;
        }

        /**
         * Sets role names with metadata read permission for the stream.
         *
         * @param aclMetaReadRoles role names.
         * @return the builder reference
         */
        public Builder aclMetaReadRoles(List<String> aclMetaReadRoles) {
            this.aclMetaReadRoles = aclMetaReadRoles;
            return this;
        }

        /**
         * Sets role names with metadata write permission for the stream.
         *
         * @param aclMetaWriteRoles role names.
         * @return the builder reference
         */
        public Builder aclMetaWriteRoles(List<String> aclMetaWriteRoles) {
            this.aclMetaWriteRoles = aclMetaWriteRoles;
            return this;
        }

        /**
         * Sets a custom metadata property.
         *
         * @param name  property name.
         * @param value property value.
         * @return the builder reference
         */
        public Builder customProperty(String name, String value) {
            customProperties.add(Property.of(name, value));
            return this;
        }

        /**
         * Builds a stream metadata.
         *
         * @return stream metadata
         */
        public StreamMetadata build() {
            if (maxCount != null) {
                checkArgument(isPositive(maxCount), "maxCount should be positive");
            }

            if (maxAge != null) {
                checkArgument(!Duration.ZERO.equals(maxAge), "maxAge cannot be zero");
            }

            if (truncateBefore != null) {
                checkArgument(!isNegative(truncateBefore), "truncateBefore should not be negative");
            }

            if (cacheControl != null) {
                checkArgument(!Duration.ZERO.equals(cacheControl), "cacheControl cannot be zero");
            }

            acl = (aclReadRoles == null &&
                aclWriteRoles == null &&
                aclDeleteRoles == null &&
                aclMetaReadRoles == null &&
                aclMetaWriteRoles == null) ?
                null : new StreamAcl(aclReadRoles, aclWriteRoles, aclDeleteRoles, aclMetaReadRoles, aclMetaWriteRoles);

            return new StreamMetadata(this);
        }
    }

    /**
     * User-defined property.
     */
    public static class Property {
        public final String name;
        public final String value;

        private Property(String name, String value) {
            checkArgument(!isNullOrEmpty(name), "name");
            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Property property = (Property) o;

            if (name != null ? !name.equals(property.name) : property.name != null) return false;
            return value != null ? value.equals(property.value) : property.value == null;

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        /**
         * Creates a new user-defined property.
         *
         * @param name  property name.
         * @param value property value.
         * @return user-defined property
         */
        public static Property of(String name, String value) {
            return new Property(name, value);
        }
    }

}
