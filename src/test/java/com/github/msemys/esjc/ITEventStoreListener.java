package com.github.msemys.esjc;

import com.github.msemys.esjc.event.ClientConnected;
import com.github.msemys.esjc.event.ClientDisconnected;
import com.github.msemys.esjc.event.ConnectionClosed;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class ITEventStoreListener extends AbstractEventStoreTest {

    public ITEventStoreListener(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void firesClientInternalEvents() throws InterruptedException {
        CountDownLatch clientConnectedSignal = new CountDownLatch(1);
        CountDownLatch clientDisconnectedSignal = new CountDownLatch(1);
        CountDownLatch connectionClosedSignal = new CountDownLatch(1);

        eventstore.addListener(event -> {
            if (event instanceof ClientConnected) {
                clientConnectedSignal.countDown();
            } else if (event instanceof ClientDisconnected) {
                clientDisconnectedSignal.countDown();
            } else if (event instanceof ConnectionClosed) {
                connectionClosedSignal.countDown();
            }
        });

        eventstore.connect();

        assertTrue("client connect timeout", clientConnectedSignal.await(15, SECONDS));

        eventstore.disconnect();

        assertTrue("connection close timeout", connectionClosedSignal.await(2, SECONDS));
        assertTrue("client disconnect timeout", clientDisconnectedSignal.await(2, SECONDS));
    }

}
