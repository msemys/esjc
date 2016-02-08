package lt.msemys.esjc.task;

import lt.msemys.esjc.node.EndpointDiscoverer;

import java.util.concurrent.CompletableFuture;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;

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
