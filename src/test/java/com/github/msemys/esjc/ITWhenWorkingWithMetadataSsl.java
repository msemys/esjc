package com.github.msemys.esjc;

public class ITWhenWorkingWithMetadataSsl extends ITWhenWorkingWithMetadata {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
