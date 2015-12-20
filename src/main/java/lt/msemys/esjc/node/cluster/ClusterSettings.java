package lt.msemys.esjc.node.cluster;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static lt.msemys.esjc.util.Preconditions.checkArgument;

public class ClusterSettings {
    public final String clusterDns;
    public final int maxDiscoverAttempts;
    public final int externalGossipPort;
    public final List<GossipSeed> gossipSeeds;
    public final Duration gossipTimeout;

    private ClusterSettings(Builder builder) {
        clusterDns = builder.clusterDns;
        maxDiscoverAttempts = builder.maxDiscoverAttempts;
        externalGossipPort = builder.externalGossipPort;
        gossipSeeds = builder.gossipSeeds;
        gossipTimeout = builder.gossipTimeout;
    }

    public static BuilderForGossipSeedDiscoverer forGossipSeedDiscoverer() {
        return new BuilderForGossipSeedDiscoverer();
    }

    public static BuilderForDnsDiscoverer forDnsDiscoverer() {
        return new BuilderForDnsDiscoverer();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ClusterSettings{");
        sb.append("clusterDns='").append(clusterDns).append('\'');
        sb.append(", maxDiscoverAttempts=").append(maxDiscoverAttempts);
        sb.append(", externalGossipPort=").append(externalGossipPort);
        sb.append(", gossipSeeds=").append(gossipSeeds);
        sb.append(", gossipTimeout=").append(gossipTimeout);
        sb.append('}');
        return sb.toString();
    }


    /**
     * Builder for gossip seed discoverer
     */
    public static class BuilderForGossipSeedDiscoverer extends Builder {

        public BuilderForGossipSeedDiscoverer maxDiscoverAttempts(int maxDiscoverAttempts) {
            super.maxDiscoverAttempts = maxDiscoverAttempts;
            return this;
        }

        public BuilderForGossipSeedDiscoverer gossipSeedEndPoints(List<InetSocketAddress> endpoints) {
            return gossipSeeds(endpoints.stream().map(GossipSeed::new).collect(toList()));
        }

        public BuilderForGossipSeedDiscoverer gossipSeeds(List<GossipSeed> gossipSeeds) {
            super.gossipSeeds = gossipSeeds;
            return this;
        }

        public BuilderForGossipSeedDiscoverer gossipTimeout(Duration gossipTimeout) {
            super.gossipTimeout = gossipTimeout;
            return this;
        }

        @Override
        public ClusterSettings build() {
            checkArgument(super.gossipSeeds != null && !super.gossipSeeds.isEmpty(), "Empty FakeDnsEntries collection.");
            return super.build();
        }
    }

    /**
     * Builder for DNS discoverer
     */
    public static class BuilderForDnsDiscoverer extends Builder {

        public BuilderForDnsDiscoverer clusterDns(String clusterDns) {
            super.clusterDns = clusterDns;
            return this;
        }

        public BuilderForDnsDiscoverer maxDiscoverAttempts(int maxDiscoverAttempts) {
            super.maxDiscoverAttempts = maxDiscoverAttempts;
            return this;
        }

        public BuilderForDnsDiscoverer externalGossipPort(int externalGossipPort) {
            super.externalGossipPort = externalGossipPort;
            return this;
        }

        public BuilderForDnsDiscoverer gossipTimeout(Duration gossipTimeout) {
            super.gossipTimeout = gossipTimeout;
            return this;
        }

        @Override
        public ClusterSettings build() {
            checkArgument(super.clusterDns != null && !super.clusterDns.isEmpty(), "clusterDns is empty");

            if (super.externalGossipPort == null) {
                super.externalGossipPort = 30778;
            }

            return super.build();
        }
    }

    /**
     * Base builder
     */
    private static class Builder {
        private String clusterDns;
        private Integer maxDiscoverAttempts;
        private Integer externalGossipPort;
        private List<GossipSeed> gossipSeeds;
        private Duration gossipTimeout;

        public ClusterSettings build() {
            if (clusterDns == null) {
                clusterDns = "";
            }

            if (maxDiscoverAttempts == null) {
                maxDiscoverAttempts = 10;
            } else {
                checkArgument(maxDiscoverAttempts >= -1, "maxDiscoverAttempts value is out of range: %d. Allowed range: [-1, infinity].", maxDiscoverAttempts);
            }

            if (externalGossipPort == null) {
                externalGossipPort = 0;
            } else {
                checkArgument(externalGossipPort > 0, "externalGossipPort should be positive");
            }

            if (gossipSeeds == null) {
                gossipSeeds = emptyList();
            } else {
                gossipSeeds = unmodifiableList(gossipSeeds);
            }

            if (gossipTimeout == null) {
                gossipTimeout = Duration.ofSeconds(1);
            }

            return new ClusterSettings(this);
        }
    }
}
