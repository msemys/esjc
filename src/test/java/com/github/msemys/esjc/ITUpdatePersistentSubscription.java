package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.AccessDeniedException;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITUpdatePersistentSubscription extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void updatesExistingPersistentSubscription() {
        final String stream = generateStreamName();
        final String group = "existing";

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromCurrent()
            .build();

        eventstore.appendToStream(stream, ExpectedVersion.any(), newTestEvent()).join();

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.updatePersistentSubscription(stream, group, settings).join();
    }

    @Test
    public void updatesExistingPersistentSubscriptionWithSubscribers() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = "existing";

        CountDownLatch closeSignal = new CountDownLatch(1);
        AtomicReference<SubscriptionDropReason> closeReason = new AtomicReference<>();
        AtomicReference<Exception> closeException = new AtomicReference<>();

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromCurrent()
            .build();

        eventstore.appendToStream(stream, ExpectedVersion.any(), newTestEvent()).join();

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, new PersistentSubscriptionListener() {
            @Override
            public void onEvent(PersistentSubscription subscription, ResolvedEvent event) {
            }

            @Override
            public void onClose(PersistentSubscription subscription, SubscriptionDropReason reason, Exception exception) {
                closeReason.set(reason);
                closeException.set(exception);
                closeSignal.countDown();
            }
        }).join();

        eventstore.updatePersistentSubscription(stream, group, settings).join();

        assertTrue("onClose timeout", closeSignal.await(5, SECONDS));
        assertEquals(SubscriptionDropReason.UserInitiated, closeReason.get());
        assertNull(closeException.get());
    }

    @Test
    public void failsToUpdateNonExistingPersistentSubscription() {
        final String stream = generateStreamName();
        final String group = "nonexisting";

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromCurrent()
            .build();

        try {
            eventstore.updatePersistentSubscription(stream, group, settings).join();
            fail("should fail with 'IllegalStateException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(IllegalStateException.class));
        }
    }

    @Test
    public void failsToUpdateExistingPersistentSubscriptionWithoutPermissions() {
        final String stream = "$" + generateStreamName();
        final String group = "existing";

        UserCredentials admin = new UserCredentials("admin", "changeit");

        EventStore unauthenticatedEventstore = EventStoreBuilder.newBuilder(eventstore.settings())
            .noUserCredentials()
            .build();

        try {
            PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
                .resolveLinkTos(false)
                .startFromCurrent()
                .build();

            unauthenticatedEventstore.appendToStream(stream, ExpectedVersion.any(), newTestEvent(), admin).join();

            unauthenticatedEventstore.createPersistentSubscription(stream, group, settings, admin).join();

            try {
                unauthenticatedEventstore.updatePersistentSubscription(stream, group, settings).join();
                fail("should fail with 'AccessDeniedException'");
            } catch (Exception e) {
                assertThat(e.getCause(), instanceOf(AccessDeniedException.class));
            }
        } finally {
            unauthenticatedEventstore.disconnect();
        }
    }

}
