package com.github.msemys.esjc.operation;

import java.net.InetSocketAddress;
import java.util.Optional;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

public class InspectionResult {
    public final InspectionDecision decision;
    public final String description;
    public final Optional<InetSocketAddress> address;
    public final Optional<InetSocketAddress> secureAddress;

    private InspectionResult(Builder builder) {
        this.decision = builder.decision;
        this.description = builder.description;
        this.address = Optional.ofNullable(builder.address);
        this.secureAddress = Optional.ofNullable(builder.secureAddress);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private InspectionDecision decision;
        private String description;
        private InetSocketAddress address;
        private InetSocketAddress secureAddress;

        private Builder() {
        }

        public Builder decision(InspectionDecision decision) {
            this.decision = decision;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder address(String hostname, int port) {
            this.address = new InetSocketAddress(hostname, port);
            return this;
        }

        public Builder secureAddress(String hostname, int port) {
            this.secureAddress = new InetSocketAddress(hostname, port);
            return this;
        }

        public InspectionResult build() {
            checkNotNull(decision, "Decision not specified.");
            checkNotNull(description, "Description not specified.");

            if (decision == InspectionDecision.Reconnect) {
                checkNotNull(address, "Address not specified.");
            } else {
                checkArgument(address == null, "Address is specified for decision %s.", decision);
            }

            return new InspectionResult(this);
        }
    }
}
