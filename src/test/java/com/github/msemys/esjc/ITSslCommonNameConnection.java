package com.github.msemys.esjc;

import com.github.msemys.esjc.event.ClientConnected;
import com.github.msemys.esjc.event.ErrorOccurred;
import org.junit.Test;

import java.security.cert.CertificateException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.fail;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

public class ITSslCommonNameConnection extends AbstractSslConnectionTest {

    @Test
    public void connectsWithMatchingCommonName() throws InterruptedException {
        EventStore eventstore = createEventStore("localhost");

        CountDownLatch signal = new CountDownLatch(1);
        eventstore.addListener(onEvent(ClientConnected.class, e -> signal.countDown()));

        eventstore.connect();

        assertSignal(signal);

        eventstore.shutdown();
    }

    @Test
    public void failsWithNonMatchingCommonName() throws InterruptedException {
        EventStore eventstore = createEventStore("somethingelse");

        CountDownLatch signal = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        eventstore.addListener(onEvent(ErrorOccurred.class, e -> {
            error.set(e.throwable);
            signal.countDown();
        }));

        eventstore.connect();

        assertSignal(signal);
        assertThrowable(error.get(),
            instanceOf(CertificateException.class),
            hasMessage(containsString("server certificate common name (CN) mismatch")));

        eventstore.shutdown();
    }

    @Test
    public void failsWithEmptyCommonName() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("certificateCommonName is null or empty");

        createEventStore("");

        fail("Exception expected!");
    }

    private static EventStore createEventStore(final String commonName) {
        return newEventStoreBuilder().useSslConnection(commonName).build();
    }

}
