package com.github.msemys.esjc.projection;

import static com.github.msemys.esjc.projection.ProjectionMode.*;
import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

/**
 * Projection settings.
 */
public class ProjectionSettings {

    /**
     * Default transient projection settings.
     *
     * @see ProjectionMode#TRANSIENT
     */
    public static final ProjectionSettings DEFAULT_TRANSIENT = newBuilder().mode(TRANSIENT).build();

    /**
     * Default one-time projection settings.
     *
     * @see ProjectionMode#ONE_TIME
     */
    public static final ProjectionSettings DEFAULT_ONE_TIME = newBuilder().mode(ONE_TIME).build();

    /**
     * Default continuous projection settings.
     *
     * @see ProjectionMode#CONTINUOUS
     */
    public static final ProjectionSettings DEFAULT_CONTINUOUS = newBuilder().mode(CONTINUOUS).emit(true).build();


    /**
     * Projection mode.
     */
    public final ProjectionMode mode;

    /**
     * Whether or not the projection is enabled.
     */
    public final boolean enabled;

    /**
     * Whether or not the checkpoints are enabled.
     */
    public final boolean checkpoints;

    /**
     * Whether or not the projection is allowed to write to streams.
     */
    public final boolean emit;

    /**
     * Whether or not the streams emitted by this projection will be tracked.
     * <p>
     * When this option is enabled, the name of the streams the projection is managing is written
     * to a separate stream ({@code $projections-PROJECTION_NAME-emittedstreams}).
     */
    public final boolean trackEmittedStreams;


    private ProjectionSettings(Builder builder) {
        mode = builder.mode;
        enabled = builder.enabled;
        checkpoints = builder.checkpoints;
        emit = builder.emit;
        trackEmittedStreams = builder.trackEmittedStreams;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ProjectionSettings{");
        sb.append("mode=").append(mode);
        sb.append(", enabled=").append(enabled);
        sb.append(", checkpoints=").append(checkpoints);
        sb.append(", emit=").append(emit);
        sb.append(", trackEmittedStreams=").append(trackEmittedStreams);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Creates a new projection settings builder.
     *
     * @return projection settings builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Projection settings builder.
     */
    public static class Builder {
        private ProjectionMode mode;
        private Boolean enabled;
        private Boolean checkpoints;
        private Boolean emit;
        private Boolean trackEmittedStreams;

        /**
         * Sets the projection mode.
         *
         * @param mode projection mode.
         * @return the builder reference
         */
        public Builder mode(ProjectionMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Specifies whether or not the projection is enabled (by default, it is enabled).
         *
         * @param enabled {@code true} to enable.
         * @return the builder reference
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Specifies whether or not the checkpoints are enabled (by default, it is disabled).
         * <p><b>Note:</b> used with {@link ProjectionMode#ONE_TIME} mode only.
         *
         * @param enabled {@code true} to enable checkpoints.
         * @return the builder reference
         */
        public Builder checkpoints(boolean enabled) {
            this.checkpoints = enabled;
            return this;
        }

        /**
         * Specifies whether or not the projection is allowed to write to streams (by default, it is disabled).
         * <p><b>Note:</b> used with {@link ProjectionMode#ONE_TIME} and {@link ProjectionMode#CONTINUOUS} modes only.
         *
         * @param enabled {@code true} to allow to write to streams.
         * @return the builder reference
         */
        public Builder emit(boolean enabled) {
            this.emit = enabled;
            return this;
        }

        /**
         * Specifies whether or not the streams emitted by this projection will be tracked (by default, it is disabled).
         * <p><b>Note:</b> used with {@link ProjectionMode#ONE_TIME} and {@link ProjectionMode#CONTINUOUS} modes only.
         *
         * @param enabled {@code true} to track the streams emitted by this projection.
         * @return the builder reference
         */
        public Builder trackEmittedStreams(boolean enabled) {
            this.trackEmittedStreams = enabled;
            return this;
        }

        /**
         * Builds a projection settings.
         *
         * @return projection settings
         */
        public ProjectionSettings build() {
            checkNotNull(mode, "mode is null");

            if (enabled == null) {
                enabled = true;
            }

            if (checkpoints == null) {
                checkpoints = false;
            }

            if (emit == null) {
                emit = false;
            }

            if (trackEmittedStreams == null) {
                trackEmittedStreams = false;
            }

            return new ProjectionSettings(this);
        }
    }

}
