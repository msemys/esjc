package lt.msemys.esjc.util;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class Throwables {

    public static <T> Supplier<T> unchecked(Callable<T> f) {
        return () -> {
            try {
                return f.call();
            } catch (Throwable t) {
                throw propagate(t);
            }
        };
    }

    public static RuntimeException propagate(Throwable throwable) {
        return (throwable instanceof RuntimeException) ? (RuntimeException) throwable : new RuntimeException(throwable);
    }

}
