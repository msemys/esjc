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

        assertEquals("{\"$maxAge\":2," +
            "\"$tb\":17," +
            "\"customString\":\"a string\"," +
            "\"customInt\":-179," +
            "\"customDouble\":1.7," +
            "\"customLong\":123123123123123123," +
            "\"customBoolean\":true," +
            "\"customStringArray\":[\"a\",\"b\",\"c\",null]," +
            "\"customIntArray\":[1,2,3,null]," +
            "\"customLongArray\":[111111111111111111,222222222222222222,333333333333333333,null]," +
            "\"customDoubleArray\":[1.2,3.4,5.6,null]," +
            "\"customBooleanArray\":[true,null,false,null]," +
            "\"customEmptyStringArray\":[]," +
            "\"customEmptyIntArray\":[]," +
            "\"customEmptyLongArray\":[]," +
            "\"customEmptyDoubleArray\":[]," +
            "\"customEmptyBooleanArray\":[]," +
            "\"customNullStringArray\":[null,null]," +
            "\"customNullIntArray\":[null,null]," +
            "\"customNullLongArray\":[null,null]," +
            "\"customNullDoubleArray\":[null,null]," +
            "\"customNullBooleanArray\":[null,null]," +
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
                "\"customStringArray\":[\"a\", \"b\", \"c\", null]," +
                "\"customIntArray\":[1, 2, 3, null]," +
                "\"customLongArray\":[111111111111111111, 222222222222222222, 333333333333333333, null]," +
                "\"customDoubleArray\":[1.2, 3.4, 5.6, null]," +
                "\"customBooleanArray\":[true, null, false, null]," +
                "\"customEmptyArray\":[]," +
                "\"customNullArray\":[null, null]," +
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

        assertArrayEquals(new String[]{"a", "b", "c", null}, streamMetadata.getCustomProperty("customStringArray").toStrings());
        assertArrayEquals(new Integer[]{1, 2, 3, null}, streamMetadata.getCustomProperty("customIntArray").toIntegers());
        assertArrayEquals(new Long[]{111111111111111111l, 222222222222222222l, 333333333333333333l, null}, streamMetadata.getCustomProperty("customLongArray").toLongs());
        assertArrayEquals(new Double[]{1.2, 3.4, 5.6, null}, streamMetadata.getCustomProperty("customDoubleArray").toDoubles());
        assertArrayEquals(new Boolean[]{true, null, false, null}, streamMetadata.getCustomProperty("customBooleanArray").toBooleans());

        assertEquals(0, streamMetadata.getCustomProperty("customEmptyArray").toStrings().length);
        assertEquals(0, streamMetadata.getCustomProperty("customEmptyArray").toIntegers().length);
        assertEquals(0, streamMetadata.getCustomProperty("customEmptyArray").toDoubles().length);
        assertEquals(0, streamMetadata.getCustomProperty("customEmptyArray").toLongs().length);
        assertEquals(0, streamMetadata.getCustomProperty("customEmptyArray").toBooleans().length);

        assertArrayEquals(new String[]{null, null}, streamMetadata.getCustomProperty("customNullArray").toStrings());
        assertArrayEquals(new Integer[]{null, null}, streamMetadata.getCustomProperty("customNullArray").toIntegers());
        assertArrayEquals(new Double[]{null, null}, streamMetadata.getCustomProperty("customNullArray").toDoubles());
        assertArrayEquals(new Long[]{null, null}, streamMetadata.getCustomProperty("customNullArray").toLongs());
        assertArrayEquals(new Boolean[]{null, null}, streamMetadata.getCustomProperty("customNullArray").toBooleans());

        assertArrayEquals(new Integer[]{-179}, streamMetadata.getCustomProperty("customInt").toIntegers());
        assertArrayEquals(new Double[]{1.7}, streamMetadata.getCustomProperty("customDouble").toDoubles());
        assertArrayEquals(new Long[]{123123123123123123l}, streamMetadata.getCustomProperty("customLong").toLongs());
        assertArrayEquals(new Boolean[]{true}, streamMetadata.getCustomProperty("customBoolean").toBooleans());

        assertNull(streamMetadata.getCustomProperty("customNullable").toString());
        assertNull(streamMetadata.getCustomProperty("customNullable").toInteger());
        assertNull(streamMetadata.getCustomProperty("customNullable").toLong());
        assertNull(streamMetadata.getCustomProperty("customNullable").toDouble());
        assertNull(streamMetadata.getCustomProperty("customNullable").toBoolean());
        assertNull(streamMetadata.getCustomProperty("customNullable").toStrings());
        assertNull(streamMetadata.getCustomProperty("customNullable").toIntegers());
        assertNull(streamMetadata.getCustomProperty("customNullable").toDoubles());
        assertNull(streamMetadata.getCustomProperty("customNullable").toLongs());
        assertNull(streamMetadata.getCustomProperty("customNullable").toBooleans());
    }

}
