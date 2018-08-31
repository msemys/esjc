package com.github.msemys.esjc;

import com.github.msemys.esjc.event.ClientConnected;
import com.github.msemys.esjc.event.ErrorOccurred;
import org.junit.Test;

import javax.net.ssl.SSLHandshakeException;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.fail;

public class ITSslCertificateConnection extends AbstractSslConnectionTest {

    @Test
    public void connectsWithMatchingCertificateFile() throws InterruptedException {
        testSuccessfulConnection("tools/ssl/domain.crt");
    }

    @Test
    public void connectsWithMatchingCACertificateFile() throws InterruptedException {
        testSuccessfulConnection("tools/ssl/rootCA.crt");
    }

    @Test
    public void failsWithNonMatchingCertificateFile() throws Throwable {
        final String certificateFile = "tools/ssl/invalid.crt";

        eventstore = createEventStore(certificateFile);
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
        assertHasCause(thrown, instanceOf(SSLHandshakeException.class));

        eventstore.disconnect(); // is probably already disconnected, waiting for disconnection is flaky.
        eventstore = null;
    }

    @Test
    public void failsWithNonExistingCertificateFile() throws InterruptedException {
        final String certificateFile = "doesnotexist.csr";
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("certificateFile " + certificateFile + " does not exist");

        eventstore = createEventStore(certificateFile);
        fail("Exception expected!");
    }

    @Test
    public void failsWithEmptyCertificateFileName() throws InterruptedException {
        final String certificateFile = "";
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("certificateFile is null or empty");

        eventstore = createEventStore(certificateFile);
        fail("Exception expected!");
    }

    private void testSuccessfulConnection(final String certificateFile) throws InterruptedException {
        eventstore = createEventStore(certificateFile);

        CountDownLatch connectedSignal = new CountDownLatch(1);
        eventstore.addListener(event -> {
            if (event instanceof ClientConnected) {
                connectedSignal.countDown();
            }
        });

        eventstore.connect();
        assertNoTimeout(connectedSignal);
    }

    private EventStore createEventStore(final String certificateFile) {
        return EventStoreBuilder.newBuilder()
            .singleNodeAddress("127.0.0.1", 7779)
            .useSslConnectionCertificateFile(certificateFile)
            .userCredentials("admin", "changeit")
            .maxReconnections(2)
            .build();
    }
}
