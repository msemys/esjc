package com.github.msemys.esjc;

import org.junit.Test;

import java.time.Duration;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class StreamMetadataTest {

    @Test
    public void duplicatedEmptyStreamMetadataShouldBeEqual() {
        StreamMetadata streamMetadata = StreamMetadata.empty();

        StreamMetadata result = streamMetadata.toBuilder().build();

        assertEquals(streamMetadata.toJson(), result.toJson());
    }

    @Test
    public void duplicatedStreamMetadataShouldBeEqual() {
        StreamMetadata streamMetadata = StreamMetadata.newBuilder()
            .maxCount(19L)
            .maxAge(Duration.ofSeconds(82))
            .truncateBefore(8L)
            .cacheControl(Duration.ofSeconds(17))
            .aclReadRoles(asList("eric", "kyle", "stan", "kenny"))
            .aclWriteRoles(asList("butters"))
            .aclDeleteRoles(asList("$admins"))
            .aclMetaReadRoles(asList("victoria", "mackey"))
            .aclMetaWriteRoles(asList("randy"))
            .customProperty("customString", "a string")
            .customProperty("customInt", -179)
            .customProperty("customDouble", 1.7)
            .customProperty("customLong", 123123123123123123L)
            .customProperty("customBoolean", true)
            .customProperty("customStringArray", "a", "b", "c", null)
            .customProperty("customIntArray", 1, 2, 3, null)
            .customProperty("customLongArray", 111111111111111111L, 222222222222222222L, 333333333333333333L, null)
            .customProperty("customDoubleArray", 1.2, 3.4, 5.6, null)
            .customProperty("customBooleanArray", true, null, false, null)
            .customProperty("customEmptyStringArray", new String[0])
            .customProperty("customEmptyIntArray", new Integer[0])
            .customProperty("customEmptyLongArray", new Long[0])
            .customProperty("customEmptyDoubleArray", new Double[0])
            .customProperty("customEmptyBooleanArray", new Boolean[0])
            .customProperty("customNullStringArray", (String) null, null)
            .customProperty("customNullIntArray", (Integer) null, null)
            .customProperty("customNullLongArray", (Long) null, null)
            .customProperty("customNullDoubleArray", (Double) null, null)
            .customProperty("customNullBooleanArray", (Boolean) null, null)
            .customProperty("customNullable", (String) null)
            .build();

        StreamMetadata result = streamMetadata.toBuilder().build();

        assertEquals(streamMetadata.toJson(), result.toJson());
    }

    @Test
    public void populatedStreamMetadataBuilderShouldBeMutable() {
        StreamMetadata streamMetadata = StreamMetadata.newBuilder()
            .maxCount(19L)
            .maxAge(Duration.ofSeconds(82))
            .truncateBefore(8L)
            .cacheControl(Duration.ofSeconds(17))
            .aclReadRoles(asList("eric", "kyle", "stan", "kenny"))
            .aclWriteRoles(asList("butters"))
            .aclDeleteRoles(asList("$admins"))
            .aclMetaReadRoles(asList("victoria", "mackey"))
            .aclMetaWriteRoles(asList("randy"))
            .customProperty("customString", "a string")
            .customProperty("customInt", -179)
            .customProperty("customDouble", 1.7)
            .customProperty("customLong", 123123123123123123L)
            .customProperty("customBoolean", true)
            .customProperty("customStringArray", "a", "b", "c", null)
            .customProperty("customIntArray", 1, 2, 3, null)
            .customProperty("customLongArray", 111111111111111111L, 222222222222222222L, 333333333333333333L, null)
            .build();

        StreamMetadata expectedStreamMetadata = StreamMetadata.newBuilder()
            .maxCount(19L)
            .maxAge(Duration.ofSeconds(17))
            .truncateBefore(8L)
            .cacheControl(Duration.ofDays(82))
            .aclReadRoles(asList("foo"))
            .aclWriteRoles(asList("bar"))
            .aclDeleteRoles(asList("baz"))
            .customProperty("customString", "a string")
            .customProperty("customInt", 123)
            .customProperty("customDouble", 2.8)
            .customProperty("customLong", 4444444444444444444L)
            .customProperty("customBoolean", false)
            .customProperty("customStringArray", "a", "b", "c", "d")
            .customProperty("customIntArray", 1, 2, 3, 4)
            .customProperty("customLongArray", (Long[]) null)
            .build();

        StreamMetadata result = streamMetadata.toBuilder()
            .maxAge(Duration.ofSeconds(17))
            .cacheControl(Duration.ofDays(82))
            .aclReadRoles(asList("foo"))
            .aclWriteRoles(asList("bar"))
            .aclDeleteRoles(asList("baz"))
            .aclMetaReadRoles(null)
            .aclMetaWriteRoles(null)
            .customProperty("customInt", 123)
            .customProperty("customDouble", 2.8)
            .customProperty("customLong", 4444444444444444444L)
            .customProperty("customBoolean", false)
            .customProperty("customStringArray", "a", "b", "c", "d")
            .customProperty("customIntArray", 1, 2, 3, 4)
            .customProperty("customLongArray", (Long[]) null)
            .build();

        assertEquals(expectedStreamMetadata.toJson(), result.toJson());
    }

    @Test
    public void shouldSetAclUsingStreamAclBuilder() {
        StreamMetadata streamMetadata = StreamMetadata.newBuilder()
            .aclReadRoles(asList("eric", "kyle", "stan", "kenny"))
            .aclWriteRoles(asList("butters"))
            .aclDeleteRoles(asList("$admins"))
            .aclMetaReadRoles(asList("victoria", "mackey"))
            .aclMetaWriteRoles(asList("randy"))
            .build();

        StreamMetadata result = StreamMetadata.newBuilder()
            .acl(StreamAcl.newBuilder()
                .readRoles(asList("eric", "kyle", "stan", "kenny"))
                .writeRoles(asList("butters"))
                .deleteRoles(asList("$admins"))
                .metaReadRoles(asList("victoria", "mackey"))
                .metaWriteRoles(asList("randy"))
                .build())
            .build();

        assertEquals(streamMetadata.toJson(), result.toJson());
        assertEquals(asList("eric", "kyle", "stan", "kenny"), result.acl.readRoles);
        assertEquals(asList("butters"), result.acl.writeRoles);
        assertEquals(asList("$admins"), result.acl.deleteRoles);
        assertEquals(asList("victoria", "mackey"), result.acl.metaReadRoles);
        assertEquals(asList("randy"), result.acl.metaWriteRoles);
    }

    @Test
    public void updatesAclAfterItIsSetUsingStreamAclBuilder() {
        StreamAcl streamAcl = StreamAcl.newBuilder()
            .readRoles(asList("eric", "kyle", "stan", "kenny"))
            .writeRoles(asList("butters"))
            .deleteRoles(asList("$admins"))
            .metaReadRoles(asList("victoria", "mackey"))
            .metaWriteRoles(asList("randy"))
            .build();

        StreamMetadata streamMetadata = StreamMetadata.newBuilder()
            .aclMetaReadRoles(asList("foo", "bar"))
            .acl(streamAcl)
            .aclWriteRoles(asList("santa"))
            .aclMetaWriteRoles(null)
            .build();

        assertEquals(asList("eric", "kyle", "stan", "kenny"), streamMetadata.acl.readRoles);
        assertEquals(asList("santa"), streamMetadata.acl.writeRoles);
        assertEquals(asList("$admins"), streamMetadata.acl.deleteRoles);
        assertEquals(asList("victoria", "mackey"), streamMetadata.acl.metaReadRoles);
        assertNull(streamMetadata.acl.metaWriteRoles);
    }

    @Test
    public void shouldRemoveAcl() {
        StreamMetadata streamMetadata = StreamMetadata.newBuilder()
            .maxCount(123L)
            .aclReadRoles(asList("eric", "kyle", "stan", "kenny"))
            .aclWriteRoles(asList("butters"))
            .aclDeleteRoles(asList("$admins"))
            .aclMetaWriteRoles(asList("randy"))
            .customProperty("customStringArray", "a", "b", "c", "d")
            .customProperty("customIntArray", 1, 2, 3, 4)
            .build();

        StreamMetadata expectedStreamMetadata = StreamMetadata.newBuilder()
            .maxCount(123L)
            .customProperty("customStringArray", "a", "b", "c", "d")
            .customProperty("customIntArray", 1, 2, 3, 4)
            .build();

        StreamMetadata result = streamMetadata.toBuilder()
            .acl(null)
            .build();

        assertEquals(expectedStreamMetadata.toJson(), result.toJson());
        assertNull(result.acl);
    }

    @Test
    public void findsCustomProperties() {
        StreamMetadata streamMetadata = StreamMetadata.newBuilder()
            .maxCount(19L)
            .maxAge(Duration.ofSeconds(17))
            .truncateBefore(8L)
            .cacheControl(Duration.ofDays(82))
            .customProperty("customString", "a string")
            .customProperty("customInt", 123)
            .customProperty("customStringArray", "a", "b", "c", "d")
            .customProperty("customIntArray", 1, 2, 3, 4)
            .build();

        assertFalse(streamMetadata.findCustomProperty("none").isPresent());
        assertEquals("a string", streamMetadata.findCustomProperty("customString").get().toString());
        assertEquals(123, streamMetadata.findCustomProperty("customInt").get().toInteger().intValue());
        assertArrayEquals(new String[]{"a", "b", "c", "d"}, streamMetadata.findCustomProperty("customStringArray").get().toStrings());
        assertArrayEquals(new Integer[]{1, 2, 3, 4}, streamMetadata.findCustomProperty("customIntArray").get().toIntegers());
    }

}
