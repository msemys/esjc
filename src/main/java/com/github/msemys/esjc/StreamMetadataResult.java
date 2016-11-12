package com.github.msemys.esjc;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;

/**
 * Represents stream metadata as a series of properties for system data and
 * a {@link StreamMetadata} object for user metadata.
 */
public class StreamMetadataResult {

    /**
     * The name of the stream.
     */
    public final String stream;

    /**
     * Indicates whether or not the stream is deleted.
     */
    public final boolean isStreamDeleted;

    /**
     * The version of the metadata format.
     */
    public final int metastreamVersion;

    /**
     * User-specified stream metadata.
     */
    public final StreamMetadata streamMetadata;

    /**
     * Creates a new instance.
     *
     * @param stream            the name of the stream.
     * @param isStreamDeleted   whether the stream is soft-deleted.
     * @param metastreamVersion the version of the metadata format.
     * @param streamMetadata    user-specified stream metadata.
     */
    public StreamMetadataResult(String stream, boolean isStreamDeleted, int metastreamVersion, StreamMetadata streamMetadata) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        this.stream = stream;
        this.isStreamDeleted = isStreamDeleted;
        this.metastreamVersion = metastreamVersion;
        this.streamMetadata = streamMetadata;
    }
}
