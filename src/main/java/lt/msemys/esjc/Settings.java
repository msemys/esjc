package lt.msemys.esjc;

import java.net.InetSocketAddress;
import java.time.Duration;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;

public class Settings {

    public final InetSocketAddress address;
    public final Duration reconnectionDelay;
    public final int writeBufferLowWaterMark;
    public final int writeBufferHighWaterMark;

    private Settings(Builder builder) {
        address = builder.address;
        reconnectionDelay = builder.reconnectionDelay;
        writeBufferLowWaterMark = builder.writeBufferLowWaterMark;
        writeBufferHighWaterMark = builder.writeBufferHighWaterMark;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Settings{");
        sb.append("address=").append(address);
        sb.append(", reconnectionDelay=").append(reconnectionDelay);
        sb.append(", writeBufferLowWaterMark=").append(writeBufferLowWaterMark);
        sb.append(", writeBufferHighWaterMark=").append(writeBufferHighWaterMark);
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {
        private InetSocketAddress address;
        private Duration reconnectionDelay;
        private Integer writeBufferLowWaterMark;
        private Integer writeBufferHighWaterMark;

        public Builder withAddress(String host, int port) {
            this.address = new InetSocketAddress(host, port);
            return this;
        }

        public Builder withReconnectionDelay(Duration duration) {
            this.reconnectionDelay = duration;
            return this;
        }

        public Builder withWriteBufferLowWaterMark(int size) {
            this.writeBufferLowWaterMark = size;
            return this;
        }

        public Builder withWriteBufferHighWaterMark(int size) {
            this.writeBufferHighWaterMark = size;
            return this;
        }

        public Settings build() {
            checkNotNull(address, "address is not specified");

            if (reconnectionDelay == null) {
                reconnectionDelay = Duration.ofSeconds(1);
            }

            if (writeBufferLowWaterMark == null) {
                writeBufferLowWaterMark = 8 * 1024;
            }

            if (writeBufferHighWaterMark == null) {
                writeBufferHighWaterMark = 32 * 1024;
            }

            return new Settings(this);
        }
    }

}
