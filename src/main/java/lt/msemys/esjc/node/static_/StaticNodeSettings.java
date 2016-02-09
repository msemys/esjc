package lt.msemys.esjc.node.static_;

import java.net.InetSocketAddress;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;

public class StaticNodeSettings {
    public final InetSocketAddress address;

    private StaticNodeSettings(Builder builder) {
        address = builder.address;
    }

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

    public static class Builder {
        private InetSocketAddress address;

        public Builder address(String host, int port) {
            return address(new InetSocketAddress(host, port));
        }

        public Builder address(InetSocketAddress address) {
            this.address = address;
            return this;
        }

        public StaticNodeSettings build() {
            checkNotNull(address, "address");
            return new StaticNodeSettings(this);
        }
    }
}
