package com.github.msemys.esjc;

import com.github.msemys.esjc.util.Throwables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static com.github.msemys.esjc.util.Strings.toBytes;

/**
 * Represents global settings for an Event Store server.
 */
public class SystemSettings {
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(SystemSettings.class, new SystemSettingsJsonAdapter())
        .create();

    /**
     * Default access control list for new user streams.
     */
    public final StreamAcl userStreamAcl;

    /**
     * Default access control list for new system streams.
     */
    public final StreamAcl systemStreamAcl;

    /**
     * Creates a new system settings instance.
     *
     * @param userStreamAcl   default access control list for new user streams.
     * @param systemStreamAcl default access control list for new system streams.
     */
    public SystemSettings(StreamAcl userStreamAcl, StreamAcl systemStreamAcl) {
        this.userStreamAcl = userStreamAcl;
        this.systemStreamAcl = systemStreamAcl;
    }

    /**
     * Converts to JSON representation.
     *
     * @return system settings
     */
    public String toJson() {
        return gson.toJson(this);
    }

    /**
     * Creates a new system settings from the specified JSON.
     *
     * @param json system settings.
     * @return system settings
     */
    public static SystemSettings fromJson(String json) {
        checkArgument(!isNullOrEmpty(json), "json");
        return fromJson(toBytes(json));
    }

    /**
     * Creates a new system settings from the specified JSON.
     *
     * @param bytes system settings.
     * @return system settings
     */
    public static SystemSettings fromJson(byte[] bytes) {
        checkNotNull(bytes, "bytes");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)))) {
            return gson.fromJson(new JsonReader(reader), SystemSettings.class);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public String toString() {
        return String.format("UserStreamAcl: (%s), SystemStreamAcl: (%s)", userStreamAcl, systemStreamAcl);
    }
}
