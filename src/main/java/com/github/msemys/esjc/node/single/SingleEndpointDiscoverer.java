package com.github.msemys.esjc.node.single;

import com.github.msemys.esjc.node.EndpointDiscoverer;
import com.github.msemys.esjc.node.NodeEndpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static io.netty.util.NetUtil.createByteArrayFromIpAddressString;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class SingleEndpointDiscoverer implements EndpointDiscoverer {
    private static final Logger logger = LoggerFactory.getLogger(SingleEndpointDiscoverer.class);

    private final String hostname;
    private final InetAddress ipAddress;
    private final int port;
    private final boolean ssl;

    public SingleEndpointDiscoverer(SingleNodeSettings settings, boolean ssl) {
        checkNotNull(settings, "settings is null");

        hostname = settings.address.getHostString();
        ipAddress = maybeIpAddress(hostname);
        port = settings.address.getPort();
        this.ssl = ssl;
    }

    @Override
    public CompletableFuture<NodeEndpoints> discover(InetSocketAddress failedTcpEndpoint) {
        InetSocketAddress address = (ipAddress != null) ?
            new InetSocketAddress(ipAddress, port) :
            new InetSocketAddress(hostname, port);

        return completedFuture(new NodeEndpoints(
            ssl ? null : address,
            ssl ? address : null));
    }

    private static InetAddress maybeIpAddress(String address) {
        byte[] ipAddressBytes = createByteArrayFromIpAddressString(address);

        if (ipAddressBytes != null) {
            try {
                return InetAddress.getByAddress(ipAddressBytes);
            } catch (UnknownHostException e) {
                logger.warn("Unable to resolve IP address by '{}'", address, e);
            }
        }

        return null;
    }

}
