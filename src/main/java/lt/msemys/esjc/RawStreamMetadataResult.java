package lt.msemys.esjc;

import static lt.msemys.esjc.util.Preconditions.checkArgument;
import static lt.msemys.esjc.util.Strings.isNullOrEmpty;

public class RawStreamMetadataResult {
    public final String stream;
    public final boolean isStreamDeleted;
    public final int metastreamVersion;
    public final byte[] streamMetadata;

    public RawStreamMetadataResult(String stream, boolean isStreamDeleted, int metastreamVersion, byte[] streamMetadata) {
        checkArgument(!isNullOrEmpty(stream), "stream");
        this.stream = stream;
        this.isStreamDeleted = isStreamDeleted;
        this.metastreamVersion = metastreamVersion;
        this.streamMetadata = streamMetadata;
    }
}
