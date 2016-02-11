package lt.msemys.esjc.tcp;

import java.time.Duration;

import static lt.msemys.esjc.util.Numbers.isNegative;
import static lt.msemys.esjc.util.Numbers.isPositive;
import static lt.msemys.esjc.util.Preconditions.checkArgument;

public class TcpSettings {
    public final Duration connectTimeout;
    public final Duration closeTimeout;
    public final boolean keepAlive;
    public final boolean tcpNoDelay;
    public final int sendBufferSize;
    public final int receiveBufferSize;
    public final int writeBufferLowWaterMark;
    public final int writeBufferHighWaterMark;

    private TcpSettings(Builder builder) {
        connectTimeout = builder.connectTimeout;
        closeTimeout = builder.closeTimeout;
        keepAlive = builder.keepAlive;
        tcpNoDelay = builder.tcpNoDelay;
        sendBufferSize = builder.sendBufferSize;
        receiveBufferSize = builder.receiveBufferSize;
        writeBufferHighWaterMark = builder.writeBufferHighWaterMark;
        writeBufferLowWaterMark = builder.writeBufferLowWaterMark;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TcpSettings{");
        sb.append("connectTimeout=").append(connectTimeout);
        sb.append(", closeTimeout=").append(closeTimeout);
        sb.append(", keepAlive=").append(keepAlive);
        sb.append(", tcpNoDelay=").append(tcpNoDelay);
        sb.append(", sendBufferSize=").append(sendBufferSize);
        sb.append(", receiveBufferSize=").append(receiveBufferSize);
        sb.append(", writeBufferLowWaterMark=").append(writeBufferLowWaterMark);
        sb.append(", writeBufferHighWaterMark=").append(writeBufferHighWaterMark);
        sb.append('}');
        return sb.toString();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Duration connectTimeout;
        private Duration closeTimeout;
        private Boolean keepAlive;
        private Boolean tcpNoDelay;
        private Integer sendBufferSize;
        private Integer receiveBufferSize;
        private Integer writeBufferHighWaterMark;
        private Integer writeBufferLowWaterMark;

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder closeTimeout(Duration closeTimeout) {
            this.closeTimeout = closeTimeout;
            return this;
        }

        public Builder keepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public Builder tcpNoDelay(boolean tcpNoDelay) {
            this.tcpNoDelay = tcpNoDelay;
            return this;
        }

        public Builder sendBufferSize(int sendBufferSize) {
            this.sendBufferSize = sendBufferSize;
            return this;
        }

        public Builder receiveBufferSize(int receiveBufferSize) {
            this.receiveBufferSize = receiveBufferSize;
            return this;
        }

        public Builder writeBufferHighWaterMark(int writeBufferHighWaterMark) {
            this.writeBufferHighWaterMark = writeBufferHighWaterMark;
            return this;
        }

        public Builder writeBufferLowWaterMark(int writeBufferLowWaterMark) {
            this.writeBufferLowWaterMark = writeBufferLowWaterMark;
            return this;
        }

        public TcpSettings build() {
            if (connectTimeout == null) {
                connectTimeout = Duration.ofSeconds(10);
            }

            if (closeTimeout == null) {
                closeTimeout = Duration.ofMillis(500);
            }

            if (keepAlive == null) {
                keepAlive = true;
            }

            if (tcpNoDelay == null) {
                tcpNoDelay = true;
            }

            if (sendBufferSize == null) {
                sendBufferSize = 65536;
            } else {
                checkArgument(isPositive(sendBufferSize), "sendBufferSize should be positive");
            }

            if (receiveBufferSize == null) {
                receiveBufferSize = 65536;
            } else {
                checkArgument(isPositive(receiveBufferSize), "receiveBufferSize should be positive");
            }

            if (writeBufferHighWaterMark == null) {
                writeBufferHighWaterMark = 65536;
            } else {
                checkArgument(!isNegative(writeBufferHighWaterMark), "writeBufferHighWaterMark should not be negative");
            }

            if (writeBufferLowWaterMark == null) {
                writeBufferLowWaterMark = 1024;
            } else {
                checkArgument(!isNegative(writeBufferLowWaterMark), "writeBufferLowWaterMark should not be negative");
            }

            return new TcpSettings(this);
        }
    }
}
