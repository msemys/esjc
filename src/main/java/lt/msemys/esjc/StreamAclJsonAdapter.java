package lt.msemys.esjc;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import lt.msemys.esjc.system.SystemMetadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class StreamAclJsonAdapter extends TypeAdapter<StreamAcl> {

    @Override
    public void write(JsonWriter writer, StreamAcl value) throws IOException {
        writer.beginObject();
        writeRoles(writer, SystemMetadata.ACL_READ, value.readRoles);
        writeRoles(writer, SystemMetadata.ACL_WRITE, value.writeRoles);
        writeRoles(writer, SystemMetadata.ACL_DELETE, value.deleteRoles);
        writeRoles(writer, SystemMetadata.ACL_META_READ, value.metaReadRoles);
        writeRoles(writer, SystemMetadata.ACL_META_WRITE, value.metaWriteRoles);
        writer.endObject();
    }

    @Override
    public StreamAcl read(JsonReader reader) throws IOException {
        List<String> readRoles = null;
        List<String> writeRoles = null;
        List<String> deleteRoles = null;
        List<String> metaReadRoles = null;
        List<String> metaWriteRoles = null;

        reader.beginObject();

        while (reader.peek() != JsonToken.END_OBJECT) {
            String name = reader.nextName();
            switch (name) {
                case SystemMetadata.ACL_READ:
                    readRoles = readRoles(reader);
                    break;
                case SystemMetadata.ACL_WRITE:
                    writeRoles = readRoles(reader);
                    break;
                case SystemMetadata.ACL_DELETE:
                    deleteRoles = readRoles(reader);
                    break;
                case SystemMetadata.ACL_META_READ:
                    metaReadRoles = readRoles(reader);
                    break;
                case SystemMetadata.ACL_META_WRITE:
                    metaWriteRoles = readRoles(reader);
                    break;
            }
        }

        reader.endObject();

        return new StreamAcl(readRoles, writeRoles, deleteRoles, metaReadRoles, metaWriteRoles);
    }

    private static void writeRoles(JsonWriter writer, String name, List<String> roles) throws IOException {
        if (roles != null) {
            writer.name(name);
            if (roles.size() == 1) {
                writer.value(roles.get(0));
            } else {
                writer.beginArray();
                for (String role : roles) {
                    writer.value(role);
                }
                writer.endArray();
            }
        }
    }

    private static List<String> readRoles(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.STRING) {
            return asList(reader.nextString());
        } else {
            List<String> roles = new ArrayList<>();

            reader.beginArray();

            while (reader.peek() != JsonToken.END_ARRAY) {
                roles.add(reader.nextString());
            }

            reader.endArray();

            return roles;
        }
    }
}
