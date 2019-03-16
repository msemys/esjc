package com.github.msemys.esjc.node.cluster;

/**
 * Indicates which order of preferred nodes for connecting to.
 */
public enum NodePreference {

    /**
     * When attempting connection, prefers master node.
     */
    Master,

    /**
     * When attempting connection, prefers slave node.
     */
    Slave,

    /**
     * When attempting connection, has no node preference.
     */
    Random
}
