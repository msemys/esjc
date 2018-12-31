package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.WrongExpectedVersionException;
import com.github.msemys.esjc.util.Strings;
import org.junit.Test;

import java.time.Duration;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITWhenWorkingWithStreamMetadataAsStructuredInfo extends AbstractEventStoreTest {

    public ITWhenWorkingWithStreamMetadataAsStructuredInfo(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void setsEmptyMetadata() {
        final String stream = generateStreamName();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.empty()).join();

        RawStreamMetadataResult metadata = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(stream, metadata.stream);
        assertFalse(metadata.isStreamDeleted);
        assertEquals(0, metadata.metastreamVersion);
        assertArrayEquals(Strings.toBytes("{}"), metadata.streamMetadata);
    }

    @Test
    public void settingMetadataFewTimesReturnsLastMetadataInfo() {
        final String stream = generateStreamName();

        StreamMetadata metadata1 = StreamMetadata.newBuilder()
            .maxCount(17L)
            .maxAge(Duration.ofMinutes(10))
            .truncateBefore(10L)
            .cacheControl(Duration.ofMinutes(5))
            .build();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, metadata1).join();

        StreamMetadataResult metadataResult1 = eventstore.getStreamMetadata(stream).join();
        assertEquals(stream, metadataResult1.stream);
        assertFalse(metadataResult1.isStreamDeleted);
        assertEquals(0, metadataResult1.metastreamVersion);
        assertEquals(metadata1.maxCount, metadataResult1.streamMetadata.maxCount);
        assertEquals(metadata1.maxAge, metadataResult1.streamMetadata.maxAge);
        assertEquals(metadata1.truncateBefore, metadataResult1.streamMetadata.truncateBefore);
        assertEquals(metadata1.cacheControl, metadataResult1.streamMetadata.cacheControl);

        StreamMetadata metadata2 = StreamMetadata.newBuilder()
            .maxCount(37L)
            .maxAge(Duration.ofMinutes(20))
            .truncateBefore(24L)
            .cacheControl(Duration.ofMinutes(10))
            .build();

        eventstore.setStreamMetadata(stream, 0, metadata2).join();

        StreamMetadataResult metadataResult2 = eventstore.getStreamMetadata(stream).join();
        assertEquals(stream, metadataResult2.stream);
        assertFalse(metadataResult2.isStreamDeleted);
        assertEquals(1, metadataResult2.metastreamVersion);
        assertEquals(metadata2.maxCount, metadataResult2.streamMetadata.maxCount);
        assertEquals(metadata2.maxAge, metadataResult2.streamMetadata.maxAge);
        assertEquals(metadata2.truncateBefore, metadataResult2.streamMetadata.truncateBefore);
        assertEquals(metadata2.cacheControl, metadataResult2.streamMetadata.cacheControl);
    }

    @Test
    public void failsToSetMetadataWithWrongExpectedVersion() {
        final String stream = generateStreamName();

        try {
            eventstore.setStreamMetadata(stream, 2, StreamMetadata.empty()).join();
            fail("should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

    @Test
    public void setsMetadataWithExpectedVersionAny() {
        final String stream = generateStreamName();

        StreamMetadata metadata1 = StreamMetadata.newBuilder()
            .maxCount(17L)
            .maxAge(Duration.ofMinutes(10))
            .truncateBefore(10L)
            .cacheControl(Duration.ofMinutes(5))
            .build();

        eventstore.setStreamMetadata(stream, ExpectedVersion.ANY, metadata1).join();

        StreamMetadataResult metadataResult1 = eventstore.getStreamMetadata(stream).join();
        assertEquals(stream, metadataResult1.stream);
        assertFalse(metadataResult1.isStreamDeleted);
        assertEquals(0, metadataResult1.metastreamVersion);
        assertEquals(metadata1.maxCount, metadataResult1.streamMetadata.maxCount);
        assertEquals(metadata1.maxAge, metadataResult1.streamMetadata.maxAge);
        assertEquals(metadata1.truncateBefore, metadataResult1.streamMetadata.truncateBefore);
        assertEquals(metadata1.cacheControl, metadataResult1.streamMetadata.cacheControl);

        StreamMetadata metadata2 = StreamMetadata.newBuilder()
            .maxCount(37L)
            .maxAge(Duration.ofMinutes(20))
            .truncateBefore(24L)
            .cacheControl(Duration.ofMinutes(10))
            .build();

        eventstore.setStreamMetadata(stream, ExpectedVersion.ANY, metadata2).join();

        StreamMetadataResult metadataResult2 = eventstore.getStreamMetadata(stream).join();
        assertEquals(stream, metadataResult2.stream);
        assertFalse(metadataResult2.isStreamDeleted);
        assertEquals(1, metadataResult2.metastreamVersion);
        assertEquals(metadata2.maxCount, metadataResult2.streamMetadata.maxCount);
        assertEquals(metadata2.maxAge, metadataResult2.streamMetadata.maxAge);
        assertEquals(metadata2.truncateBefore, metadataResult2.streamMetadata.truncateBefore);
        assertEquals(metadata2.cacheControl, metadataResult2.streamMetadata.cacheControl);
    }

    @Test
    public void setsMetadataForNonExistingStream() {
        final String stream = generateStreamName();

        StreamMetadata metadata = StreamMetadata.newBuilder()
            .maxCount(17L)
            .maxAge(Duration.ofMinutes(10))
            .truncateBefore(10L)
            .cacheControl(Duration.ofMinutes(5))
            .build();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, metadata).join();

        StreamMetadataResult metadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(stream, metadataResult.stream);
        assertFalse(metadataResult.isStreamDeleted);
        assertEquals(0, metadataResult.metastreamVersion);
        assertEquals(metadata.maxCount, metadataResult.streamMetadata.maxCount);
        assertEquals(metadata.maxAge, metadataResult.streamMetadata.maxAge);
        assertEquals(metadata.truncateBefore, metadataResult.streamMetadata.truncateBefore);
        assertEquals(metadata.cacheControl, metadataResult.streamMetadata.cacheControl);
    }

    @Test
    public void setsMetadataForExistingStream() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvent()).join();

        StreamMetadata metadata = StreamMetadata.newBuilder()
            .maxCount(17L)
            .maxAge(Duration.ofMinutes(10))
            .truncateBefore(10L)
            .cacheControl(Duration.ofMinutes(5))
            .build();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, metadata).join();

        StreamMetadataResult metadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(stream, metadataResult.stream);
        assertFalse(metadataResult.isStreamDeleted);
        assertEquals(0, metadataResult.metastreamVersion);
        assertEquals(metadata.maxCount, metadataResult.streamMetadata.maxCount);
        assertEquals(metadata.maxAge, metadataResult.streamMetadata.maxAge);
        assertEquals(metadata.truncateBefore, metadataResult.streamMetadata.truncateBefore);
        assertEquals(metadata.cacheControl, metadataResult.streamMetadata.cacheControl);
    }

    @Test
    public void gettingMetadataForNonExistingStreamReturnsEmptyStreamMetadata() {
        final String stream = generateStreamName();

        StreamMetadataResult metadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(stream, metadataResult.stream);
        assertFalse(metadataResult.isStreamDeleted);
        assertEquals(-1, metadataResult.metastreamVersion);
        assertNull(metadataResult.streamMetadata.maxCount);
        assertNull(metadataResult.streamMetadata.maxAge);
        assertNull(metadataResult.streamMetadata.truncateBefore);
        assertNull(metadataResult.streamMetadata.cacheControl);
    }

    @Test
    public void gettingMetadataForDeletedStreamReturnsEmptyStreamMetadataAndSignalsStreamDeletion() {
        final String stream = generateStreamName();

        StreamMetadata metadata = StreamMetadata.newBuilder()
            .maxCount(17L)
            .maxAge(Duration.ofMinutes(10))
            .truncateBefore(10L)
            .cacheControl(Duration.ofMinutes(5))
            .build();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, metadata).join();

        eventstore.deleteStream(stream, ExpectedVersion.NO_STREAM, true).join();

        StreamMetadataResult metadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(stream, metadataResult.stream);
        assertTrue(metadataResult.isStreamDeleted);
        assertEquals(Long.MAX_VALUE, metadataResult.metastreamVersion);
        assertNull(metadataResult.streamMetadata.maxCount);
        assertNull(metadataResult.streamMetadata.maxAge);
        assertNull(metadataResult.streamMetadata.truncateBefore);
        assertNull(metadataResult.streamMetadata.cacheControl);
        assertNull(metadataResult.streamMetadata.acl);
    }

    @Test
    public void settingCorrectlyFormattedMetadataAsRawAllowsToReadItAsStructuredMetadata() {
        final String stream = generateStreamName();

        byte[] rawMetadata = Strings.toBytes("{ " +
            "  \"$maxCount\": 17," +
            "  \"$maxAge\": 123321," +
            "  \"$tb\": 23," +
            "  \"$cacheControl\": 7654321," +
            "  \"$acl\": {" +
            "      \"$r\": \"readRole\"," +
            "      \"$w\": \"writeRole\"," +
            "      \"$d\": \"deleteRole\"," +
            "      \"$mw\": \"metaWriteRole\"" +
            "  }, " +
            "  \"customString\": \"a string\"," +
            "  \"customInt\": -179," +
            "  \"customDouble\": 1.7," +
            "  \"customLong\": 123123123123123123," +
            "  \"customBool\": true," +
            "  \"customNullable\": null" +
            "}");

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, rawMetadata).join();

        StreamMetadataResult metadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(stream, metadataResult.stream);
        assertFalse(metadataResult.isStreamDeleted);
        assertEquals(0, metadataResult.metastreamVersion);
        assertEquals(Long.valueOf(17), metadataResult.streamMetadata.maxCount);
        assertEquals(Duration.ofSeconds(123321), metadataResult.streamMetadata.maxAge);
        assertEquals(Long.valueOf(23), metadataResult.streamMetadata.truncateBefore);
        assertEquals(Duration.ofSeconds(7654321), metadataResult.streamMetadata.cacheControl);

        assertNotNull(metadataResult.streamMetadata.acl);
        assertThat(metadataResult.streamMetadata.acl.readRoles, hasItems("readRole"));
        assertThat(metadataResult.streamMetadata.acl.writeRoles, hasItems("writeRole"));
        assertThat(metadataResult.streamMetadata.acl.deleteRoles, hasItems("deleteRole"));
        assertThat(metadataResult.streamMetadata.acl.metaWriteRoles, hasItems("metaWriteRole"));

        assertEquals("a string", metadataResult.streamMetadata.getCustomProperty("customString").toString());
        assertEquals(Integer.valueOf(-179), metadataResult.streamMetadata.getCustomProperty("customInt").toInteger());
        assertEquals(Double.valueOf(1.7), metadataResult.streamMetadata.getCustomProperty("customDouble").toDouble());
        assertEquals(Long.valueOf(123123123123123123L), metadataResult.streamMetadata.getCustomProperty("customLong").toLong());
        assertTrue(metadataResult.streamMetadata.getCustomProperty("customBool").toBoolean());
        assertNull(metadataResult.streamMetadata.getCustomProperty("customNullable").toInteger());
    }

    @Test
    public void settingStructuredMetadataWithCustomPropertiesReturnsThemUntouched() {
        final String stream = generateStreamName();

        StreamMetadata metadata = StreamMetadata.newBuilder()
            .maxCount(17L)
            .maxAge(Duration.ofSeconds(123321))
            .truncateBefore(23L)
            .cacheControl(Duration.ofSeconds(7654321))
            .aclReadRoles(asList("readRole"))
            .aclWriteRoles(asList("writeRole"))
            .aclDeleteRoles(asList("deleteRole"))
            .aclMetaWriteRoles(asList("metaWriteRole"))
            .customProperty("customString", "a string")
            .customProperty("customInt", -179)
            .customProperty("customDouble", 1.7)
            .customProperty("customLong", 123123123123123123L)
            .customProperty("customBool", true)
            .customProperty("customNullable", (Integer) null)
            .build();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, metadata).join();

        StreamMetadataResult metadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(stream, metadataResult.stream);
        assertFalse(metadataResult.isStreamDeleted);
        assertEquals(0, metadataResult.metastreamVersion);
        assertEquals(Long.valueOf(17), metadataResult.streamMetadata.maxCount);
        assertEquals(Duration.ofSeconds(123321), metadataResult.streamMetadata.maxAge);
        assertEquals(Long.valueOf(23), metadataResult.streamMetadata.truncateBefore);
        assertEquals(Duration.ofSeconds(7654321), metadataResult.streamMetadata.cacheControl);

        assertNotNull(metadataResult.streamMetadata.acl);
        assertThat(metadataResult.streamMetadata.acl.readRoles, hasItems("readRole"));
        assertThat(metadataResult.streamMetadata.acl.writeRoles, hasItems("writeRole"));
        assertThat(metadataResult.streamMetadata.acl.deleteRoles, hasItems("deleteRole"));
        assertThat(metadataResult.streamMetadata.acl.metaWriteRoles, hasItems("metaWriteRole"));

        assertEquals("a string", metadataResult.streamMetadata.getCustomProperty("customString").toString());
        assertEquals(Integer.valueOf(-179), metadataResult.streamMetadata.getCustomProperty("customInt").toInteger());
        assertEquals(Double.valueOf(1.7), metadataResult.streamMetadata.getCustomProperty("customDouble").toDouble());
        assertEquals(Long.valueOf(123123123123123123L), metadataResult.streamMetadata.getCustomProperty("customLong").toLong());
        assertTrue(metadataResult.streamMetadata.getCustomProperty("customBool").toBoolean());
        assertNull(metadataResult.streamMetadata.getCustomProperty("customNullable").toInteger());
    }

    @Test
    public void settingStructuredMetadataWithMultipleRolesCanBeReadBack() {
        final String stream = generateStreamName();

        StreamMetadata metadata = StreamMetadata.newBuilder()
            .aclReadRoles(asList("r1", "r2", "r3"))
            .aclWriteRoles(asList("w1", "w2"))
            .aclDeleteRoles(asList("d1", "d2", "d3", "d4"))
            .aclMetaWriteRoles(asList("mw1", "mw2"))
            .build();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, metadata).join();

        StreamMetadataResult metadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(stream, metadataResult.stream);
        assertFalse(metadataResult.isStreamDeleted);
        assertEquals(0, metadataResult.metastreamVersion);

        assertNotNull(metadataResult.streamMetadata.acl);
        assertThat(metadataResult.streamMetadata.acl.readRoles, hasItems("r1", "r2", "r3"));
        assertThat(metadataResult.streamMetadata.acl.writeRoles, hasItems("w1", "w2"));
        assertThat(metadataResult.streamMetadata.acl.deleteRoles, hasItems("d1", "d2", "d3", "d4"));
        assertThat(metadataResult.streamMetadata.acl.metaWriteRoles, hasItems("mw1", "mw2"));
    }

    @Test
    public void settingCorrectMetadataWithMultipleRolesInAclAllowsToReadItAsStructuredMetadata() {
        final String stream = generateStreamName();

        byte[] rawMetadata = Strings.toBytes("{ " +
            "  \"$acl\": {" +
            "  \"$r\": [\"r1\", \"r2\", \"r3\"]," +
            "  \"$w\": [\"w1\", \"w2\"]," +
            "  \"$d\": [\"d1\", \"d2\", \"d3\", \"d4\"]," +
            "  \"$mw\": [\"mw1\", \"mw2\"]" +
            "  }" +
            "}");

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, rawMetadata).join();

        StreamMetadataResult metadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(stream, metadataResult.stream);
        assertFalse(metadataResult.isStreamDeleted);
        assertEquals(0, metadataResult.metastreamVersion);

        assertNotNull(metadataResult.streamMetadata.acl);
        assertThat(metadataResult.streamMetadata.acl.readRoles, hasItems("r1", "r2", "r3"));
        assertThat(metadataResult.streamMetadata.acl.writeRoles, hasItems("w1", "w2"));
        assertThat(metadataResult.streamMetadata.acl.deleteRoles, hasItems("d1", "d2", "d3", "d4"));
        assertThat(metadataResult.streamMetadata.acl.metaWriteRoles, hasItems("mw1", "mw2"));
    }

}
