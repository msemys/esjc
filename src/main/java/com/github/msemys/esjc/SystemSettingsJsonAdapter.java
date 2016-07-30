package com.github.msemys.esjc;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class SystemSettingsJsonAdapter extends TypeAdapter<SystemSettings> {
    private static final String USER_STREAM_ACL = "$userStreamAcl";
    private static final String SYSTEM_STREAM_ACL = "$systemStreamAcl";

    private final StreamAclJsonAdapter streamAclJsonAdapter = new StreamAclJsonAdapter();

    @Override
    public void write(JsonWriter writer, SystemSettings value) throws IOException {
        writer.beginObject();

        if (value.userStreamAcl != null) {
            writer.name(USER_STREAM_ACL);
            streamAclJsonAdapter.write(writer, value.userStreamAcl);
        }

        if (value.systemStreamAcl != null) {
            writer.name(SYSTEM_STREAM_ACL);
            streamAclJsonAdapter.write(writer, value.systemStreamAcl);
        }

        writer.endObject();
    }

    @Override
    public SystemSettings read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            return null;
        }

        SystemSettings.Builder builder = SystemSettings.newBuilder();

        reader.beginObject();

        while (reader.peek() != JsonToken.END_OBJECT && reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case USER_STREAM_ACL:
                    builder.userStreamAcl(streamAclJsonAdapter.read(reader));
                    break;
                case SYSTEM_STREAM_ACL:
                    builder.systemStreamAcl(streamAclJsonAdapter.read(reader));
                    break;
            }
        }

        reader.endObject();

        return builder.build();
    }
}
