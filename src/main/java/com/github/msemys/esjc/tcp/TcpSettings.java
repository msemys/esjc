package com.github.msemys.esjc.tcp;

import java.time.Duration;

import static com.github.msemys.esjc.util.Numbers.isNegative;
import static com.github.msemys.esjc.util.Numbers.isPositive;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;

/**
 * TCP settings.
 */
public class TcpSettings {

    /**
     * Specifies connection establishment timeout.
     */
    public final Duration connectTimeout;

    /**
     * Specifies connection closing timeout.
     */
    public final Duration closeTimeout;

    /**
     * Whether the socket keep-alive option is enabled.
     */
    public final boolean keepAlive;

    /**
     * Whether the socket no-delay option is enabled.
     */
    public final boolean tcpNoDelay;

    /**
     * The maximum socket send buffer in bytes.
     */
    public final int sendBufferSize;

    /**
     * The maximum socket receive buffer in bytes.
     */
    public final int receiveBufferSize;

    /**
     * Write buffer low watermark in bytes.
     */
    public final int writeBufferLowWaterMark;

    /**
     * Write buffer high watermark in bytes.
     */
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

    /**
     * Creates a new TCP settings builder.
     *
     * @return TCP settings builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * TCP settings builder.
     */
    public static class Builder {
        private Duration connectTimeout;
        private Duration closeTimeout;
        private Boolean keepAlive;
        private Boolean tcpNoDelay;
        private Integer sendBufferSize;
        private Integer receiveBufferSize;
        private Integer writeBufferHighWaterMark;
        private Integer writeBufferLowWaterMark;

        /**
         * Sets connection establishment timeout.
         *
         * @param connectTimeout connection establishment timeout.
         * @return the builder reference
         */
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * Sets connection closing timeout.
         *
         * @param closeTimeout connection closing timeout.
         * @return the builder reference
         */
        public Builder closeTimeout(Duration closeTimeout) {
            this.closeTimeout = closeTimeout;
            return this;
        }

        /**
         * Specifies socket keep-alive option.
         *
         * @param keepAlive {@code true} to enable.
         * @return the builder reference
         */
        public Builder keepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        /**
         * Specifies socket no-delay option.
         * <p>
         * When no-delay is enabled, Nagle's algorithm will not be use.
         * </p>
         *
         * @param tcpNoDelay {@code true} to enable.
         * @return the builder reference
         */
        public Builder tcpNoDelay(boolean tcpNoDelay) {
            this.tcpNoDelay = tcpNoDelay;
            return this;
        }

        /**
         * Sets the maximum socket send buffer in bytes.
         *
         * @param sendBufferSize the maximum socket send buffer in bytes.
         * @return the builder reference
         */
        public Builder sendBufferSize(int sendBufferSize) {
            this.sendBufferSize = sendBufferSize;
            return this;
        }

        /**
         * Sets the maximum socket receive buffer in bytes.
         *
         * @param receiveBufferSize the maximum socket receive buffer in bytes.
         * @return the builder reference
         */
        public Builder receiveBufferSize(int receiveBufferSize) {
            this.receiveBufferSize = receiveBufferSize;
            return this;
        }

        /**
         * Sets write buffer high watermark in bytes.
         *
         * @param writeBufferHighWaterMark write buffer high watermark in bytes.
         * @return the builder reference
         */
        public Builder writeBufferHighWaterMark(int writeBufferHighWaterMark) {
            this.writeBufferHighWaterMark = writeBufferHighWaterMark;
            return this;
        }

        /**
         * Sets write buffer low watermark in bytes.
         *
         * @param writeBufferLowWaterMark write buffer low watermark in bytes.
         * @return the builder reference
         */
        public Builder writeBufferLowWaterMark(int writeBufferLowWaterMark) {
            this.writeBufferLowWaterMark = writeBufferLowWaterMark;
            return this;
        }

        /**
         * Builds a TCP settings.
         *
         * @return TCP settings
         */
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
                sendBufferSize = 64 * 1024;
            } else {
                checkArgument(isPositive(sendBufferSize), "sendBufferSize should be positive");
            }

            if (receiveBufferSize == null) {
                receiveBufferSize = 64 * 1024;
            } else {
                checkArgument(isPositive(receiveBufferSize), "receiveBufferSize should be positive");
            }

            if (writeBufferHighWaterMark == null) {
                writeBufferHighWaterMark = 64 * 1024;
            } else {
                checkArgument(!isNegative(writeBufferHighWaterMark), "writeBufferHighWaterMark should not be negative");
            }

            if (writeBufferLowWaterMark == null) {
                writeBufferLowWaterMark = 32 * 1024;
            } else {
                checkArgument(!isNegative(writeBufferLowWaterMark), "writeBufferLowWaterMark should not be negative");
            }

            return new TcpSettings(this);
        }
    }
}
