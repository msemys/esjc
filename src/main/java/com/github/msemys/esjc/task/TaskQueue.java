package com.github.msemys.esjc.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static com.github.msemys.esjc.util.Preconditions.checkState;

public class TaskQueue {
    private static final Logger logger = LoggerFactory.getLogger(TaskQueue.class);

    private final Executor executor;
    private final Queue<Task> queue = new ConcurrentLinkedQueue<>();
    private final Map<Class<? extends Task>, Consumer<Task>> handlers = new HashMap<>();
    private final AtomicBoolean processing = new AtomicBoolean();

    public TaskQueue(Executor executor) {
        this.executor = executor;
    }

    @SuppressWarnings("unchecked")
    public <T extends Task> void register(Class<T> type, Consumer<T> handler) {
        checkNotNull(type, "type is null");
        checkNotNull(handler, "handler is null");
        handlers.put(type, (Consumer<Task>) handler);
    }

    public void enqueue(Task task) {
        checkNotNull(task, "task is null");

        queue.offer(task);

        if (processing.compareAndSet(false, true)) {
            executor.execute(this::process);
        }
    }

    private void process() {
        do {
            Task task;

            while ((task = queue.poll()) != null) {
                try {
                    Consumer<Task> handler = handlers.get(task.getClass());
                    checkState(handler != null, "No handler registered for task '%s'", task.getClass().getSimpleName());
                    handler.accept(task);
                } catch (Exception e) {
                    logger.error("Failed processing task: {}", task.getClass().getSimpleName(), e);
                    task.fail(e);
                }
            }

            processing.set(false);
        } while (!queue.isEmpty() && processing.compareAndSet(false, true));
    }

}
