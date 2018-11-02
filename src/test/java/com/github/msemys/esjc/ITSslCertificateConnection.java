package com.github.msemys.esjc;

import com.github.msemys.esjc.event.ClientConnected;
import com.github.msemys.esjc.event.ErrorOccurred;
import org.junit.Test;

import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.fail;

public class ITSslCertificateConnection extends AbstractSslConnectionTest {

    @Test
    public void connectsWithMatchingCertificateFile() throws InterruptedException {
        testSuccessfulConnection(new File("tools/ssl/domain.crt"));
    }

    @Test
    public void connectsWithMatchingCACertificateFile() throws InterruptedException {
        testSuccessfulConnection(new File("tools/ssl/rootCA.crt"));
    }

    @Test
    public void failsWithNonMatchingCertificateFile() throws Throwable {
        final File certificateFile = new File("tools/ssl/invalid.crt");

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
        final File certificateFile = new File("doesnotexist.csr");
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("certificateFile '" + certificateFile + "' does not exist");

        eventstore = createEventStore(certificateFile);
        fail("Exception expected!");
    }

    @Test
    public void failsWithEmptyCertificateFileName() throws InterruptedException {
        final File certificateFile = new File("");
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("certificateFile '' does not exist");

        eventstore = createEventStore(certificateFile);
        fail("Exception expected!");
    }

    private void testSuccessfulConnection(final File certificateFile) throws InterruptedException {
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

    private EventStore createEventStore(final File certificateFile) {
        return EventStoreBuilder.newBuilder()
            .singleNodeAddress("127.0.0.1", 7779)
            .useSslConnection(certificateFile)
            .userCredentials("admin", "changeit")
            .maxReconnections(2)
            .build();
    }
}
