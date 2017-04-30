package com.github.msemys.esjc.projection;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

/**
 * Create operation options.
 */
public class CreateOptions {

    /**
     * Transient projection default options.
     *
     * @see ProjectionMode#TRANSIENT
     */
    public static final CreateOptions TRANSIENT = newBuilder().mode(ProjectionMode.TRANSIENT).build();

    /**
     * One-time projection default options.
     *
     * @see ProjectionMode#ONE_TIME
     */
    public static final CreateOptions ONE_TIME = newBuilder().mode(ProjectionMode.ONE_TIME).build();

    /**
     * Continuous projection default options.
     *
     * @see ProjectionMode#CONTINUOUS
     */
    public static final CreateOptions CONTINUOUS = newBuilder().mode(ProjectionMode.CONTINUOUS).emit(true).build();


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


    private CreateOptions(Builder builder) {
        mode = builder.mode;
        enabled = builder.enabled;
        checkpoints = builder.checkpoints;
        emit = builder.emit;
        trackEmittedStreams = builder.trackEmittedStreams;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CreateOptions{");
        sb.append("mode=").append(mode);
        sb.append(", enabled=").append(enabled);
        sb.append(", checkpoints=").append(checkpoints);
        sb.append(", emit=").append(emit);
        sb.append(", trackEmittedStreams=").append(trackEmittedStreams);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Creates a new create operation options builder.
     *
     * @return create operation options builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Create operation options builder.
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
         * Builds a create operation options.
         *
         * @return create operation options
         */
        public CreateOptions build() {
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

            return new CreateOptions(this);
        }
    }

}
