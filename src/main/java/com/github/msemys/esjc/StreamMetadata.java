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
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.github.msemys.esjc.util.Numbers.isNegative;
import static com.github.msemys.esjc.util.Numbers.isPositive;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static com.github.msemys.esjc.util.Strings.toBytes;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * Represents stream metadata with strongly typed properties for system values and custom values.
 */
public class StreamMetadata {
    private static final StreamMetadata EMPTY = newBuilder().build();

    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(StreamMetadata.class, new StreamMetadataJsonAdapter())
        .serializeNulls()
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
        acl = (builder.aclBuilder != null) ? builder.aclBuilder.build() : null;
        customProperties = (builder.customProperties != null) ? unmodifiableList(builder.customProperties) : emptyList();
    }

    /**
     * Finds custom property.
     *
     * @param name property name.
     * @return custom property
     */
    public Optional<Property> findCustomProperty(String name) {
        return customProperties.stream().filter(p -> p.name.equals(name)).findFirst();
    }

    /**
     * Gets custom property.
     *
     * @param name property name.
     * @return custom property
     * @throws NoSuchElementException if custom property not found
     */
    public Property getCustomProperty(String name) throws NoSuchElementException {
        return findCustomProperty(name).get();
    }

    /**
     * Creates a new stream metadata builder populated with the values from this instance.
     *
     * @return stream metadata builder
     */
    public Builder toBuilder() {
        Builder builder = new Builder();

        if (maxCount != null) {
            builder.maxCount(maxCount);
        }

        if (maxAge != null) {
            builder.maxAge(maxAge);
        }

        if (truncateBefore != null) {
            builder.truncateBefore(truncateBefore);
        }

        if (cacheControl != null) {
            builder.cacheControl(cacheControl);
        }

        if (acl != null) {
            builder.aclBuilder = acl.toBuilder();
        }

        if (customProperties != null) {
            builder.customProperties(new ArrayList<>(customProperties));
        }

        return builder;
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
        private StreamAcl.Builder aclBuilder;
        private List<Property> customProperties = new ArrayList<>();

        private Builder() {
        }

        /**
         * Sets the maximum number of events allowed in the stream.
         *
         * @param maxCount the maximum number of events allowed in the stream.
         * @return the builder reference
         */
        public Builder maxCount(Integer maxCount) {
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
         * Sets an access-control-list for the stream.
         *
         * @param acl stream ACL.
         * @return the builder reference
         */
        public Builder acl(StreamAcl acl) {
            aclBuilder = (acl != null) ? acl.toBuilder() : null;
            return this;
        }

        /**
         * Sets role names with read permission for the stream.
         *
         * @param aclReadRoles role names.
         * @return the builder reference
         * @see #acl(StreamAcl)
         */
        public Builder aclReadRoles(List<String> aclReadRoles) {
            aclBuilder().readRoles(aclReadRoles);
            return this;
        }

        /**
         * Sets role names with write permission for the stream.
         *
         * @param aclWriteRoles role names.
         * @return the builder reference
         * @see #acl(StreamAcl)
         */
        public Builder aclWriteRoles(List<String> aclWriteRoles) {
            aclBuilder().writeRoles(aclWriteRoles);
            return this;
        }

        /**
         * Sets role names with delete permission for the stream.
         *
         * @param aclDeleteRoles role names.
         * @return the builder reference
         * @see #acl(StreamAcl)
         */
        public Builder aclDeleteRoles(List<String> aclDeleteRoles) {
            aclBuilder().deleteRoles(aclDeleteRoles);
            return this;
        }

        /**
         * Sets role names with metadata read permission for the stream.
         *
         * @param aclMetaReadRoles role names.
         * @return the builder reference
         * @see #acl(StreamAcl)
         */
        public Builder aclMetaReadRoles(List<String> aclMetaReadRoles) {
            aclBuilder().metaReadRoles(aclMetaReadRoles);
            return this;
        }

        /**
         * Sets role names with metadata write permission for the stream.
         *
         * @param aclMetaWriteRoles role names.
         * @return the builder reference
         * @see #acl(StreamAcl)
         */
        public Builder aclMetaWriteRoles(List<String> aclMetaWriteRoles) {
            aclBuilder().metaWriteRoles(aclMetaWriteRoles);
            return this;
        }

        private StreamAcl.Builder aclBuilder() {
            if (aclBuilder == null) {
                aclBuilder = StreamAcl.newBuilder();
            }
            return aclBuilder;
        }

        /**
         * Sets metadata custom property list.
         *
         * @param properties custom properties.
         * @return the builder reference
         */
        public Builder customProperties(List<Property> properties) {
            customProperties = properties;
            return this;
        }

        /**
         * Sets metadata custom property.
         *
         * @param property custom property.
         * @return the builder reference
         */
        public Builder customProperty(Property property) {
            int index = customProperties.stream()
                .filter(p -> p.name.equals(property.name))
                .findFirst()
                .map(p -> customProperties.indexOf(p)).orElse(-1);

            if (index > -1) {
                customProperties.set(index, property);
            } else {
                customProperties.add(property);
            }

            return this;
        }

        /**
         * Sets text based custom metadata property.
         *
         * @param name  property name.
         * @param value property value.
         * @return the builder reference
         */
        public Builder customProperty(String name, String value) {
            return customProperty(new Property(name, value));
        }

        /**
         * Sets number type custom metadata property.
         *
         * @param name  property name.
         * @param value property value.
         * @return the builder reference
         */
        public Builder customProperty(String name, Number value) {
            return customProperty(new Property(name, value));
        }

        /**
         * Sets boolean type custom metadata property.
         *
         * @param name  property name.
         * @param value property value.
         * @return the builder reference
         */
        public Builder customProperty(String name, Boolean value) {
            return customProperty(new Property(name, value));
        }

        /**
         * Sets text array type custom metadata property.
         *
         * @param name   property name.
         * @param values property values.
         * @return the builder reference
         */
        public Builder customProperty(String name, String... values) {
            return customProperty(new Property(name, values));
        }

        /**
         * Sets number array type custom metadata property.
         *
         * @param name   property name.
         * @param values property values.
         * @return the builder reference
         */
        public Builder customProperty(String name, Number... values) {
            return customProperty(new Property(name, values));
        }

        /**
         * Sets boolean array type custom metadata property.
         *
         * @param name   property name.
         * @param values property values.
         * @return the builder reference
         */
        public Builder customProperty(String name, Boolean... values) {
            return customProperty(new Property(name, values));
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

            return new StreamMetadata(this);
        }
    }

    /**
     * User-defined property.
     */
    public static class Property {
        public final String name;
        public final Object value;

        private Property(String name, Object value) {
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

        @Override
        public String toString() {
            if (value instanceof String) {
                return (String) value;
            } else {
                return (value != null) ? value.toString() : null;
            }
        }

        public Integer toInteger() {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else {
                return (value != null) ? Integer.parseInt(toString()) : null;
            }
        }

        public Long toLong() {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else {
                return (value != null) ? Long.parseLong(toString()) : null;
            }
        }

        public Double toDouble() {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else {
                return (value != null) ? Double.parseDouble(toString()) : null;
            }
        }

        public Boolean toBoolean() {
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else {
                return (value != null) ? Boolean.parseBoolean(toString()) : null;
            }
        }

        public String[] toStrings() {
            if (value instanceof String[]) {
                return (String[]) value;
            } else {
                return (value != null) ? new String[]{toString()} : null;
            }
        }

        public Integer[] toIntegers() {
            if (value instanceof Number[]) {
                return stream(((Number[]) value)).map(v -> (v != null) ? v.intValue() : null).toArray(Integer[]::new);
            } else if (value instanceof String[]) {
                return new Integer[((String[]) value).length];
            } else {
                return (value != null) ? new Integer[]{Integer.parseInt(toString())} : null;
            }
        }

        public Long[] toLongs() {
            if (value instanceof Number[]) {
                return stream(((Number[]) value)).map(v -> (v != null) ? v.longValue() : null).toArray(Long[]::new);
            } else if (value instanceof String[]) {
                return new Long[((String[]) value).length];
            } else {
                return (value != null) ? new Long[]{Long.parseLong(toString())} : null;
            }
        }

        public Double[] toDoubles() {
            if (value instanceof Number[]) {
                return stream(((Number[]) value)).map(v -> (v != null) ? v.doubleValue() : null).toArray(Double[]::new);
            } else if (value instanceof String[]) {
                return new Double[((String[]) value).length];
            } else {
                return (value != null) ? new Double[]{Double.parseDouble(toString())} : null;
            }
        }

        public Boolean[] toBooleans() {
            if (value instanceof Boolean[]) {
                return (Boolean[]) value;
            } else if (value instanceof String[]) {
                return new Boolean[((String[]) value).length];
            } else {
                return (value != null) ? new Boolean[]{Boolean.parseBoolean(toString())} : null;
            }
        }
    }

}
