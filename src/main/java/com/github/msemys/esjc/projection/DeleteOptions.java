package com.github.msemys.esjc.projection;

/**
 * Delete operation options.
 */
public class DeleteOptions {

    /**
     * Deletes a projection without deleting the streams that were created by this projection.
     */
    public static final DeleteOptions PROJECTION_ONLY = newBuilder()
        .deleteCheckpointStream(false)
        .deleteStateStream(false)
        .deleteEmittedStreams(false)
        .build();

    /**
     * Deletes a projection and all streams that were created by this projection.
     */
    public static final DeleteOptions ALL = newBuilder()
        .deleteCheckpointStream(true)
        .deleteStateStream(true)
        .deleteEmittedStreams(true)
        .build();


    /**
     * Whether or not to delete the checkpoint stream.
     */
    public final boolean deleteCheckpointStream;

    /**
     * Whether or not to delete the state stream.
     */
    public final boolean deleteStateStream;

    /**
     * Whether or not to delete the emitted streams stream.
     */
    public final boolean deleteEmittedStreams;

    private DeleteOptions(Builder builder) {
        deleteCheckpointStream = builder.deleteCheckpointStream;
        deleteStateStream = builder.deleteStateStream;
        deleteEmittedStreams = builder.deleteEmittedStreams;
    }

    /**
     * Creates a new delete operation options builder.
     *
     * @return delete operation options builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DeleteOptions{");
        sb.append("deleteCheckpointStream=").append(deleteCheckpointStream);
        sb.append(", deleteStateStream=").append(deleteStateStream);
        sb.append(", deleteEmittedStreams=").append(deleteEmittedStreams);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Delete operation options builder.
     */
    public static class Builder {
        private Boolean deleteCheckpointStream;
        private Boolean deleteStateStream;
        private Boolean deleteEmittedStreams;

        /**
         * Specifies whether or not to delete the checkpoint stream (by default, is not deleted).
         *
         * @param delete {@code true} to delete.
         * @return the builder reference
         */
        public Builder deleteCheckpointStream(boolean delete) {
            deleteCheckpointStream = delete;
            return this;
        }

        /**
         * Specifies whether or not to delete the state stream (by default, is not deleted).
         *
         * @param delete {@code true} to delete.
         * @return the builder reference
         */
        public Builder deleteStateStream(boolean delete) {
            deleteStateStream = delete;
            return this;
        }

        /**
         * Specifies whether or not to delete the emitted streams stream (by default, is not deleted).
         *
         * @param delete {@code true} to delete.
         * @return the builder reference
         */
        public Builder deleteEmittedStreams(boolean delete) {
            deleteEmittedStreams = delete;
            return this;
        }

        /**
         * Builds delete operation options.
         *
         * @return delete operation options
         */
        public DeleteOptions build() {
            if (deleteCheckpointStream == null) {
                deleteCheckpointStream = false;
            }

            if (deleteStateStream == null) {
                deleteStateStream = false;
            }

            if (deleteEmittedStreams == null) {
                deleteEmittedStreams = false;
            }

            return new DeleteOptions(this);
        }
    }

}
