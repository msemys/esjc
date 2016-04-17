package com.github.msemys.esjc;

import org.junit.Test;

import java.time.Duration;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class StreamMetadataJsonAdapterTest {

    @Test
    public void serializesWithAcl() {
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
            .build();

        assertEquals("{\"$maxCount\":19," +
            "\"$maxAge\":82," +
            "\"$tb\":8," +
            "\"$cacheControl\":17," +
            "\"$acl\":{" +
            "\"$r\":[\"eric\",\"kyle\",\"stan\",\"kenny\"]," +
            "\"$w\":\"butters\"," +
            "\"$d\":\"$admins\"," +
            "\"$mr\":[\"victoria\",\"mackey\"]," +
            "\"$mw\":\"randy\"}}", streamMetadata.toJson());
    }

    @Test
    public void serializesWithCustomProperties() {
        StreamMetadata streamMetadata = StreamMetadata.newBuilder()
            .maxAge(Duration.ofSeconds(2))
            .truncateBefore(17)
            .customProperty("customString", "a string")
            .customProperty("customInt", -179)
            .customProperty("customDouble", 1.7)
            .customProperty("customLong", 123123123123123123l)
            .customProperty("customBoolean", true)
            .customProperty("customNullable", (String) null)
            .build();

        assertEquals("{\"$maxAge\":2," +
            "\"$tb\":17," +
            "\"customString\":\"a string\"," +
            "\"customInt\":-179," +
            "\"customDouble\":1.7," +
            "\"customLong\":123123123123123123," +
            "\"customBoolean\":true," +
            "\"customNullable\":null}", streamMetadata.toJson());
    }

    @Test
    public void deserializesWithAcl() {
        StreamMetadata streamMetadata = StreamMetadata.fromJson("{\"$maxCount\":19," +
            "\"$maxAge\":82," +
            "\"$tb\":8," +
            "\"$cacheControl\":17," +
            "\"$acl\":{" +
            "\"$r\":[\"eric\",\"kyle\",\"stan\",\"kenny\"]," +
            "\"$w\":\"butters\"," +
            "\"$d\":\"$admins\"," +
            "\"$mr\":[\"victoria\",\"mackey\"]," +
            "\"$mw\":\"randy\"}}");

        assertEquals(new Integer(19), streamMetadata.maxCount);
        assertEquals(Duration.ofSeconds(82), streamMetadata.maxAge);
        assertEquals(new Integer(8), streamMetadata.truncateBefore);
        assertEquals(Duration.ofSeconds(17), streamMetadata.cacheControl);
        assertNotNull(streamMetadata.acl);
        assertNotNull(streamMetadata.acl.readRoles);
        assertEquals(asList("eric", "kyle", "stan", "kenny"), streamMetadata.acl.readRoles);
        assertNotNull(streamMetadata.acl.writeRoles);
        assertEquals(asList("butters"), streamMetadata.acl.writeRoles);
        assertNotNull(streamMetadata.acl.deleteRoles);
        assertEquals(asList("$admins"), streamMetadata.acl.deleteRoles);
        assertNotNull(streamMetadata.acl.metaReadRoles);
        assertEquals(asList("victoria", "mackey"), streamMetadata.acl.metaReadRoles);
        assertNotNull(streamMetadata.acl.metaWriteRoles);
        assertEquals(asList("randy"), streamMetadata.acl.metaWriteRoles);
        assertNotNull(streamMetadata.customProperties);
        assertTrue(streamMetadata.customProperties.isEmpty());
    }

    @Test
    public void deserializesWithCustomProperties() {
        StreamMetadata streamMetadata = StreamMetadata
            .fromJson("{\"$maxAge\":2," +
                "\"$tb\":17," +
                "\"customString\":\"a string\"," +
                "\"customInt\":-179," +
                "\"customDouble\":1.7," +
                "\"customLong\":123123123123123123," +
                "\"customBoolean\":true," +
                "\"customNullable\":null}");

        assertNull(streamMetadata.maxCount);
        assertEquals(Duration.ofSeconds(2), streamMetadata.maxAge);
        assertEquals(new Integer(17), streamMetadata.truncateBefore);
        assertNull(streamMetadata.cacheControl);
        assertNull(streamMetadata.acl);
        assertNotNull(streamMetadata.customProperties);
        assertEquals("a string", streamMetadata.getCustomProperty("customString").toString());
        assertEquals(Integer.valueOf(-179), streamMetadata.getCustomProperty("customInt").toInteger());
        assertEquals(Double.valueOf(1.7), streamMetadata.getCustomProperty("customDouble").toDouble());
        assertEquals(Long.valueOf(123123123123123123l), streamMetadata.getCustomProperty("customLong").toLong());
        assertTrue(streamMetadata.getCustomProperty("customBoolean").toBoolean());
        assertNull(streamMetadata.getCustomProperty("customNullable").toString());
        assertNull(streamMetadata.getCustomProperty("customNullable").toInteger());
        assertNull(streamMetadata.getCustomProperty("customNullable").toLong());
        assertNull(streamMetadata.getCustomProperty("customNullable").toDouble());
        assertNull(streamMetadata.getCustomProperty("customNullable").toBoolean());
    }

}
