package com.github.msemys.esjc;

import com.github.msemys.esjc.node.cluster.ClusterNodeSettings;
import com.github.msemys.esjc.node.cluster.GossipSeed;
import com.github.msemys.esjc.node.single.SingleNodeSettings;
import com.github.msemys.esjc.ssl.SslSettings;
import com.github.msemys.esjc.tcp.TcpSettings;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class EventStoreBuilderTest {

    @Test
    public void createsSingleNodeClientFromSettings() {
        Settings settings = Settings.newBuilder()
            .connectionName("test")
            .nodeSettings(SingleNodeSettings.newBuilder()
                .address("localhost", 1010)
                .build())
            .userCredentials("username", "password")
            .tcpSettings(TcpSettings.newBuilder()
                .connectTimeout(Duration.ofSeconds(11))
                .closeTimeout(Duration.ofSeconds(22))
                .keepAlive(false)
                .noDelay(false)
                .sendBufferSize(1111)
                .receiveBufferSize(2222)
                .writeBufferHighWaterMark(4444)
                .writeBufferLowWaterMark(3333)
                .build())
            .sslSettings(SslSettings.trustCertificateCN("dummy"))
            .reconnectionDelay(Duration.ofSeconds(33))
            .heartbeatInterval(Duration.ofSeconds(44))
            .heartbeatTimeout(Duration.ofSeconds(55))
            .requireMaster(false)
            .operationTimeout(Duration.ofMinutes(1))
            .operationTimeoutCheckInterval(Duration.ofMinutes(2))
            .maxOperationQueueSize(100)
            .maxConcurrentOperations(200)
            .maxOperationRetries(300)
            .maxReconnections(400)
            .persistentSubscriptionBufferSize(5555)
            .persistentSubscriptionAutoAck(false)
            .failOnNoServerResponse(true)
            .executor(Executors.newCachedThreadPool())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings).build();

        assertEquals(settings.toString(), result.settings().toString());
    }

    @Test
    public void createsClusterNodeClientFromSettings() {
        Settings settings = Settings.newBuilder()
            .nodeSettings(ClusterNodeSettings.forGossipSeedDiscoverer()
                .gossipSeedEndpoints(asList(
                    new InetSocketAddress("localhost", 1001),
                    new InetSocketAddress("localhost", 1002),
                    new InetSocketAddress("localhost", 1003)))
                .gossipTimeout(Duration.ofSeconds(60))
                .discoverAttemptInterval(Duration.ofMinutes(2))
                .maxDiscoverAttempts(5)
                .preferRandomNode(true)
                .build())
            .userCredentials("username", "password")
            .tcpSettings(TcpSettings.newBuilder()
                .connectTimeout(Duration.ofSeconds(11))
                .closeTimeout(Duration.ofSeconds(22))
                .keepAlive(false)
                .noDelay(false)
                .sendBufferSize(1111)
                .receiveBufferSize(2222)
                .writeBufferHighWaterMark(4444)
                .writeBufferLowWaterMark(3333)
                .build())
            .sslSettings(SslSettings.trustAllCertificates())
            .reconnectionDelay(Duration.ofSeconds(33))
            .heartbeatInterval(Duration.ofSeconds(44))
            .heartbeatTimeout(Duration.ofSeconds(55))
            .requireMaster(false)
            .operationTimeout(Duration.ofMinutes(1))
            .operationTimeoutCheckInterval(Duration.ofMinutes(2))
            .maxOperationQueueSize(100)
            .maxConcurrentOperations(200)
            .maxOperationRetries(300)
            .maxReconnections(400)
            .persistentSubscriptionBufferSize(5555)
            .persistentSubscriptionAutoAck(false)
            .failOnNoServerResponse(true)
            .executor(Executors.newCachedThreadPool())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings).build();

        assertEquals(settings.toString(), result.settings().toString());
    }

    @Test
    public void createsClientWithoutUserCredentialsFromSettings() {
        Settings settings = Settings.newBuilder()
            .nodeSettings(SingleNodeSettings.newBuilder()
                .address("localhost", 1010)
                .build())
            .tcpSettings(TcpSettings.newBuilder()
                .connectTimeout(Duration.ofSeconds(11))
                .closeTimeout(Duration.ofSeconds(22))
                .keepAlive(false)
                .noDelay(false)
                .sendBufferSize(1111)
                .receiveBufferSize(2222)
                .writeBufferHighWaterMark(4444)
                .writeBufferLowWaterMark(3333)
                .build())
            .sslSettings(SslSettings.noSsl())
            .reconnectionDelay(Duration.ofSeconds(33))
            .heartbeatInterval(Duration.ofSeconds(44))
            .heartbeatTimeout(Duration.ofSeconds(55))
            .requireMaster(false)
            .operationTimeout(Duration.ofMinutes(1))
            .operationTimeoutCheckInterval(Duration.ofMinutes(2))
            .maxOperationQueueSize(100)
            .maxConcurrentOperations(200)
            .maxOperationRetries(300)
            .maxReconnections(400)
            .persistentSubscriptionBufferSize(5555)
            .persistentSubscriptionAutoAck(false)
            .failOnNoServerResponse(true)
            .executor(Executors.newCachedThreadPool())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings).build();

        assertEquals(settings.toString(), result.settings().toString());
        assertNull(result.settings().userCredentials);
    }

    @Test
    public void createsCustomizedClientWithDisabledConnectionEncryptionFromSettings() {
        Settings settings = Settings.newBuilder()
            .nodeSettings(SingleNodeSettings.newBuilder().address("localhost", 1010).build())
            .sslSettings(SslSettings.trustAllCertificates())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings)
            .noSslConnection()
            .build();

        assertFalse(result.settings().sslSettings.useSslConnection);
    }

    @Test
    public void createsCustomizedClientFromSettings() {
        Settings settings = Settings.newBuilder()
            .nodeSettings(SingleNodeSettings.newBuilder()
                .address("localhost", 1010)
                .build())
            .userCredentials("username", "password")
            .tcpSettings(TcpSettings.newBuilder()
                .connectTimeout(Duration.ofSeconds(11))
                .closeTimeout(Duration.ofSeconds(22))
                .keepAlive(false)
                .noDelay(false)
                .sendBufferSize(1111)
                .receiveBufferSize(2222)
                .writeBufferHighWaterMark(4444)
                .writeBufferLowWaterMark(3333)
                .build())
            .sslSettings(SslSettings.noSsl())
            .reconnectionDelay(Duration.ofSeconds(33))
            .heartbeatInterval(Duration.ofSeconds(44))
            .heartbeatTimeout(Duration.ofSeconds(55))
            .requireMaster(false)
            .operationTimeout(Duration.ofMinutes(1))
            .operationTimeoutCheckInterval(Duration.ofMinutes(2))
            .maxOperationQueueSize(100)
            .maxConcurrentOperations(200)
            .maxOperationRetries(300)
            .maxReconnections(400)
            .persistentSubscriptionBufferSize(5555)
            .persistentSubscriptionAutoAck(false)
            .failOnNoServerResponse(false)
            .executor(Executors.newCachedThreadPool())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings)
            .connectionName("test")
            .singleNodeAddress("localhost", 2020)
            .userCredentials("usr", "psw")
            .tcpSettings(tcp -> tcp.keepAlive(true).noDelay(true).sendBufferSize(11110))
            .useSslConnection()
            .requireMaster(true)
            .failOnNoServerResponse(true)
            .build();

        assertEquals("test", result.settings().connectionName);
        assertEquals(2020, result.settings().singleNodeSettings.address.getPort());
        assertEquals("usr", result.settings().userCredentials.username);
        assertEquals("psw", result.settings().userCredentials.password);
        assertTrue(result.settings().tcpSettings.keepAlive);
        assertTrue(result.settings().tcpSettings.noDelay);
        assertEquals(11110, result.settings().tcpSettings.sendBufferSize);
        assertTrue(result.settings().sslSettings.useSslConnection);
        assertTrue(result.settings().requireMaster);
        assertTrue(result.settings().failOnNoServerResponse);
    }

    @Test
    public void createsCustomizedClientWithoutUserCredentialsFromSettings() {
        Settings settings = Settings.newBuilder()
            .connectionName("test")
            .nodeSettings(SingleNodeSettings.newBuilder()
                .address("localhost", 1010)
                .build())
            .userCredentials("username", "password")
            .tcpSettings(TcpSettings.newBuilder()
                .connectTimeout(Duration.ofSeconds(11))
                .closeTimeout(Duration.ofSeconds(22))
                .keepAlive(false)
                .noDelay(false)
                .sendBufferSize(1111)
                .receiveBufferSize(2222)
                .writeBufferHighWaterMark(4444)
                .writeBufferLowWaterMark(3333)
                .build())
            .sslSettings(SslSettings.noSsl())
            .reconnectionDelay(Duration.ofSeconds(33))
            .heartbeatInterval(Duration.ofSeconds(44))
            .heartbeatTimeout(Duration.ofSeconds(55))
            .requireMaster(false)
            .operationTimeout(Duration.ofMinutes(1))
            .operationTimeoutCheckInterval(Duration.ofMinutes(2))
            .maxOperationQueueSize(100)
            .maxConcurrentOperations(200)
            .maxOperationRetries(300)
            .maxReconnections(400)
            .persistentSubscriptionBufferSize(5555)
            .persistentSubscriptionAutoAck(false)
            .failOnNoServerResponse(false)
            .executor(Executors.newCachedThreadPool())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings)
            .singleNodeAddress("localhost", 2020)
            .noUserCredentials()
            .tcpSettings(tcp -> tcp.keepAlive(true).noDelay(true).sendBufferSize(11110))
            .useSslConnection()
            .requireMaster(true)
            .failOnNoServerResponse(true)
            .build();

        assertEquals("test", result.settings().connectionName);
        assertEquals(2020, result.settings().singleNodeSettings.address.getPort());
        assertNull(result.settings().userCredentials);
        assertTrue(result.settings().tcpSettings.keepAlive);
        assertTrue(result.settings().tcpSettings.noDelay);
        assertEquals(11110, result.settings().tcpSettings.sendBufferSize);
        assertTrue(result.settings().sslSettings.useSslConnection);
        assertTrue(result.settings().requireMaster);
        assertTrue(result.settings().failOnNoServerResponse);
    }

    @Test
    public void createsCustomizedClusterNodeUsingGossipSeedsClientFromSettings() {
        Settings settings = Settings.newBuilder()
            .nodeSettings(ClusterNodeSettings.forGossipSeedDiscoverer()
                .gossipSeedEndpoints(asList(
                    new InetSocketAddress("localhost", 1001),
                    new InetSocketAddress("localhost", 1002),
                    new InetSocketAddress("localhost", 1003)))
                .gossipTimeout(Duration.ofSeconds(60))
                .discoverAttemptInterval(Duration.ofMinutes(2))
                .maxDiscoverAttempts(5)
                .build())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings)
            .clusterNodeUsingGossipSeeds(cluster -> cluster
                .gossipTimeout(Duration.ofSeconds(120))
                .discoverAttemptInterval(Duration.ofMinutes(4))
                .maxDiscoverAttempts(10)
                .preferRandomNode(true))
            .build();

        assertNull(result.settings().singleNodeSettings);
        assertNotNull(result.settings().clusterNodeSettings);
        assertEquals(10, result.settings().clusterNodeSettings.maxDiscoverAttempts);
        assertEquals(Duration.ofMinutes(4), result.settings().clusterNodeSettings.discoverAttemptInterval);
        assertThat(result.settings().clusterNodeSettings.gossipSeeds.stream()
                .map(GossipSeed::toString)
                .collect(toList()),
            hasItems(
                new GossipSeed(new InetSocketAddress("localhost", 1001)).toString(),
                new GossipSeed(new InetSocketAddress("localhost", 1002)).toString(),
                new GossipSeed(new InetSocketAddress("localhost", 1003)).toString()));
        assertEquals(Duration.ofSeconds(120), result.settings().clusterNodeSettings.gossipTimeout);
        assertTrue(result.settings().clusterNodeSettings.preferRandomNode);
    }

    @Test
    public void createsCustomizedClusterNodeUsingDnsClientFromSettings() {
        Settings settings = Settings.newBuilder()
            .nodeSettings(ClusterNodeSettings.forDnsDiscoverer()
                .dns("dns1")
                .externalGossipPort(1234)
                .gossipTimeout(Duration.ofSeconds(60))
                .discoverAttemptInterval(Duration.ofMinutes(2))
                .maxDiscoverAttempts(5)
                .build())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings)
            .clusterNodeUsingDns(cluster -> cluster
                .dns("dns2")
                .gossipTimeout(Duration.ofSeconds(120))
                .discoverAttemptInterval(Duration.ofMinutes(4))
                .maxDiscoverAttempts(10)
                .preferRandomNode(true))
            .build();

        assertNull(result.settings().singleNodeSettings);
        assertNotNull(result.settings().clusterNodeSettings);
        assertEquals("dns2", result.settings().clusterNodeSettings.dns);
        assertEquals(1234, result.settings().clusterNodeSettings.externalGossipPort);
        assertEquals(10, result.settings().clusterNodeSettings.maxDiscoverAttempts);
        assertEquals(Duration.ofMinutes(4), result.settings().clusterNodeSettings.discoverAttemptInterval);
        assertTrue(result.settings().clusterNodeSettings.gossipSeeds.isEmpty());
        assertEquals(Duration.ofSeconds(120), result.settings().clusterNodeSettings.gossipTimeout);
        assertTrue(result.settings().clusterNodeSettings.preferRandomNode);
    }

    @Test
    public void createsSingleNodeClientFromSettingsWithClusterNode() {
        Settings settings = Settings.newBuilder()
            .nodeSettings(ClusterNodeSettings.forDnsDiscoverer()
                .dns("dns1")
                .externalGossipPort(1234)
                .gossipTimeout(Duration.ofSeconds(60))
                .discoverAttemptInterval(Duration.ofMinutes(2))
                .maxDiscoverAttempts(5)
                .build())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings)
            .singleNodeAddress("localhost", 1009)
            .build();

        assertNotNull(result.settings().singleNodeSettings);
        assertNull(result.settings().clusterNodeSettings);
        assertEquals("localhost", result.settings().singleNodeSettings.address.getHostName());
        assertEquals(1009, result.settings().singleNodeSettings.address.getPort());
    }

    @Test
    public void createsClusterNodeUsingGossipSeedsClientFromSettingsWithSingleNode() {
        Settings settings = Settings.newBuilder()
            .nodeSettings(SingleNodeSettings.newBuilder().address("localhost", 1001).build())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings)
            .clusterNodeUsingGossipSeeds(cluster -> cluster
                .maxDiscoverAttempts(10)
                .discoverAttemptInterval(Duration.ofMinutes(4))
                .gossipSeedEndpoints(asList(
                    new InetSocketAddress("localhost", 1001),
                    new InetSocketAddress("localhost", 1002),
                    new InetSocketAddress("localhost", 1003)))
                .gossipTimeout(Duration.ofSeconds(120)))
            .build();

        assertNull(result.settings().singleNodeSettings);
        assertNotNull(result.settings().clusterNodeSettings);
        assertEquals(10, result.settings().clusterNodeSettings.maxDiscoverAttempts);
        assertEquals(Duration.ofMinutes(4), result.settings().clusterNodeSettings.discoverAttemptInterval);
        assertThat(result.settings().clusterNodeSettings.gossipSeeds.stream()
                .map(GossipSeed::toString)
                .collect(toList()),
            hasItems(
                new GossipSeed(new InetSocketAddress("localhost", 1001)).toString(),
                new GossipSeed(new InetSocketAddress("localhost", 1002)).toString(),
                new GossipSeed(new InetSocketAddress("localhost", 1003)).toString()));
        assertEquals(Duration.ofSeconds(120), result.settings().clusterNodeSettings.gossipTimeout);
    }

    @Test
    public void createsClusterNodeUsingDnsClientFromSettingsWithSingleNode() {
        Settings settings = Settings.newBuilder()
            .nodeSettings(SingleNodeSettings.newBuilder().address("localhost", 1001).build())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings)
            .clusterNodeUsingDns(cluster -> cluster
                .dns("dns")
                .externalGossipPort(1234)
                .gossipTimeout(Duration.ofSeconds(120))
                .discoverAttemptInterval(Duration.ofMinutes(4))
                .maxDiscoverAttempts(10))
            .build();

        assertNull(result.settings().singleNodeSettings);
        assertNotNull(result.settings().clusterNodeSettings);
        assertEquals("dns", result.settings().clusterNodeSettings.dns);
        assertEquals(1234, result.settings().clusterNodeSettings.externalGossipPort);
        assertEquals(10, result.settings().clusterNodeSettings.maxDiscoverAttempts);
        assertEquals(Duration.ofMinutes(4), result.settings().clusterNodeSettings.discoverAttemptInterval);
        assertTrue(result.settings().clusterNodeSettings.gossipSeeds.isEmpty());
        assertEquals(Duration.ofSeconds(120), result.settings().clusterNodeSettings.gossipTimeout);
    }

    @Test
    public void createsSingleNodeClient() {
        EventStore result = EventStoreBuilder.newBuilder()
            .singleNodeAddress("localhost", 1009)
            .build();

        assertNotNull(result.settings().singleNodeSettings);
        assertNull(result.settings().clusterNodeSettings);
        assertEquals("localhost", result.settings().singleNodeSettings.address.getHostName());
        assertEquals(1009, result.settings().singleNodeSettings.address.getPort());
    }

    @Test
    public void createsClusterNodeUsingGossipSeedsClient() {
        EventStore result = EventStoreBuilder.newBuilder()
            .clusterNodeUsingGossipSeeds(cluster -> cluster
                .maxDiscoverAttempts(-1)
                .discoverAttemptInterval(Duration.ofMinutes(5))
                .gossipSeedEndpoints(asList(
                    new InetSocketAddress("localhost", 1001),
                    new InetSocketAddress("localhost", 1002),
                    new InetSocketAddress("localhost", 1003)))
                .gossipTimeout(Duration.ofSeconds(73)))
            .build();

        assertNull(result.settings().singleNodeSettings);
        assertNotNull(result.settings().clusterNodeSettings);
        assertEquals("", result.settings().clusterNodeSettings.dns);
        assertEquals(-1, result.settings().clusterNodeSettings.maxDiscoverAttempts);
        assertEquals(Duration.ofMinutes(5), result.settings().clusterNodeSettings.discoverAttemptInterval);
        assertEquals(0, result.settings().clusterNodeSettings.externalGossipPort);
        assertThat(result.settings().clusterNodeSettings.gossipSeeds.stream()
                .map(GossipSeed::toString)
                .collect(toList()),
            hasItems(
                new GossipSeed(new InetSocketAddress("localhost", 1001)).toString(),
                new GossipSeed(new InetSocketAddress("localhost", 1002)).toString(),
                new GossipSeed(new InetSocketAddress("localhost", 1003)).toString()));
        assertEquals(Duration.ofSeconds(73), result.settings().clusterNodeSettings.gossipTimeout);
        assertFalse(result.settings().clusterNodeSettings.preferRandomNode);
    }

    @Test
    public void createsClusterNodeUsingDnsClient() {
        EventStore result = EventStoreBuilder.newBuilder()
            .clusterNodeUsingDns(cluster -> cluster
                .dns("dns")
                .maxDiscoverAttempts(3)
                .discoverAttemptInterval(Duration.ofMinutes(6))
                .externalGossipPort(1717)
                .gossipTimeout(Duration.ofSeconds(83)))
            .build();

        assertNull(result.settings().singleNodeSettings);
        assertNotNull(result.settings().clusterNodeSettings);
        assertEquals("dns", result.settings().clusterNodeSettings.dns);
        assertEquals(3, result.settings().clusterNodeSettings.maxDiscoverAttempts);
        assertEquals(Duration.ofMinutes(6), result.settings().clusterNodeSettings.discoverAttemptInterval);
        assertEquals(1717, result.settings().clusterNodeSettings.externalGossipPort);
        assertTrue(result.settings().clusterNodeSettings.gossipSeeds.isEmpty());
        assertEquals(Duration.ofSeconds(83), result.settings().clusterNodeSettings.gossipTimeout);
        assertFalse(result.settings().clusterNodeSettings.preferRandomNode);
    }

    @Test
    public void createsClientWithCustomTcpSettings() {
        EventStore result = EventStoreBuilder.newBuilder()
            .singleNodeAddress("localhost", 1009)
            .tcpSettings(tcp -> tcp
                .closeTimeout(Duration.ofSeconds(100))
                .connectTimeout(Duration.ofSeconds(200))
                .keepAlive(false)
                .noDelay(false)
                .sendBufferSize(1)
                .receiveBufferSize(2)
                .writeBufferLowWaterMark(3)
                .writeBufferHighWaterMark(4))
            .build();

        assertEquals(Duration.ofSeconds(100), result.settings().tcpSettings.closeTimeout);
        assertEquals(Duration.ofSeconds(200), result.settings().tcpSettings.connectTimeout);
        assertFalse(result.settings().tcpSettings.keepAlive);
        assertFalse(result.settings().tcpSettings.noDelay);
        assertEquals(1, result.settings().tcpSettings.sendBufferSize);
        assertEquals(2, result.settings().tcpSettings.receiveBufferSize);
        assertEquals(3, result.settings().tcpSettings.writeBufferLowWaterMark);
        assertEquals(4, result.settings().tcpSettings.writeBufferHighWaterMark);
    }

    @Test
    public void createsClientWithUnlimitedAttemptsValues() {
        EventStore result = EventStoreBuilder.newBuilder()
            .clusterNodeUsingGossipSeeds(cluster -> cluster
                .gossipSeedEndpoints(asList(new InetSocketAddress("localhost", 1001)))
                .maxDiscoverAttempts(-1))
            .maxOperationRetries(-1)
            .maxReconnections(-1)
            .build();

        assertEquals(-1, result.settings().clusterNodeSettings.maxDiscoverAttempts);
        assertEquals(-1, result.settings().maxOperationRetries);
        assertEquals(-1, result.settings().maxReconnections);
    }

    @Test
    public void createsClientWithInsaneAttemptsValues() {
        EventStore result = EventStoreBuilder.newBuilder()
            .clusterNodeUsingGossipSeeds(cluster -> cluster
                .gossipSeedEndpoints(asList(new InetSocketAddress("localhost", 1001)))
                .maxDiscoverAttempts(Integer.MAX_VALUE))
            .maxOperationRetries(Integer.MAX_VALUE)
            .maxReconnections(Integer.MAX_VALUE)
            .build();

        assertEquals(Integer.MAX_VALUE, result.settings().clusterNodeSettings.maxDiscoverAttempts);
        assertEquals(Integer.MAX_VALUE, result.settings().maxOperationRetries);
        assertEquals(Integer.MAX_VALUE, result.settings().maxReconnections);
    }

    @Test
    public void createsClientWithDefaultConnectionName() {
        EventStore result = EventStoreBuilder.newBuilder()
            .singleNodeAddress("localhost", 1009)
            .build();

        assertTrue(result.settings().connectionName.startsWith("ESJC-"));
    }

    @Test
    public void createsClientWithCustomConnectionName() {
        EventStore result = EventStoreBuilder.newBuilder()
            .connectionName("test")
            .singleNodeAddress("localhost", 1009)
            .build();

        assertEquals("test", result.settings().connectionName);
    }

    @Test
    public void failsToCreateClientWithoutNodeSettings() {
        try {
            EventStoreBuilder.newBuilder().build();
            fail("should fail with 'IllegalArgumentException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertEquals("Missing node settings", e.getMessage());
        }
    }

    @Test
    public void failsToCreateClusterNodeUsingGossipSeedsClientWithoutGossipSeeds() {
        try {
            EventStoreBuilder.newBuilder()
                .clusterNodeUsingGossipSeeds(cluster -> cluster)
                .build();
            fail("should fail with 'IllegalArgumentException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertEquals("Gossip seeds are not specified.", e.getMessage());
        }
    }

    @Test
    public void failsToCreateClusterNodeUsingDnsClientWithoutDns() {
        try {
            EventStoreBuilder.newBuilder()
                .clusterNodeUsingDns(cluster -> cluster)
                .build();
            fail("should fail with 'IllegalArgumentException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertEquals("dns is null or empty", e.getMessage());
        }
    }

    @Test
    public void failsToCreateClusterNodeClientWithTooSmallMaxDiscoverAttemptsValue() {
        try {
            EventStoreBuilder.newBuilder()
                .clusterNodeUsingDns(cluster -> cluster.dns("ok").maxDiscoverAttempts(-2))
                .build();
            fail("should fail with 'IllegalArgumentException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertEquals("maxDiscoverAttempts value is out of range. Allowed range: [-1..infinity].", e.getMessage());
        }
    }

    @Test
    public void failsToCreateClientWithTooSmallMaxReconnectionsValue() {
        try {
            EventStoreBuilder.newBuilder()
                .singleNodeAddress("localhost", 1009)
                .maxReconnections(-2)
                .build();
            fail("should fail with 'IllegalArgumentException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertEquals("maxReconnections value is out of range. Allowed range: [-1..infinity].", e.getMessage());
        }
    }

    @Test
    public void failsToCreateClientWithTooSmallMaxOperationRetriesValue() {
        try {
            EventStoreBuilder.newBuilder()
                .singleNodeAddress("localhost", 1009)
                .maxOperationRetries(-2)
                .build();
            fail("should fail with 'IllegalArgumentException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertEquals("maxOperationRetries value is out of range. Allowed range: [-1..infinity].", e.getMessage());
        }
    }
}
