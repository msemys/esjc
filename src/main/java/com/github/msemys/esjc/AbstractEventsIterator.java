package com.github.msemys.esjc;

import java.util.Iterator;

abstract class AbstractEventsIterator implements Iterator<ResolvedEvent> {
    protected Iterator<ResolvedEvent> iterator;
    protected boolean endOfStream;

    @Override
    public boolean hasNext() {
        return iterator().hasNext();
    }

    @Override
    public ResolvedEvent next() {
        ResolvedEvent event = iterator().next();

        if (!iterator().hasNext() && !endOfStream) {
            read();
        }

        return event;
    }

    private Iterator<ResolvedEvent> iterator() {
        if (iterator == null) {
            read();
        }
        return iterator;
    }

    protected abstract void read();

}
