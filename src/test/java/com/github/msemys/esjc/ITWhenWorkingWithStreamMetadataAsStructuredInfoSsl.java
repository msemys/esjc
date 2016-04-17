package com.github.msemys.esjc;

public class ITWhenWorkingWithStreamMetadataAsStructuredInfoSsl extends ITWhenWorkingWithStreamMetadataAsStructuredInfo {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
