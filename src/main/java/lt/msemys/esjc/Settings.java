package lt.msemys.esjc;

import static lt.msemys.esjc.util.Preconditions.checkArgument;
import static lt.msemys.esjc.util.Preconditions.checkNotNull;

public class Settings {

    public final String host;
    public final int port;

    private Settings(Builder builder) {
        host = builder.host;
        port = builder.port;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Settings{");
        sb.append("host='").append(host).append('\'');
        sb.append(", port=").append(port);
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {
        private String host;
        private int port;

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Settings build() {
            checkNotNull(host, "Host is not specified");
            checkArgument(port > 0, "Port is negative");
            return new Settings(this);
        }
    }

}
