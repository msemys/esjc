package lt.msemys.esjc.task;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;
import static lt.msemys.esjc.util.Preconditions.checkState;

public class TaskQueue {
    private final Executor executor;
    private final Queue<Task> queue = new ConcurrentLinkedQueue<>();
    private final Map<Class<? extends Task>, Consumer<Task>> handlers = new HashMap<>();
    private final AtomicBoolean processing = new AtomicBoolean();

    public TaskQueue() {
        this(Executors.newCachedThreadPool());
    }

    public TaskQueue(Executor executor) {
        this.executor = executor;
    }

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
                Consumer<Task> handler = handlers.get(task.getClass());
                checkState(handler != null, "No handler registered for task '%s'", task.getClass().getSimpleName());
                handler.accept(task);
            }

            processing.set(false);
        } while (!queue.isEmpty() && processing.compareAndSet(false, true));
    }

}
