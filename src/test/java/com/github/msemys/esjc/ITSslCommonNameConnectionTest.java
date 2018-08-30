package com.github.msemys.esjc;

import com.github.msemys.esjc.event.ClientConnected;
import com.github.msemys.esjc.event.ErrorOccurred;
import org.junit.Test;
import org.junit.internal.matchers.ThrowableMessageMatcher;

import java.security.cert.CertificateException;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.fail;

public class ITSslCommonNameConnectionTest extends AbstractSslConnectionTest {

    @Test
    public void connectsWithMatchingCommonName() throws InterruptedException {
        eventstore = createEventStore("localhost");

        CountDownLatch connectedSignal = new CountDownLatch(1);
        eventstore.addListener(event -> {
            if (event instanceof ClientConnected) {
                connectedSignal.countDown();
            }
        });

        eventstore.connect();
        assertNoTimeout(connectedSignal);
    }

    @Test
    public void failsWithEmptyCommonName() {
        final String commonName = "";
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("certificateCommonName is null or empty");

        eventstore = createEventStore(commonName);
        fail("Exception expected!");
    }


    @Test
    public void failsWithNonMatchingCommonName() throws InterruptedException {
        final String commonName = "somethingelse";

        eventstore = createEventStore(commonName);
        CountDownLatch connectionErrorSignal = new CountDownLatch(1);
        eventstore.addListener(event -> {
            if (event instanceof ErrorOccurred) {
                ErrorOccurred error = (ErrorOccurred) event;
                thrown = error.throwable;
                connectionErrorSignal.countDown();
            }
        });

        eventstore.connect();

        assertNoTimeout(connectionErrorSignal);
        assertHasCause(
            thrown,
            allOf(
                instanceOf(CertificateException.class),
                ThrowableMessageMatcher.hasMessage(containsString("server certificate common name (CN) mismatch"))
            )
        );
        eventstore.disconnect(); // is probably already disconnected, waiting for disconnection is flaky.
        eventstore = null;
    }

    private EventStore createEventStore(final String commonName) {
        return EventStoreBuilder.newBuilder()
            .singleNodeAddress("127.0.0.1", 7779)
            .useSslConnection(commonName)
            .userCredentials("admin", "changeit")
            .maxReconnections(2)
            .build();
    }
}
