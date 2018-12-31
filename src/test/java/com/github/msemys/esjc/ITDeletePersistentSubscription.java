package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.AccessDeniedException;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITDeletePersistentSubscription extends AbstractEventStoreTest {

    public ITDeletePersistentSubscription(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void deletesExistingPersistentSubscriptionGroupWithPermissions() {
        final String stream = generateStreamName();
        final String group = "groupname123";

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromCurrent()
            .build();

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.deletePersistentSubscription(stream, group).join();
    }

    @Test
    public void deletesExistingPersistentSubscriptionWithSubscriber() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = "groupname123";

        CountDownLatch closeSignal = new CountDownLatch(1);

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromCurrent()
            .build();

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, new PersistentSubscriptionListener() {
            @Override
            public void onEvent(PersistentSubscription subscription, ResolvedEvent event) {
            }

            @Override
            public void onClose(PersistentSubscription subscription, SubscriptionDropReason reason, Exception exception) {
                closeSignal.countDown();
            }
        }).join();

        eventstore.deletePersistentSubscription(stream, group).join();

        assertTrue("onClose timeout", closeSignal.await(5, SECONDS));
    }

    @Test
    public void failsToDeletePersistentSubscriptionGroupThatDoesntExist() {
        final String stream = generateStreamName();
        final String group = UUID.randomUUID().toString();

        try {
            eventstore.deletePersistentSubscription(stream, group).join();
            fail("should fail with 'IllegalStateException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(IllegalStateException.class));
        }
    }

    @Test
    public void failsToDeletePersistentSubscriptionGroupWithoutPermissions() {
        final String stream = generateStreamName();
        final String group = UUID.randomUUID().toString();

        EventStore unauthenticatedEventstore = EventStoreBuilder.newBuilder(eventstore.settings())
            .noUserCredentials()
            .defaultExecutor()
            .build();

        try {
            unauthenticatedEventstore.deletePersistentSubscription(stream, group).join();
            fail("should fail with 'AccessDeniedException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(AccessDeniedException.class));
        } finally {
            unauthenticatedEventstore.shutdown();
        }
    }

}
