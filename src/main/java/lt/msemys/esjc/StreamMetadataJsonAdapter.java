package lt.msemys.esjc;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import lt.msemys.esjc.system.SystemMetadata;

import java.io.IOException;
import java.time.Duration;

public class StreamMetadataJsonAdapter extends TypeAdapter<StreamMetadata> {
    private final StreamAclJsonAdapter streamAclJsonAdapter = new StreamAclJsonAdapter();

    @Override
    public void write(JsonWriter writer, StreamMetadata value) throws IOException {
        writer.beginObject();

        if (value.maxCount != null) {
            writer.name(SystemMetadata.MAX_COUNT).value(value.maxCount);
        }

        if (value.maxAge != null) {
            writer.name(SystemMetadata.MAX_AGE).value(value.maxAge.getSeconds());
        }

        if (value.truncateBefore != null) {
            writer.name(SystemMetadata.TRUNCATE_BEFORE).value(value.truncateBefore);
        }

        if (value.cacheControl != null) {
            writer.name(SystemMetadata.CACHE_CONTROL).value(value.cacheControl.getSeconds());
        }

        if (value.acl != null) {
            writer.name(SystemMetadata.ACL);
            streamAclJsonAdapter.write(writer, value.acl);
        }

        if (value.customProperties != null) {
            for (StreamMetadata.Property property : value.customProperties) {
                writer.name(property.name).value(property.value);
            }
        }

        writer.endObject();
    }

    @Override
    public StreamMetadata read(JsonReader reader) throws IOException {
        StreamMetadata.Builder builder = StreamMetadata.newBuilder();

        if (reader.peek() == JsonToken.NULL) {
            return null;
        }

        reader.beginObject();

        while (reader.peek() != JsonToken.END_OBJECT) {
            String name = reader.nextName();
            switch (name) {
                case SystemMetadata.MAX_COUNT:
                    builder.maxCount(reader.nextInt());
                    break;
                case SystemMetadata.MAX_AGE:
                    builder.maxAge(Duration.ofSeconds(reader.nextLong()));
                    break;
                case SystemMetadata.TRUNCATE_BEFORE:
                    builder.truncateBefore(reader.nextInt());
                    break;
                case SystemMetadata.CACHE_CONTROL:
                    builder.cacheControl(Duration.ofSeconds(reader.nextLong()));
                    break;
                case SystemMetadata.ACL:
                    StreamAcl acl = streamAclJsonAdapter.read(reader);
                    if (acl != null) {
                        builder.aclReadRoles(acl.readRoles);
                        builder.aclWriteRoles(acl.writeRoles);
                        builder.aclDeleteRoles(acl.deleteRoles);
                        builder.aclMetaReadRoles(acl.metaReadRoles);
                        builder.aclMetaWriteRoles(acl.metaWriteRoles);
                    }
                    break;
                default:
                    builder.customProperty(name, reader.nextString());
            }
        }

        reader.endObject();

        return builder.build();
    }

}
