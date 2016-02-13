package lt.msemys.esjc.node.cluster;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.stream.JsonReader;
import lt.msemys.esjc.node.EndpointDiscoverer;
import lt.msemys.esjc.node.NodeEndpoints;
import lt.msemys.esjc.util.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static lt.msemys.esjc.util.Preconditions.checkNotNull;

public class ClusterDnsEndpointDiscoverer implements EndpointDiscoverer {
    private static final Logger logger = LoggerFactory.getLogger(ClusterDnsEndpointDiscoverer.class);

    private final ScheduledExecutorService scheduler;
    private List<MemberInfoDto> oldGossip;
    private final ReentrantLock oldGossipLock = new ReentrantLock();
    private final ClusterNodeSettings settings;
    private final Gson gson;

    public ClusterDnsEndpointDiscoverer(ClusterNodeSettings settings, ScheduledExecutorService scheduler) {
        checkNotNull(settings, "settings");
        checkNotNull(scheduler, "scheduler");

        this.settings = settings;
        this.scheduler = scheduler;

        gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class,
                (JsonDeserializer<Instant>) (json, type, ctx) -> Instant.parse(json.getAsJsonPrimitive().getAsString()))
            .create();
    }

    @Override
    public CompletableFuture<NodeEndpoints> discover(InetSocketAddress failedTcpEndpoint) {
        CompletableFuture<NodeEndpoints> result = new CompletableFuture<>();

        if (settings.maxDiscoverAttempts != 0) {
            scheduler.execute(() -> discover(result, failedTcpEndpoint, 1));
        } else {
            result.completeExceptionally(new ClusterException("Cluster endpoint discover is not enabled."));
        }

        return result;
    }

    private void discover(CompletableFuture<NodeEndpoints> result, InetSocketAddress failedEndpoint, int attempt) {
        final String attemptInfo = (settings.maxDiscoverAttempts != -1) ?
            String.format("%d/%d", attempt, settings.maxDiscoverAttempts) : String.valueOf(attempt);

        try {
            Optional<NodeEndpoints> nodeEndpoints = tryDiscover(failedEndpoint);

            if (nodeEndpoints.isPresent()) {
                logger.info("Discovering attempt {} successful: best candidate is {}.", attemptInfo, nodeEndpoints.get());
                result.complete(nodeEndpoints.get());
            } else {
                logger.info("Discovering attempt {} failed: no candidate found.", attemptInfo);
            }
        } catch (Exception e) {
            logger.info("Discovering attempt {} failed.", attemptInfo, e);
        }

        if (!result.isDone() && (attempt < settings.maxDiscoverAttempts || settings.maxDiscoverAttempts == -1)) {
            scheduler.schedule(() -> discover(result, failedEndpoint, attempt + 1), 500, MILLISECONDS);
        } else {
            result.completeExceptionally(new ClusterException(String.format("Failed to discover candidate in %d attempts.", attempt)));
        }
    }

    private Optional<NodeEndpoints> tryDiscover(InetSocketAddress failedEndpoint) {
        oldGossipLock.lock();
        List<MemberInfoDto> oldGossipCopy;
        try {
            oldGossipCopy = oldGossip;
            oldGossip = null;
        } finally {
            oldGossipLock.unlock();
        }

        List<GossipSeed> gossipCandidates = (oldGossipCopy != null) ?
            getGossipCandidatesFromOldGossip(oldGossipCopy, failedEndpoint) : getGossipCandidatesFromDns();

        Iterator<GossipSeed> iterator = gossipCandidates.iterator();
        while (iterator.hasNext()) {
            Optional<ClusterInfoDto> gossip = tryGetGossipFrom(iterator.next())
                .filter(c -> c.members != null && !c.members.isEmpty());

            if (gossip.isPresent()) {
                Optional<NodeEndpoints> bestNode = tryDetermineBestNode(gossip.get().members);

                if (bestNode.isPresent()) {
                    oldGossipLock.lock();
                    try {
                        oldGossip = gossip.get().members;
                        return bestNode;
                    } finally {
                        oldGossipLock.unlock();
                    }
                }
            }
        }

        return Optional.empty();
    }

    private List<GossipSeed> getGossipCandidatesFromDns() {
        List<GossipSeed> endpoints;

        if (!settings.gossipSeeds.isEmpty()) {
            endpoints = new ArrayList<>(settings.gossipSeeds);
        } else {
            endpoints = resolveDns().stream()
                .map(address -> new GossipSeed(new InetSocketAddress(address, settings.externalGossipPort)))
                .collect(Collectors.toList());
        }

        if (endpoints.size() > 1) {
            Collections.shuffle(endpoints);
        }

        return endpoints;
    }

    private List<InetAddress> resolveDns() {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(settings.clusterDns);

            if (addresses == null || addresses.length == 0) {
                throw new ClusterException(String.format("DNS entry '%s' resolved into empty list.", settings.clusterDns));
            } else {
                return asList(addresses);
            }
        } catch (Exception e) {
            throw new ClusterException(String.format("Error while resolving DNS entry '%s'.", settings.clusterDns), e);
        }
    }

    private List<GossipSeed> getGossipCandidatesFromOldGossip(List<MemberInfoDto> oldGossip, InetSocketAddress failedTcpEndpoint) {
        List<MemberInfoDto> gossipCandidates = (failedTcpEndpoint == null) ? oldGossip : oldGossip.stream()
            .filter(m -> {
                try {
                    return !(m.externalTcpPort == failedTcpEndpoint.getPort() && InetAddress.getByName(m.externalTcpIp).equals(failedTcpEndpoint.getAddress()));
                } catch (UnknownHostException e) {
                    throw Throwables.propagate(e);
                }
            })
            .collect(Collectors.toList());

        return arrangeGossipCandidates(gossipCandidates);
    }

    private List<GossipSeed> arrangeGossipCandidates(List<MemberInfoDto> members) {
        List<GossipSeed> managers = new ArrayList<>();
        List<GossipSeed> nodes = new ArrayList<>();

        members.forEach(m -> {
            InetSocketAddress address = new InetSocketAddress(m.externalHttpIp, m.externalHttpPort);

            if (m.state == VNodeState.Manager) {
                managers.add(new GossipSeed(address));
            } else {
                nodes.add(new GossipSeed(address));
            }
        });

        Collections.shuffle(managers);
        Collections.shuffle(nodes);

        List<GossipSeed> result = new ArrayList<>();
        result.addAll(nodes);
        result.addAll(managers);

        return result;
    }

    private Optional<ClusterInfoDto> tryGetGossipFrom(GossipSeed gossipSeed) {
        try {
            URL url = new URL("http://" + gossipSeed.endpoint.getHostString() + ":" + gossipSeed.endpoint.getPort() + "/gossip?format=json");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout((int) settings.gossipTimeout.toMillis());
            connection.setReadTimeout((int) settings.gossipTimeout.toMillis());
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    return Optional.of(gson.fromJson(new JsonReader(reader), ClusterInfoDto.class));
                }
            }
        } catch (Exception e) {
            // ignore
        }

        return Optional.empty();
    }

    private Optional<NodeEndpoints> tryDetermineBestNode(List<MemberInfoDto> members) {
        Predicate<VNodeState> matchesNotAllowedStates = s ->
            s == VNodeState.Manager || s == VNodeState.ShuttingDown || s == VNodeState.Shutdown;

        return members.stream()
            .filter(m -> m.isAlive && !matchesNotAllowedStates.test(m.state))
            .sorted((a, b) -> a.state.ordinal() > b.state.ordinal() ? -1 : 1)
            .findFirst()
            .map(n -> {
                InetSocketAddress tcp = new InetSocketAddress(n.externalTcpIp, n.externalTcpPort);
                InetSocketAddress secureTcp = n.externalSecureTcpPort > 0 ? new InetSocketAddress(n.externalTcpIp, n.externalSecureTcpPort) : null;

                logger.info("Discovering: found best choice [{},{}] ({}).", tcp, secureTcp == null ? "n/a" : secureTcp.toString(), n.state);

                return new NodeEndpoints(tcp, secureTcp);
            });
    }

}
