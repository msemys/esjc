package com.github.msemys.esjc.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.function.Consumer;

public class Iterables {

    public static <T> void consume(Collection<T> collection, Consumer<T> consumer) {
        Iterator<T> iterator = collection.iterator();

        while (iterator.hasNext()) {
            consumer.accept(iterator.next());
            iterator.remove();
        }
    }

    public static <T> void consume(Queue<T> queue, Consumer<T> consumer) {
        T item;
        while ((item = queue.poll()) != null) {
            consumer.accept(item);
        }
    }

}
