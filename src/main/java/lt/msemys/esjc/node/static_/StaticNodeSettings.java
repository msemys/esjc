package lt.msemys.esjc.node.static_;

import java.net.InetSocketAddress;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;

public class StaticNodeSettings {
    public final InetSocketAddress address;
    public final boolean ssl;

    private StaticNodeSettings(Builder builder) {
        address = builder.address;
        ssl = builder.ssl;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StaticNodeSettings{");
        sb.append("address=").append(address);
        sb.append(", ssl=").append(ssl);
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {
        private InetSocketAddress address;
        private Boolean ssl;

        public Builder address(String host, int port) {
            return address(new InetSocketAddress(host, port));
        }

        public Builder address(InetSocketAddress address) {
            this.address = address;
            return this;
        }

        public Builder ssl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public StaticNodeSettings build() {
            checkNotNull(address, "address is null");

            if (ssl == null) {
                ssl = false;
            }

            return new StaticNodeSettings(this);
        }
    }
}
