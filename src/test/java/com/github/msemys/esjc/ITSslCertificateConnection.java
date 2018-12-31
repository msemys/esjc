package com.github.msemys.esjc;

import com.github.msemys.esjc.event.ClientConnected;
import com.github.msemys.esjc.event.ErrorOccurred;
import org.junit.Test;

import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.fail;

public class ITSslCertificateConnection extends AbstractSslConnectionTest {

    @Test
    public void connectsWithMatchingCertificateFile() throws InterruptedException {
        testSuccessfulConnection(new File("ssl/domain.crt"));
    }

    @Test
    public void connectsWithMatchingCACertificateFile() throws InterruptedException {
        testSuccessfulConnection(new File("ssl/rootCA.crt"));
    }

    @Test
    public void failsWithNonMatchingCertificateFile() throws Throwable {
        final File certificateFile = new File("ssl/invalid.crt");

        EventStore eventstore = createEventStore(certificateFile);

        CountDownLatch signal = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        eventstore.addListener(onEvent(ErrorOccurred.class, e -> {
            error.set(e.throwable);
            signal.countDown();
        }));

        eventstore.connect();

        assertSignal(signal);
        assertThrowable(error.get(), instanceOf(SSLHandshakeException.class));

        eventstore.shutdown();
    }

    @Test
    public void failsWithNonExistingCertificateFile() {
        final File certificateFile = new File("doesnotexist.csr");
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("certificateFile '" + certificateFile + "' does not exist");

        createEventStore(certificateFile);

        fail("Exception expected!");
    }

    @Test
    public void failsWithEmptyCertificateFileName() {
        final File certificateFile = new File("");
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("certificateFile '' does not exist");

        createEventStore(certificateFile);

        fail("Exception expected!");
    }

    private void testSuccessfulConnection(final File certificateFile) throws InterruptedException {
        EventStore eventstore = createEventStore(certificateFile);

        CountDownLatch signal = new CountDownLatch(1);
        eventstore.addListener(onEvent(ClientConnected.class, e -> signal.countDown()));

        eventstore.connect();

        assertSignal(signal);

        eventstore.shutdown();
    }

    private static EventStore createEventStore(final File certificateFile) {
        return newEventStoreBuilder().useSslConnection(certificateFile).build();
    }

}
