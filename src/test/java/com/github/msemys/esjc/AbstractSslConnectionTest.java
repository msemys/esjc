package com.github.msemys.esjc;

import com.github.msemys.esjc.event.Event;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AbstractSslConnectionTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected static void assertSignal(CountDownLatch signal) throws InterruptedException {
        assertTrue("timeout", signal.await(15, SECONDS));
    }

    @SafeVarargs
    protected static void assertThrowable(Throwable throwable, Matcher<? super Throwable>... matchers) {
        assertThat(throwable, is(notNullValue()));
        assertThat(unwrapCauses(throwable), hasItem(allOf(asList(matchers))));
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Event> EventStoreListener onEvent(Class<T> eventClass, Consumer<T> consumer) {
        return event -> {
            if (eventClass.isAssignableFrom(event.getClass())) {
                consumer.accept((T) event);
            }
        };
    }

    protected static EventStoreBuilder newEventStoreBuilder() {
        return EventStoreBuilder.newBuilder()
            .singleNodeAddress("127.0.0.1", 7779)
            .userCredentials("admin", "changeit")
            .maxReconnections(2);
    }

    private static List<Throwable> unwrapCauses(Throwable thrown) {
        List<Throwable> result = new ArrayList<>();

        do {
            result.add(thrown);
            thrown = thrown.getCause();
        } while (thrown != null);

        return result;
    }

}
