package com.github.msemys.esjc.node;

import com.github.msemys.esjc.Settings;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Endpoint discoverer factory.
 */
public interface EndpointDiscovererFactory {

    /**
     * Creates endpoint discoverer
     *
     * @param settings  client settings.
     * @param scheduler scheduled executor service that could be used to discover endpoint.
     * @return endpoint discoverer
     */
    EndpointDiscoverer create(Settings settings, ScheduledExecutorService scheduler);

}
