package com.github.msemys.esjc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.github.msemys.esjc.util.Throwables;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static com.github.msemys.esjc.util.Numbers.isPositive;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static com.github.msemys.esjc.util.Strings.toBytes;

public class StreamMetadata {
    private static final StreamMetadata EMPTY = newBuilder().build();

    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(StreamMetadata.class, new StreamMetadataJsonAdapter())
        .create();

    public final Integer maxCount;
    public final Duration maxAge;
    public final Integer truncateBefore;
    public final Duration cacheControl;
    public final StreamAcl acl;
    public final List<Property> customProperties;

    private StreamMetadata(Builder builder) {
        maxCount = builder.maxCount;
        maxAge = builder.maxAge;
        truncateBefore = builder.truncateBefore;
        cacheControl = builder.cacheControl;
        acl = builder.acl;
        customProperties = (builder.customProperties != null) ? unmodifiableList(builder.customProperties) : emptyList();
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static StreamMetadata fromJson(String json) {
        checkArgument(!isNullOrEmpty(json), "json");
        return fromJson(toBytes(json));
    }

    public static StreamMetadata fromJson(byte[] bytes) {
        checkNotNull(bytes, "bytes");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)))) {
            return gson.fromJson(new JsonReader(reader), StreamMetadata.class);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public static StreamMetadata empty() {
        return EMPTY;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

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

        public Builder maxCount(int maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        public Builder maxAge(Duration maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public Builder truncateBefore(Integer truncateBefore) {
            this.truncateBefore = truncateBefore;
            return this;
        }

        public Builder cacheControl(Duration cacheControl) {
            this.cacheControl = cacheControl;
            return this;
        }

        public Builder aclReadRoles(List<String> aclReadRoles) {
            this.aclReadRoles = aclReadRoles;
            return this;
        }

        public Builder aclWriteRoles(List<String> aclWriteRoles) {
            this.aclWriteRoles = aclWriteRoles;
            return this;
        }

        public Builder aclDeleteRoles(List<String> aclDeleteRoles) {
            this.aclDeleteRoles = aclDeleteRoles;
            return this;
        }

        public Builder aclMetaReadRoles(List<String> aclMetaReadRoles) {
            this.aclMetaReadRoles = aclMetaReadRoles;
            return this;
        }

        public Builder aclMetaWriteRoles(List<String> aclMetaWriteRoles) {
            this.aclMetaWriteRoles = aclMetaWriteRoles;
            return this;
        }

        public Builder customProperty(String name, String value) {
            customProperties.add(Property.of(name, value));
            return this;
        }

        public StreamMetadata build() {
            if (maxCount != null) {
                checkArgument(isPositive(maxCount), "maxCount should be positive");
            }

            if (maxAge != null) {
                checkArgument(!Duration.ZERO.equals(maxAge), "maxAge cannot be zero");
            }

            if (truncateBefore != null) {
                checkArgument(isPositive(truncateBefore), "truncateBefore should be positive");
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

        public static Property of(String name, String value) {
            return new Property(name, value);
        }
    }

}
