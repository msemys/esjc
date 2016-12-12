package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.AccessDeniedException;
import com.github.msemys.esjc.subscription.MaximumSubscribersReachedException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITSubscribeToPersistentSubscription extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void failsToSubscribeToNonExistingPersistentSubscription() {
        final String stream = generateStreamName();
        final String group = "foo";

        try {
            eventstore.subscribeToPersistent(stream, group, (s, e) -> System.out.println(e)).join();
            fail("should fail with 'IllegalArgumentException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
        }
    }

    @Test
    public void subscribesToExistingPersistentSubscriptionWithPermissions() {
        final String stream = generateStreamName();
        final String group = "agroupname17";

        eventstore.createPersistentSubscription(stream, group).join();

        PersistentSubscription subscription = eventstore.subscribeToPersistent(stream, group, (s, e) -> System.out.println(e)).join();

        assertNotNull(subscription);
    }

    @Test
    public void failsToSubscribeToExistingPersistentSubscriptionWithoutPermissions() {
        final String stream = "$" + generateStreamName();
        final String group = "agroupname55";

        EventStore unauthenticatedEventstore = EventStoreBuilder.newBuilder(eventstore.settings())
            .noUserCredentials()
            .build();

        try {
            PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
                .resolveLinkTos(false)
                .startFromCurrent()
                .build();

            unauthenticatedEventstore.createPersistentSubscription(stream, group, settings, new UserCredentials("admin", "changeit")).join();

            try {
                unauthenticatedEventstore.subscribeToPersistent(stream, group, (s, e) -> System.out.println(e)).join();
                fail("should fail with 'AccessDeniedException'");
            } catch (Exception e) {
                assertThat(e.getCause(), instanceOf(AccessDeniedException.class));
            }
        } finally {
            awaitDisconnected(unauthenticatedEventstore);
        }
    }

    @Test
    public void failsToSubscribeSecondTimeToExistingPersistentSubscriptionWithMaxOneSubscriber() {
        final String stream = generateStreamName();
        final String group = "startinbeginning1";

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromCurrent()
            .maxSubscriberCount(1)
            .build();

        eventstore.createPersistentSubscription(stream, group, settings).join();

        PersistentSubscription subscription = eventstore.subscribeToPersistent(stream, group, (s, e) -> System.out.println(e)).join();
        assertNotNull(subscription);

        try {
            eventstore.subscribeToPersistent(stream, group, (s, e) -> System.out.println(e)).join();
            fail("should fail with 'MaximumSubscribersReachedException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(MaximumSubscribersReachedException.class));
        }
    }

    @Test
    public void subscribesToExistingPersistentSubscriptionWithStartFromBeginningAndNoStream() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = "startinbeginning1";

        CountDownLatch eventSignal = new CountDownLatch(1);
        UUID eventId = UUID.randomUUID();
        List<ResolvedEvent> events = new ArrayList<>();

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromBeginning()
            .build();

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, (s, e) -> {
            events.add(e);
            eventSignal.countDown();
        }).join();

        eventstore.appendToStream(stream, ExpectedVersion.ANY,
            EventData.newBuilder()
                .eventId(eventId)
                .type("test")
                .jsonData("{'foo' : 'bar'}")
                .metadata(new byte[0])
                .build()
        ).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
        assertEquals(1, events.size());
        assertEquals(0, events.get(0).event.eventNumber);
        assertEquals(eventId, events.get(0).event.eventId);
    }

    @Test
    public void subscribesToExistingPersistentSubscriptionWithStartFromTwoAndNoStream() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = "startinbeginning1";

        CountDownLatch eventSignal = new CountDownLatch(1);
        List<UUID> eventIds = new ArrayList<>();
        List<ResolvedEvent> events = new ArrayList<>();

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFrom(2)
            .build();

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, (s, e) -> {
            events.add(e);
            eventSignal.countDown();
        }).join();

        range(0, 3).forEach(i -> {
            eventIds.add(UUID.randomUUID());
            eventstore.appendToStream(stream, ExpectedVersion.ANY,
                EventData.newBuilder()
                    .eventId(eventIds.get(i))
                    .type("test")
                    .jsonData("{'foo' : 'bar'}")
                    .metadata(new byte[0])
                    .build()
            ).join();
        });

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
        assertEquals(1, events.size());
        assertEquals(2, events.get(0).event.eventNumber);
        assertEquals(eventIds.get(2), events.get(0).event.eventId);
    }

    @Test
    public void subscribesToExistingPersistentSubscriptionWithStartFromBeginningAndEventsInIt() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = "startinbeginning1";

        CountDownLatch eventSignal = new CountDownLatch(1);
        List<UUID> eventIds = new ArrayList<>();
        List<ResolvedEvent> events = new ArrayList<>();

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromBeginning()
            .build();

        range(0, 10).forEach(i -> {
            eventIds.add(UUID.randomUUID());
            eventstore.appendToStream(stream, ExpectedVersion.ANY,
                EventData.newBuilder()
                    .eventId(eventIds.get(i))
                    .type("test")
                    .jsonData("{'foo' : 'bar'}")
                    .metadata(new byte[0])
                    .build()
            ).join();
        });

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, (s, e) -> {
            events.add(e);
            eventSignal.countDown();
        }).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
        assertEquals(0, events.get(0).event.eventNumber);
        assertEquals(eventIds.get(0), events.get(0).event.eventId);
    }

    @Test
    public void subscribesToExistingPersistentSubscriptionWithStartFromBeginningNotSetAndEventsInIt() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = "startinbeginning1";

        CountDownLatch eventSignal = new CountDownLatch(1);

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromCurrent()
            .build();

        range(0, 10).forEach(i ->
            eventstore.appendToStream(stream, ExpectedVersion.ANY,
                EventData.newBuilder()
                    .eventId(UUID.randomUUID())
                    .type("test")
                    .jsonData("{'foo' : 'bar'}")
                    .metadata(new byte[0])
                    .build()
            ).join());

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, (s, e) -> eventSignal.countDown()).join();

        assertFalse("Some event appeared", eventSignal.await(1, SECONDS));
    }

    @Test
    public void subscribesToExistingPersistentSubscriptionWithStartFromBeginningNotSetAndEventsInItThenWritesEvent() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = "startinbeginning1";

        CountDownLatch eventSignal = new CountDownLatch(1);
        UUID eventId = UUID.randomUUID();
        List<ResolvedEvent> events = new ArrayList<>();

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .build();

        range(0, 10).forEach(i ->
            eventstore.appendToStream(stream, ExpectedVersion.ANY,
                EventData.newBuilder()
                    .eventId(UUID.randomUUID())
                    .type("test")
                    .jsonData("{'foo' : 'bar'}")
                    .metadata(new byte[0])
                    .build()
            ).join());

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, (s, e) -> {
            events.add(e);
            eventSignal.countDown();
        }).join();

        eventstore.appendToStream(stream, ExpectedVersion.ANY,
            EventData.newBuilder()
                .eventId(eventId)
                .type("test")
                .jsonData("{'foo' : 'bar'}")
                .metadata(new byte[0])
                .build()
        ).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
        assertEquals(1, events.size());
        assertEquals(10, events.get(0).event.eventNumber);
        assertEquals(eventId, events.get(0).event.eventId);
    }

    @Test
    public void subscribesToExistingPersistentSubscriptionWithStartFromXSetHigherThanXAndEventsInItThenWritesEvent() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = "startinbeginning1";

        CountDownLatch eventSignal = new CountDownLatch(1);
        UUID eventId = UUID.randomUUID();
        List<ResolvedEvent> events = new ArrayList<>();

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFrom(11)
            .build();

        range(0, 11).forEach(i ->
            eventstore.appendToStream(stream, ExpectedVersion.ANY,
                EventData.newBuilder()
                    .eventId(UUID.randomUUID())
                    .type("test")
                    .jsonData("{'foo' : 'bar'}")
                    .metadata(new byte[0])
                    .build()
            ).join());

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, (s, e) -> {
            events.add(e);
            eventSignal.countDown();
        }).join();

        eventstore.appendToStream(stream, ExpectedVersion.ANY,
            EventData.newBuilder()
                .eventId(eventId)
                .type("test")
                .jsonData("{'foo' : 'bar'}")
                .metadata(new byte[0])
                .build()
        ).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
        assertEquals(1, events.size());
        assertEquals(11, events.get(0).event.eventNumber);
        assertEquals(eventId, events.get(0).event.eventId);
    }

    @Test
    public void nakInSubscriptionHandlerInAutoAckModeDropsTheSubscription() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = "naktest";

        CountDownLatch closeSignal = new CountDownLatch(1);
        AtomicReference<SubscriptionDropReason> closeReason = new AtomicReference<>();
        AtomicReference<Exception> closeException = new AtomicReference<>();

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFromBeginning()
            .build();

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, new PersistentSubscriptionListener() {
            @Override
            public void onEvent(PersistentSubscription subscription, ResolvedEvent event) {
                throw new RuntimeException("test");
            }

            @Override
            public void onClose(PersistentSubscription subscription, SubscriptionDropReason reason, Exception exception) {
                closeReason.set(reason);
                closeException.set(exception);
                closeSignal.countDown();
            }
        }).join();

        eventstore.appendToStream(stream, ExpectedVersion.ANY,
            EventData.newBuilder()
                .eventId(UUID.randomUUID())
                .type("test")
                .jsonData("{'foo' : 'bar'}")
                .metadata(new byte[0])
                .build()
        ).join();

        assertTrue("onClose timeout", closeSignal.await(5, SECONDS));
        assertEquals(SubscriptionDropReason.EventHandlerException, closeReason.get());
        assertThat(closeException.get(), instanceOf(RuntimeException.class));
        assertEquals("test", closeException.get().getMessage());
    }

    @Test
    public void subscribesToExistingPersistentSubscriptionWithStartFromXSetAndEventsInItThenWritesEvent() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = "startinbeginning1";

        CountDownLatch eventSignal = new CountDownLatch(1);
        UUID eventId = UUID.randomUUID();
        List<ResolvedEvent> events = new ArrayList<>();

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFrom(10)
            .build();

        range(0, 10).forEach(i ->
            eventstore.appendToStream(stream, ExpectedVersion.ANY,
                EventData.newBuilder()
                    .eventId(UUID.randomUUID())
                    .type("test")
                    .jsonData("{'foo' : 'bar'}")
                    .metadata(new byte[0])
                    .build()
            ).join());

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, (s, e) -> {
            events.add(e);
            eventSignal.countDown();
        }).join();

        eventstore.appendToStream(stream, ExpectedVersion.ANY,
            EventData.newBuilder()
                .eventId(eventId)
                .type("test")
                .jsonData("{'foo' : 'bar'}")
                .metadata(new byte[0])
                .build()
        ).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
        assertEquals(1, events.size());
        assertEquals(10, events.get(0).event.eventNumber);
        assertEquals(eventId, events.get(0).event.eventId);
    }

    @Test
    public void subscribesToExistingPersistentSubscriptionWithStartFromXSetAndEventsInIt() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = "startinx2";

        CountDownLatch eventSignal = new CountDownLatch(1);
        List<UUID> eventIds = new ArrayList<>();
        List<ResolvedEvent> events = new ArrayList<>();

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(false)
            .startFrom(4)
            .build();

        range(0, 10).forEach(i -> {
            eventIds.add(UUID.randomUUID());
            eventstore.appendToStream(stream, ExpectedVersion.ANY,
                EventData.newBuilder()
                    .eventId(eventIds.get(i))
                    .type("test")
                    .jsonData("{'foo' : 'bar'}")
                    .metadata(new byte[0])
                    .build()
            ).join();
        });

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, (s, e) -> {
            events.add(e);
            eventSignal.countDown();
        }).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
        assertEquals(4, events.get(0).event.eventNumber);
        assertEquals(eventIds.get(4), events.get(0).event.eventId);
    }

}
