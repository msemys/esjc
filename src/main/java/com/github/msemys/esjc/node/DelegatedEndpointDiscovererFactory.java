package com.github.msemys.esjc.node;

import com.github.msemys.esjc.Settings;

import java.util.concurrent.ScheduledExecutorService;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

/**
 * Delegated endpoint discoverer factory.
 */
public class DelegatedEndpointDiscovererFactory implements EndpointDiscovererFactory {

    private final EndpointDiscoverer discoverer;

    public DelegatedEndpointDiscovererFactory(EndpointDiscoverer discoverer) {
        checkNotNull(discoverer, "discoverer is null");
        this.discoverer = discoverer;
    }

    @Override
    public EndpointDiscoverer create(Settings settings, ScheduledExecutorService scheduler) {
        return discoverer;
    }

}
