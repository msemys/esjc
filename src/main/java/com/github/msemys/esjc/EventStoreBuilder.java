package com.github.msemys.esjc;

import com.github.msemys.esjc.node.cluster.ClusterNodeSettings;
import com.github.msemys.esjc.node.cluster.GossipSeed;
import com.github.msemys.esjc.node.static_.StaticNodeSettings;
import com.github.msemys.esjc.tcp.TcpSettings;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;

public class EventStoreBuilder {
    private final Settings.Builder settingsBuilder = Settings.newBuilder();
    private final TcpSettings.Builder tcpSettingsBuilder = TcpSettings.newBuilder();
    private InetSocketAddress singleNodeAddress;
    private Duration clusterNodeGossipTimeout;
    private Duration clusterNodeDiscoverAttemptInterval;
    private Integer clusterNodeMaxDiscoverAttempts;
    private String clusterNodeDiscoveryFromDns;
    private Integer clusterNodeDiscoveryFromDnsOnGosipPort;
    private List<GossipSeed> clusterNodeDiscoveryFromGossipSeeds;

    private EventStoreBuilder() {
    }

    public static EventStoreBuilder newBuilder() {
        return new EventStoreBuilder();
    }

    public EventStoreBuilder tcpConnectTimeout(Duration duration) {
        tcpSettingsBuilder.connectTimeout(duration);
        return this;
    }

    public EventStoreBuilder tcpCloseTimeout(Duration duration) {
        tcpSettingsBuilder.closeTimeout(duration);
        return this;
    }

    public EventStoreBuilder tcpKeepAliveEnabled() {
        tcpSettingsBuilder.keepAlive(true);
        return this;
    }

    public EventStoreBuilder tcpKeepAliveDisabled() {
        tcpSettingsBuilder.keepAlive(false);
        return this;
    }

    public EventStoreBuilder tcpNoDelayEnabled() {
        tcpSettingsBuilder.tcpNoDelay(true);
        return this;
    }

    public EventStoreBuilder tcpNoDelayDisabled() {
        tcpSettingsBuilder.tcpNoDelay(false);
        return this;
    }

    public EventStoreBuilder tcpSendBufferSize(int size) {
        tcpSettingsBuilder.sendBufferSize(size);
        return this;
    }

    public EventStoreBuilder tcpReceiveBufferSize(int size) {
        tcpSettingsBuilder.receiveBufferSize(size);
        return this;
    }

    public EventStoreBuilder tcpWriteBufferHighWaterMark(int size) {
        tcpSettingsBuilder.writeBufferHighWaterMark(size);
        return this;
    }

    public EventStoreBuilder tcpWriteBufferLowWaterMark(int size) {
        tcpSettingsBuilder.writeBufferLowWaterMark(size);
        return this;
    }

    public EventStoreBuilder singleNodeAddress(InetSocketAddress address) {
        singleNodeAddress = address;
        return this;
    }

    public EventStoreBuilder singleNodeAddress(String host, int port) {
        singleNodeAddress = new InetSocketAddress(host, port);
        return this;
    }

    public EventStoreBuilder clusterNodeGossipTimeout(Duration duration) {
        clusterNodeGossipTimeout = duration;
        return this;
    }

    public EventStoreBuilder clusterNodeDiscoverAttemptInterval(Duration duration) {
        clusterNodeDiscoverAttemptInterval = duration;
        return this;
    }

    public EventStoreBuilder clusterNodeMaxDiscoverAttempts(int count) {
        clusterNodeMaxDiscoverAttempts = count;
        return this;
    }

    public EventStoreBuilder clusterNodeUnlimitedDiscoverAttempts() {
        clusterNodeMaxDiscoverAttempts = -1;
        return this;
    }

    public EventStoreBuilder clusterNodeDiscoveryFromDns(String dns) {
        clusterNodeDiscoveryFromDns = dns;
        return this;
    }

    public EventStoreBuilder clusterNodeDiscoveryFromDnsOnGosipPort(int port) {
        clusterNodeDiscoveryFromDnsOnGosipPort = port;
        return this;
    }

    public EventStoreBuilder clusterNodeDiscoveryFromGossipSeeds(List<InetSocketAddress> endpoints) {
        clusterNodeDiscoveryFromGossipSeeds = endpoints.stream().map(GossipSeed::new).collect(toList());
        return this;
    }

    public EventStoreBuilder sslEnabled() {
        settingsBuilder.ssl(true);
        return this;
    }

    public EventStoreBuilder sslDisabled() {
        settingsBuilder.ssl(false);
        return this;
    }

    public EventStoreBuilder clientReconnectionDelay(Duration duration) {
        settingsBuilder.reconnectionDelay(duration);
        return this;
    }

    public EventStoreBuilder maxClientReconnections(int count) {
        settingsBuilder.maxReconnections(count);
        return this;
    }

    public EventStoreBuilder unlimitedClientReconnections() {
        settingsBuilder.maxReconnections(-1);
        return this;
    }

    public EventStoreBuilder heartbeatInterval(Duration duration) {
        settingsBuilder.heartbeatInterval(duration);
        return this;
    }

    public EventStoreBuilder heartbeatTimeout(Duration duration) {
        settingsBuilder.heartbeatTimeout(duration);
        return this;
    }

    public EventStoreBuilder requireMasterEnabled() {
        settingsBuilder.requireMaster(true);
        return this;
    }

    public EventStoreBuilder requireMasterDisabled() {
        settingsBuilder.requireMaster(false);
        return this;
    }

    public EventStoreBuilder userCredentials(String username, String password) {
        settingsBuilder.userCredentials(username, password);
        return this;
    }

    public EventStoreBuilder operationTimeout(Duration duration) {
        settingsBuilder.operationTimeout(duration);
        return this;
    }

    public EventStoreBuilder operationTimeoutCheckInterval(Duration duration) {
        settingsBuilder.operationTimeoutCheckInterval(duration);
        return this;
    }

    public EventStoreBuilder maxOperationQueueSize(int size) {
        settingsBuilder.maxQueueSize(size);
        return this;
    }

    public EventStoreBuilder maxConcurrentOperations(int count) {
        settingsBuilder.maxConcurrentOperations(count);
        return this;
    }

    public EventStoreBuilder maxOperationRetries(int count) {
        settingsBuilder.maxOperationRetries(count);
        return this;
    }

    public EventStoreBuilder unlimitedOperationRetries() {
        settingsBuilder.maxOperationRetries(-1);
        return this;
    }

    public EventStoreBuilder catchupSubscriptionMaxPushQueueSize(int size) {
        settingsBuilder.maxPushQueueSize(size);
        return this;
    }

    public EventStoreBuilder catchupSubscriptionReadBatchSize(int size) {
        settingsBuilder.readBatchSize(size);
        return this;
    }

    public EventStoreBuilder persistentSubscriptionBufferSize(int size) {
        settingsBuilder.persistentSubscriptionBufferSize(size);
        return this;
    }

    public EventStoreBuilder persistentSubscriptionAutoAckEnabled() {
        settingsBuilder.persistentSubscriptionAutoAckEnabled(true);
        return this;
    }

    public EventStoreBuilder persistentSubscriptionAutoAckDisabled() {
        settingsBuilder.persistentSubscriptionAutoAckEnabled(false);
        return this;
    }

    public EventStoreBuilder failOnNoServerResponseEnabled() {
        settingsBuilder.failOnNoServerResponse(true);
        return this;
    }

    public EventStoreBuilder failOnNoServerResponseDisabled() {
        settingsBuilder.failOnNoServerResponse(false);
        return this;
    }

    public EventStoreBuilder minThreadPoolSize(int size) {
        settingsBuilder.minThreadPoolSize(size);
        return this;
    }

    public EventStoreBuilder maxThreadPoolSize(int size) {
        settingsBuilder.maxThreadPoolSize(size);
        return this;
    }

    public EventStore build() {
        if (singleNodeAddress != null) {
            settingsBuilder.nodeSettings(StaticNodeSettings.newBuilder()
                .address(singleNodeAddress)
                .build());
        }

        if (clusterNodeDiscoveryFromGossipSeeds != null && !clusterNodeDiscoveryFromGossipSeeds.isEmpty()) {
            checkArgument(isNullOrEmpty(clusterNodeDiscoveryFromDns), "Usage of gossip-seed and DNS discoverer at once is not allowed.");

            ClusterNodeSettings.BuilderForGossipSeedDiscoverer clusterNodeSettingsBuilder = ClusterNodeSettings.forGossipSeedDiscoverer();

            clusterNodeSettingsBuilder.gossipSeeds(clusterNodeDiscoveryFromGossipSeeds);
            clusterNodeSettingsBuilder.gossipTimeout(clusterNodeGossipTimeout);
            clusterNodeSettingsBuilder.discoverAttemptInterval(clusterNodeDiscoverAttemptInterval);
            if (clusterNodeMaxDiscoverAttempts != null) {
                clusterNodeSettingsBuilder.maxDiscoverAttempts(clusterNodeMaxDiscoverAttempts);
            }

            settingsBuilder.nodeSettings(clusterNodeSettingsBuilder.build());
        }

        if (!isNullOrEmpty(clusterNodeDiscoveryFromDns)) {
            checkArgument(clusterNodeDiscoveryFromGossipSeeds == null || clusterNodeDiscoveryFromGossipSeeds.isEmpty(),
                "Usage of gossip-seed and DNS discoverer at once is not allowed.");

            ClusterNodeSettings.BuilderForDnsDiscoverer clusterNodeSettingsBuilder = ClusterNodeSettings.forDnsDiscoverer();

            clusterNodeSettingsBuilder.clusterDns(clusterNodeDiscoveryFromDns);
            if (clusterNodeDiscoveryFromDnsOnGosipPort != null) {
                clusterNodeSettingsBuilder.externalGossipPort(clusterNodeDiscoveryFromDnsOnGosipPort);
            }
            clusterNodeSettingsBuilder.gossipTimeout(clusterNodeGossipTimeout);
            clusterNodeSettingsBuilder.discoverAttemptInterval(clusterNodeDiscoverAttemptInterval);
            if (clusterNodeMaxDiscoverAttempts != null) {
                clusterNodeSettingsBuilder.maxDiscoverAttempts(clusterNodeMaxDiscoverAttempts);
            }

            settingsBuilder.nodeSettings(clusterNodeSettingsBuilder.build());
        }

        settingsBuilder.tcpSettings(tcpSettingsBuilder.build());

        return new EventStore(settingsBuilder.build());
    }

}
