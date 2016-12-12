package com.github.msemys.esjc.event;

import com.github.msemys.esjc.EventStoreListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

public class EventQueue {
    private static final Logger logger = LoggerFactory.getLogger(EventQueue.class);

    private final Executor executor;
    private final Queue<Event> queue = new ConcurrentLinkedQueue<>();
    private final Set<EventStoreListener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean processing = new AtomicBoolean();

    public EventQueue(Executor executor) {
        this.executor = executor;
    }

    public void register(EventStoreListener listener) {
        listeners.add(listener);
    }

    public void unregister(EventStoreListener listener) {
        listeners.remove(listener);
    }

    public void enqueue(Event event) {
        checkNotNull(event, "event is null");

        queue.offer(event);

        if (processing.compareAndSet(false, true)) {
            executor.execute(this::process);
        }
    }

    private void process() {
        do {
            Event event;

            while ((event = queue.poll()) != null) {
                for (EventStoreListener listener : listeners) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        logger.error("Error occurred while handling '{}' event in {}",
                            event.getClass().getSimpleName(),
                            listener.getClass().getName(),
                            e);
                    }
                }
            }

            processing.set(false);
        } while (!queue.isEmpty() && processing.compareAndSet(false, true));
    }

}
