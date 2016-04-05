package com.github.msemys.esjc;

public class ITDeleteStreamSsl extends ITDeleteStream {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
