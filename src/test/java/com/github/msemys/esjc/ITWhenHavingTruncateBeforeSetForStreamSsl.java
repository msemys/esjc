package com.github.msemys.esjc;

public class ITWhenHavingTruncateBeforeSetForStreamSsl extends ITWhenHavingTruncateBeforeSetForStream {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
