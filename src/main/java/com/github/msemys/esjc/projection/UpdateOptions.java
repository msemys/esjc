package com.github.msemys.esjc.projection;

/**
 * Update operation options.
 */
public class UpdateOptions {

    /**
     * Updates projection query without changing the current emit option.
     */
    public static final UpdateOptions QUERY_ONLY = newBuilder().ignoreEmit().build();

    /**
     * Updates projection query and enables emit option, that allows projection to write to streams.
     */
    public static final UpdateOptions EMIT_ENABLED = newBuilder().emit(true).build();

    /**
     * Updates projection query and disables emit option, that denies projection to write to streams.
     */
    public static final UpdateOptions EMIT_DISABLED = newBuilder().emit(false).build();


    /**
     * Whether or not the projection is allowed to write to streams.
     * <p><b>Note:</b> option ignored when {@code null}
     */
    public final Boolean emit;

    private UpdateOptions(Builder builder) {
        emit = builder.emit;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UpdateOptions{");
        sb.append("emit=").append(emit);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Creates a new update operation options builder.
     *
     * @return update operation options builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Update operation options builder.
     */
    public static class Builder {
        private Boolean emit;

        /**
         * Specifies whether or not the projection is allowed to write to streams (by default, it is ignored).
         *
         * @param enabled {@code true} to allow to write to streams.
         * @return the builder reference
         * @see #ignoreEmit()
         */
        public Builder emit(boolean enabled) {
            this.emit = enabled;
            return this;
        }

        /**
         * Ignores emit option modification.
         *
         * @return the builder reference
         */
        public Builder ignoreEmit() {
            emit = null;
            return this;
        }

        /**
         * Builds an update operation options.
         *
         * @return update operation options
         */
        public UpdateOptions build() {
            return new UpdateOptions(this);
        }
    }

}
