package com.github.msemys.esjc;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;

/**
 * Represents stream metadata as a series of properties for system data and a byte array for user metadata.
 */
public class RawStreamMetadataResult {

    /**
     * The name of the stream.
     */
    public final String stream;

    /**
     * Indicates whether or not the stream is soft-deleted.
     */
    public final boolean isStreamDeleted;

    /**
     * The version of the metadata format.
     */
    public final int metastreamVersion;

    /**
     * A byte array containing user-specified metadata.
     */
    public final byte[] streamMetadata;

    /**
     * Creates a new instance.
     *
     * @param stream            the name of the stream.
     * @param isStreamDeleted   whether the stream is soft-deleted.
     * @param metastreamVersion the version of the metadata format.
     * @param streamMetadata    a byte array containing user-specified metadata.
     */
    public RawStreamMetadataResult(String stream, boolean isStreamDeleted, int metastreamVersion, byte[] streamMetadata) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        this.stream = stream;
        this.isStreamDeleted = isStreamDeleted;
        this.metastreamVersion = metastreamVersion;
        this.streamMetadata = streamMetadata;
    }
}
