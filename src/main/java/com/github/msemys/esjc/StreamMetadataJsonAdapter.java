package com.github.msemys.esjc;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class StreamMetadataJsonAdapter extends TypeAdapter<StreamMetadata> {
    private static final String MAX_AGE = "$maxAge";
    private static final String MAX_COUNT = "$maxCount";
    private static final String TRUNCATE_BEFORE = "$tb";
    private static final String CACHE_CONTROL = "$cacheControl";
    private static final String ACL = "$acl";

    private final StreamAclJsonAdapter streamAclJsonAdapter = new StreamAclJsonAdapter();

    @Override
    public void write(JsonWriter writer, StreamMetadata value) throws IOException {
        writer.beginObject();

        if (value.maxCount != null) {
            writer.name(MAX_COUNT).value(value.maxCount);
        }

        if (value.maxAge != null) {
            writer.name(MAX_AGE).value(value.maxAge.getSeconds());
        }

        if (value.truncateBefore != null) {
            writer.name(TRUNCATE_BEFORE).value(value.truncateBefore);
        }

        if (value.cacheControl != null) {
            writer.name(CACHE_CONTROL).value(value.cacheControl.getSeconds());
        }

        if (value.acl != null) {
            writer.name(ACL);
            streamAclJsonAdapter.write(writer, value.acl);
        }

        if (value.customProperties != null) {
            for (StreamMetadata.Property property : value.customProperties) {
                JsonWriter propertyWriter = writer.name(property.name);

                if (property.value == null) {
                    propertyWriter.nullValue();
                } else if (property.value instanceof Number) {
                    propertyWriter.value((Number) property.value);
                } else if (property.value instanceof Boolean) {
                    propertyWriter.value((Boolean) property.value);
                } else if (property.value instanceof String[]) {
                    propertyWriter.beginArray();
                    for (String v : ((String[]) property.value)) {
                        propertyWriter.value(v);
                    }
                    propertyWriter.endArray();
                } else if (property.value instanceof Number[]) {
                    propertyWriter.beginArray();
                    for (Number v : ((Number[]) property.value)) {
                        propertyWriter.value(v);
                    }
                    propertyWriter.endArray();
                } else if (property.value instanceof Boolean[]) {
                    propertyWriter.beginArray();
                    for (Boolean v : ((Boolean[]) property.value)) {
                        propertyWriter.value(v);
                    }
                    propertyWriter.endArray();
                } else {
                    propertyWriter.value(property.value.toString());
                }
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

        while (reader.peek() != JsonToken.END_OBJECT && reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case MAX_COUNT:
                    builder.maxCount(reader.nextInt());
                    break;
                case MAX_AGE:
                    builder.maxAge(Duration.ofSeconds(reader.nextLong()));
                    break;
                case TRUNCATE_BEFORE:
                    builder.truncateBefore(reader.nextInt());
                    break;
                case CACHE_CONTROL:
                    builder.cacheControl(Duration.ofSeconds(reader.nextLong()));
                    break;
                case ACL:
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
                    switch (reader.peek()) {
                        case NULL:
                            reader.nextNull();
                            builder.customProperty(name, (String) null);
                            break;
                        case BEGIN_ARRAY:
                            List<Object> values = new ArrayList<>();

                            reader.beginArray();
                            while (reader.peek() != JsonToken.END_ARRAY) {
                                switch (reader.peek()) {
                                    case NULL:
                                        reader.nextNull();
                                        values.add(null);
                                        break;
                                    case BOOLEAN:
                                        values.add(reader.nextBoolean());
                                        break;
                                    case NUMBER:
                                        values.add(new BigDecimal(reader.nextString()));
                                        break;
                                    case STRING:
                                        values.add(reader.nextString());
                                }
                            }
                            reader.endArray();

                            if (values.stream().anyMatch(v -> v instanceof Boolean)) {
                                builder.customProperty(name, values.stream().toArray(Boolean[]::new));
                            } else if (values.stream().anyMatch(v -> v instanceof Number)) {
                                builder.customProperty(name, values.stream().toArray(Number[]::new));
                            } else {
                                builder.customProperty(name, values.stream().toArray(String[]::new));
                            }

                            break;
                        case BOOLEAN:
                            builder.customProperty(name, reader.nextBoolean());
                            break;
                        case NUMBER:
                            builder.customProperty(name, new BigDecimal(reader.nextString()));
                            break;
                        case STRING:
                            builder.customProperty(name, reader.nextString());
                    }
            }
        }

        reader.endObject();

        return builder.build();
    }

}
