package com.github.msemys.esjc;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;

public class StreamAcl {
    public final List<String> readRoles;
    public final List<String> writeRoles;
    public final List<String> deleteRoles;
    public final List<String> metaReadRoles;
    public final List<String> metaWriteRoles;

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
