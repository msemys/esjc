package lt.msemys.esjc.task;

import lt.msemys.esjc.node.EndPointDiscoverer;

import java.util.concurrent.CompletableFuture;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;

public class StartConnection implements Task {
    public final CompletableFuture<Void> result;
    public final EndPointDiscoverer endPointDiscoverer;

    public StartConnection(CompletableFuture<Void> result, EndPointDiscoverer endPointDiscoverer) {
        checkNotNull(result, "result is null");
        checkNotNull(endPointDiscoverer, "endPointDiscoverer is null");

        this.result = result;
        this.endPointDiscoverer = endPointDiscoverer;
    }
}
