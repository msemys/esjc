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

    /**
     * Creates a new stream access-control-list.
     *
     * @param readRoles      roles and users permitted to read the stream.
     * @param writeRoles     roles and users permitted to write to the stream.
     * @param deleteRoles    roles and users permitted to delete the stream.
     * @param metaReadRoles  roles and users permitted to read stream metadata.
     * @param metaWriteRoles roles and users permitted to write stream metadata.
     */
    public StreamAcl(List<String> readRoles,
                     List<String> writeRoles,
                     List<String> deleteRoles,
                     List<String> metaReadRoles,
                     List<String> metaWriteRoles) {
        this.readRoles = (readRoles != null) ? unmodifiableList(readRoles) : null;
        this.writeRoles = (writeRoles != null) ? unmodifiableList(writeRoles) : null;
        this.deleteRoles = (deleteRoles != null) ? unmodifiableList(deleteRoles) : null;
        this.metaReadRoles = (metaReadRoles != null) ? unmodifiableList(metaReadRoles) : null;
        this.metaWriteRoles = (metaWriteRoles != null) ? unmodifiableList(metaWriteRoles) : null;
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

}
