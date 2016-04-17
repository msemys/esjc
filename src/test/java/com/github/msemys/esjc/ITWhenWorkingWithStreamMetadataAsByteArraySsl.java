package com.github.msemys.esjc;

public class ITWhenWorkingWithStreamMetadataAsByteArraySsl extends ITWhenWorkingWithStreamMetadataAsByteArray {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
