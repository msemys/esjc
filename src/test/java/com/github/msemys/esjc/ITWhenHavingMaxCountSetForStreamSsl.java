package com.github.msemys.esjc;

public class ITWhenHavingMaxCountSetForStreamSsl extends ITWhenHavingMaxCountSetForStream {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
