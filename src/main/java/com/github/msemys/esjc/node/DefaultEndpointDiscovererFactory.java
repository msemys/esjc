package com.github.msemys.esjc.node;

import com.github.msemys.esjc.Settings;
import com.github.msemys.esjc.node.cluster.ClusterEndpointDiscoverer;
import com.github.msemys.esjc.node.single.SingleEndpointDiscoverer;

import java.util.concurrent.ScheduledExecutorService;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

/**
 * Default endpoint discoverer factory.
 */
public class DefaultEndpointDiscovererFactory implements EndpointDiscovererFactory {

    @Override
    public EndpointDiscoverer create(Settings settings, ScheduledExecutorService scheduler) {
        checkNotNull(settings, "settings is null");
        checkNotNull(scheduler, "scheduler is null");

        if (settings.singleNodeSettings != null) {
            return new SingleEndpointDiscoverer(settings.singleNodeSettings, settings.sslSettings.useSslConnection);
        } else if (settings.clusterNodeSettings != null) {
            return new ClusterEndpointDiscoverer(settings.clusterNodeSettings, scheduler);
        } else {
            throw new IllegalStateException("Node settings not found");
        }
    }

}
