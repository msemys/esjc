package com.github.msemys.esjc;

import org.junit.Test;

import java.time.Duration;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

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
            .maxCount(19)
            .maxAge(Duration.ofSeconds(82))
            .truncateBefore(8)
            .cacheControl(Duration.ofSeconds(17))
            .aclReadRoles(asList("eric", "kyle", "stan", "kenny"))
            .aclWriteRoles(asList("butters"))
            .aclDeleteRoles(asList("$admins"))
            .aclMetaReadRoles(asList("victoria", "mackey"))
            .aclMetaWriteRoles(asList("randy"))
            .customProperty("customString", "a string")
            .customProperty("customInt", -179)
            .customProperty("customDouble", 1.7)
            .customProperty("customLong", 123123123123123123l)
            .customProperty("customBoolean", true)
            .customProperty("customStringArray", "a", "b", "c", null)
            .customProperty("customIntArray", 1, 2, 3, null)
            .customProperty("customLongArray", 111111111111111111l, 222222222222222222l, 333333333333333333l, null)
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
            .maxCount(19)
            .maxAge(Duration.ofSeconds(82))
            .truncateBefore(8)
            .cacheControl(Duration.ofSeconds(17))
            .aclReadRoles(asList("eric", "kyle", "stan", "kenny"))
            .aclWriteRoles(asList("butters"))
            .aclDeleteRoles(asList("$admins"))
            .aclMetaReadRoles(asList("victoria", "mackey"))
            .aclMetaWriteRoles(asList("randy"))
            .customProperty("customString", "a string")
            .customProperty("customInt", -179)
            .customProperty("customDouble", 1.7)
            .customProperty("customLong", 123123123123123123l)
            .customProperty("customBoolean", true)
            .customProperty("customStringArray", "a", "b", "c", null)
            .customProperty("customIntArray", 1, 2, 3, null)
            .customProperty("customLongArray", 111111111111111111l, 222222222222222222l, 333333333333333333l, null)
            .build();

        StreamMetadata expectedStreamMetadata = StreamMetadata.newBuilder()
            .maxCount(19)
            .maxAge(Duration.ofSeconds(17))
            .truncateBefore(8)
            .cacheControl(Duration.ofDays(82))
            .aclReadRoles(asList("foo"))
            .aclWriteRoles(asList("bar"))
            .aclDeleteRoles(asList("baz"))
            .customProperty("customString", "a string")
            .customProperty("customInt", 123)
            .customProperty("customDouble", 2.8)
            .customProperty("customLong", 4444444444444444444l)
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
            .customProperty("customLong", 4444444444444444444l)
            .customProperty("customBoolean", false)
            .customProperty("customStringArray", "a", "b", "c", "d")
            .customProperty("customIntArray", 1, 2, 3, 4)
            .customProperty("customLongArray", (Long[]) null)
            .build();

        assertEquals(expectedStreamMetadata.toJson(), result.toJson());
    }

}
