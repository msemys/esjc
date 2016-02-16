package com.github.msemys.esjc.node;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public interface EndpointDiscoverer {

    CompletableFuture<NodeEndpoints> discover(InetSocketAddress failedTcpEndpoint);

}
