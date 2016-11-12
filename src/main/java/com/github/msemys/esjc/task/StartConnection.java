package com.github.msemys.esjc.task;

import com.github.msemys.esjc.node.EndpointDiscoverer;

import java.util.concurrent.CompletableFuture;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

public class StartConnection implements Task {
    public final CompletableFuture<Void> result;
    public final EndpointDiscoverer endpointDiscoverer;

    public StartConnection(CompletableFuture<Void> result, EndpointDiscoverer endpointDiscoverer) {
        checkNotNull(result, "result is null");
        checkNotNull(endpointDiscoverer, "endpointDiscoverer is null");

        this.result = result;
        this.endpointDiscoverer = endpointDiscoverer;
    }
}
