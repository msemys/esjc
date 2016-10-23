package com.github.msemys.esjc;

import com.github.msemys.esjc.node.cluster.ClusterNodeSettings;
import com.github.msemys.esjc.node.static_.StaticNodeSettings;
import com.github.msemys.esjc.ssl.SslSettings;
import com.github.msemys.esjc.tcp.TcpSettings;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class EventStoreBuilderTest {

    @Test
    public void createsSingleNodeClientFromSettings() {
        Settings settings = Settings.newBuilder()
            .nodeSettings(StaticNodeSettings.newBuilder()
                .address("localhost", 1010)
                .build())
            .userCredentials("username", "password")
            .tcpSettings(TcpSettings.newBuilder()
                .connectTimeout(Duration.ofSeconds(11))
                .closeTimeout(Duration.ofSeconds(22))
                .keepAlive(false)
                .tcpNoDelay(false)
                .sendBufferSize(1111)
                .receiveBufferSize(2222)
                .writeBufferHighWaterMark(3333)
                .writeBufferLowWaterMark(4444)
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
            .persistentSubscriptionAutoAckEnabled(false)
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
                .build())
            .userCredentials("username", "password")
            .tcpSettings(TcpSettings.newBuilder()
                .connectTimeout(Duration.ofSeconds(11))
                .closeTimeout(Duration.ofSeconds(22))
                .keepAlive(false)
                .tcpNoDelay(false)
                .sendBufferSize(1111)
                .receiveBufferSize(2222)
                .writeBufferHighWaterMark(3333)
                .writeBufferLowWaterMark(4444)
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
            .persistentSubscriptionAutoAckEnabled(false)
            .failOnNoServerResponse(true)
            .executor(Executors.newCachedThreadPool())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings).build();

        assertEquals(settings.toString(), result.settings().toString());
    }

    @Test
    public void createsClientWithoutUserCredentialsFromSettings() {
        Settings settings = Settings.newBuilder()
            .nodeSettings(StaticNodeSettings.newBuilder()
                .address("localhost", 1010)
                .build())
            .tcpSettings(TcpSettings.newBuilder()
                .connectTimeout(Duration.ofSeconds(11))
                .closeTimeout(Duration.ofSeconds(22))
                .keepAlive(false)
                .tcpNoDelay(false)
                .sendBufferSize(1111)
                .receiveBufferSize(2222)
                .writeBufferHighWaterMark(3333)
                .writeBufferLowWaterMark(4444)
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
            .persistentSubscriptionAutoAckEnabled(false)
            .failOnNoServerResponse(true)
            .executor(Executors.newCachedThreadPool())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings).build();

        assertEquals(settings.toString(), result.settings().toString());
        assertFalse(result.settings().userCredentials.isPresent());
    }

    @Test
    public void createsCustomizedClientFromSettings() {
        Settings settings = Settings.newBuilder()
            .nodeSettings(StaticNodeSettings.newBuilder()
                .address("localhost", 1010)
                .build())
            .userCredentials("username", "password")
            .tcpSettings(TcpSettings.newBuilder()
                .connectTimeout(Duration.ofSeconds(11))
                .closeTimeout(Duration.ofSeconds(22))
                .keepAlive(false)
                .tcpNoDelay(false)
                .sendBufferSize(1111)
                .receiveBufferSize(2222)
                .writeBufferHighWaterMark(3333)
                .writeBufferLowWaterMark(4444)
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
            .persistentSubscriptionAutoAckEnabled(false)
            .failOnNoServerResponse(false)
            .executor(Executors.newCachedThreadPool())
            .build();

        EventStore result = EventStoreBuilder.newBuilder(settings)
            .singleNodeAddress("localhost", 2020)
            .userCredentials("usr", "psw")
            .tcpKeepAliveEnabled()
            .tcpNoDelayEnabled()
            .tcpSendBufferSize(11110)
            .useSslConnection()
            .requireMasterEnabled()
            .failOnNoServerResponseEnabled()
            .build();

        assertEquals(2020, result.settings().staticNodeSettings.get().address.getPort());
        assertEquals("usr", result.settings().userCredentials.get().username);
        assertEquals("psw", result.settings().userCredentials.get().password);
        assertTrue(result.settings().tcpSettings.keepAlive);
        assertTrue(result.settings().tcpSettings.tcpNoDelay);
        assertEquals(11110, result.settings().tcpSettings.sendBufferSize);
        assertTrue(result.settings().sslSettings.useSslConnection);
        assertTrue(result.settings().requireMaster);
        assertTrue(result.settings().failOnNoServerResponse);
    }

}
