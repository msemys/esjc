package com.github.msemys.esjc;

import com.github.msemys.esjc.event.ClientDisconnected;
import com.github.msemys.esjc.event.Event;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AbstractSslConnectionTest {

    protected EventStore eventstore;

    protected static final int TEST_TIMEOUT = 15;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected volatile Throwable thrown = null;

    @After
    public void tearDown() throws Exception {
        awaitDisconnected(eventstore);
    }

    protected static void assertNoTimeout(CountDownLatch signal) throws InterruptedException {
        assertTrue("client connect timeout", signal.await(TEST_TIMEOUT, SECONDS));
    }

    protected static void assertHasCause(Throwable thrown, Matcher<? super Throwable> matcher) {
        assertThat(unwrapCauses(thrown), hasItem(matcher));
    }

    private static void awaitDisconnected(EventStore eventstore) throws InterruptedException {
        if (eventstore == null)
            return;
        CountDownLatch clientDisconnectedSignal = new CountDownLatch(1);
        eventstore.addListener(createListener(clientDisconnectedSignal, ClientDisconnected.class));
        eventstore.disconnect();

        assertTrue("client disconnect timeout", clientDisconnectedSignal.await(TEST_TIMEOUT, SECONDS));
    }

    private static List<Throwable> unwrapCauses(Throwable thrown) {
        assertThat(thrown, is(notNullValue()));
        List<Throwable> result = new ArrayList<>();
        do {
            result.add(thrown);
            thrown = thrown.getCause();
        } while (thrown != null);
        return result;
    }

    private static <T extends Class<E>, E extends Event> EventStoreListener createListener(
        final CountDownLatch signal,
        final T eventClass
    ) {
        return event -> {
            if (eventClass.isAssignableFrom(event.getClass())) {
                signal.countDown();
            }
        };
    }
}
