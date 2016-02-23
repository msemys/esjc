package com.github.msemys.esjc.node.static_;

import java.net.InetSocketAddress;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

/**
 * Single node settings.
 */
public class StaticNodeSettings {

    /**
     * Server address.
     */
    public final InetSocketAddress address;

    private StaticNodeSettings(Builder builder) {
        address = builder.address;
    }

    /**
     * Creates a new single-node settings builder.
     *
     * @return single-node settings builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StaticNodeSettings{");
        sb.append("address=").append(address);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Single node settings builder.
     */
    public static class Builder {
        private InetSocketAddress address;

        /**
         * Sets server address.
         *
         * @param host the host name.
         * @param port The port number.
         * @return the builder reference
         */
        public Builder address(String host, int port) {
            return address(new InetSocketAddress(host, port));
        }

        /**
         * Sets server address.
         *
         * @param address the server address.
         * @return the builder reference
         */
        public Builder address(InetSocketAddress address) {
            this.address = address;
            return this;
        }

        /**
         * Builds a static-node settings.
         *
         * @return static-node settings
         */
        public StaticNodeSettings build() {
            checkNotNull(address, "address");
            return new StaticNodeSettings(this);
        }
    }
}
