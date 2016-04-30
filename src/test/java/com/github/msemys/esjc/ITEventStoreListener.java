package com.github.msemys.esjc;

import com.github.msemys.esjc.event.ClientConnected;
import com.github.msemys.esjc.event.ClientDisconnected;
import com.github.msemys.esjc.event.ConnectionClosed;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class ITEventStoreListener extends AbstractIntegrationTest {

    private CountDownLatch clientConnectedSignal;
    private CountDownLatch clientDisconnectedSignal;
    private CountDownLatch connectionClosedSignal;

    @Rule
    public Verifier verifier = new Verifier() {
        @Override
        protected void verify() throws Throwable {
            assertTrue("connection close timeout", connectionClosedSignal.await(2, SECONDS));
            assertTrue("client disconnect timeout", clientDisconnectedSignal.await(2, SECONDS));
        }
    };

    @Override
    protected EventStore createEventStore() {
        return applyTestListener(eventstoreSupplier.get());
    }

    protected EventStore applyTestListener(EventStore eventstore) {
        clientConnectedSignal = new CountDownLatch(1);
        clientDisconnectedSignal = new CountDownLatch(1);
        connectionClosedSignal = new CountDownLatch(1);

        eventstore.addListener(event -> {
            if (event instanceof ClientConnected) {
                clientConnectedSignal.countDown();
            } else if (event instanceof ClientDisconnected) {
                clientDisconnectedSignal.countDown();
            } else if (event instanceof ConnectionClosed) {
                connectionClosedSignal.countDown();
            }
        });

        return eventstore;
    }

    @Test
    public void firesClientConnectedEvent() throws InterruptedException {
        assertTrue("client connect timeout", clientConnectedSignal.await(15, SECONDS));
    }

}
