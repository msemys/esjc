package com.github.msemys.esjc;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;

/**
 * Represents an access-control-list for a stream.
 */
public class StreamAcl {

    /**
     * Roles and users permitted to read the stream.
     */
    public final List<String> readRoles;

    /**
     * Roles and users permitted to write to the stream.
     */
    public final List<String> writeRoles;

    /**
     * Roles and users permitted to delete the stream.
     */
    public final List<String> deleteRoles;

    /**
     * Roles and users permitted to read stream metadata.
     */
    public final List<String> metaReadRoles;

    /**
     * Roles and users permitted to write stream metadata.
     */
    public final List<String> metaWriteRoles;

    private StreamAcl(Builder builder) {
        readRoles = builder.readRoles;
        writeRoles = builder.writeRoles;
        deleteRoles = builder.deleteRoles;
        metaReadRoles = builder.metaReadRoles;
        metaWriteRoles = builder.metaWriteRoles;
    }

    @Override
    public String toString() {
        return String.format("Read: %s, Write: %s, Delete: %s, MetaRead: %s, MetaWrite: %s",
            (readRoles == null) ? "<null>" : "[" + readRoles.stream().collect(joining(",")) + "]",
            (writeRoles == null) ? "<null>" : "[" + writeRoles.stream().collect(joining(",")) + "]",
            (deleteRoles == null) ? "<null>" : "[" + deleteRoles.stream().collect(joining(",")) + "]",
            (metaReadRoles == null) ? "<null>" : "[" + metaReadRoles.stream().collect(joining(",")) + "]",
            (metaWriteRoles == null) ? "<null>" : "[" + metaWriteRoles.stream().collect(joining(",")) + "]");
    }

    /**
     * Creates a new stream access-control-list builder.
     *
     * @return stream access-control-list builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Stream access-control-list builder.
     */
    public static class Builder {
        private List<String> readRoles;
        private List<String> writeRoles;
        private List<String> deleteRoles;
        private List<String> metaReadRoles;
        private List<String> metaWriteRoles;

        private Builder() {
        }

        /**
         * Sets roles and users permitted to read the stream.
         *
         * @param readRoles the list of roles and users.
         * @return the builder reference
         */
        public Builder readRoles(List<String> readRoles) {
            this.readRoles = readRoles;
            return this;
        }

        /**
         * Sets roles and users permitted to write to the stream.
         *
         * @param writeRoles the list of roles and users.
         * @return the builder reference
         */
        public Builder writeRoles(List<String> writeRoles) {
            this.writeRoles = writeRoles;
            return this;
        }

        /**
         * Sets roles and users permitted to delete the stream.
         *
         * @param deleteRoles the list of roles and users.
         * @return the builder reference
         */
        public Builder deleteRoles(List<String> deleteRoles) {
            this.deleteRoles = deleteRoles;
            return this;
        }

        /**
         * Sets roles and users permitted to read stream metadata.
         *
         * @param metaReadRoles the list of roles and users.
         * @return the builder reference
         */
        public Builder metaReadRoles(List<String> metaReadRoles) {
            this.metaReadRoles = metaReadRoles;
            return this;
        }

        /**
         * Sets roles and users permitted to write stream metadata.
         *
         * @param metaWriteRoles the list of roles and users.
         * @return the builder reference
         */
        public Builder metaWriteRoles(List<String> metaWriteRoles) {
            this.metaWriteRoles = metaWriteRoles;
            return this;
        }

        /**
         * Builds a stream access-control-list.
         *
         * @return stream access-control-list
         */
        public StreamAcl build() {
            readRoles = (readRoles != null) ? unmodifiableList(readRoles) : null;
            writeRoles = (writeRoles != null) ? unmodifiableList(writeRoles) : null;
            deleteRoles = (deleteRoles != null) ? unmodifiableList(deleteRoles) : null;
            metaReadRoles = (metaReadRoles != null) ? unmodifiableList(metaReadRoles) : null;
            metaWriteRoles = (metaWriteRoles != null) ? unmodifiableList(metaWriteRoles) : null;

            return new StreamAcl(this);
        }
    }

}
