package com.github.msemys.esjc.projection;

/**
 * Projection mode.
 */
public enum ProjectionMode {

    /**
     * Transient (ad-hoc) projection runs until completion and is automatically deleted afterwards.
     */
    TRANSIENT,

    /**
     * One-time projection runs until completion and then stops.
     */
    ONE_TIME,

    /**
     * Continuous projection runs continuously unless disabled or an unrecoverable error has been encountered.
     */
    CONTINUOUS

}
