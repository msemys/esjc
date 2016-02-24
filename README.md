# EventStore Java Client [![Build Status](https://api.travis-ci.org/msemys/esjc.svg)](https://travis-ci.org/msemys/esjc) [![Version](https://img.shields.io/maven-central/v/com.github.msemys/esjc.svg?label=version)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.msemys%22%20AND%20a%3A%22esjc%22) [![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.github.msemys/esjc/badge.svg)](http://www.javadoc.io/doc/com.github.msemys/esjc) 

This is [EventStore](https://geteventstore.com/) driver for Java, that uses [Netty](http://netty.io/) for network communication and [GSON](https://github.com/google/gson) for object serialization/deserialization to JSON (e.g.: stream metadata, cluster information dto). Client logic implementation is the same as in the original client for .NET platform.

NOTE: connection encryption using SSL is not implemented yet.

## Requirements

* Java 8
* EventStore Server >= 3.2.0 (tested with 3.3.1, 3.4.0)


## Maven Dependency

```.xml
<dependency>
    <groupId>com.github.msemys</groupId>
    <artifactId>esjc</artifactId>
    <version>1.2.0</version>
</dependency>
```


## Usage

### Creating a Client Instance

There are two ways to create a new client instance. The examples below demonstrate how to create default client with singe-node and cluster-node configuration in both ways.


* creates a client using builder class

```java
EventStore eventstore = EventStoreBuilder.newBuilder()
    .singleNodeAddress("127.0.0.1", 1113)
    .userCredentials("admin", "changeit")
    .build();
```

```java
EventStore eventstore = EventStoreBuilder.newBuilder()
    .clusterNodeDiscoveryFromGossipSeeds(asList(
        new InetSocketAddress("127.0.0.1", 2113),
        new InetSocketAddress("127.0.0.1", 2213),
        new InetSocketAddress("127.0.0.1", 2313)))
    .userCredentials("admin", "changeit")
    .build();
```

* creates a client by calling constructor and passing settings instance

```java
EventStore eventstore = new EventStore(Settings.newBuilder()
    .nodeSettings(StaticNodeSettings.newBuilder()
        .address("127.0.0.1", 1113)
        .build())
    .userCredentials("admin", "changeit")
    .build());
```

```java
EventStore eventstore = new EventStore(Settings.newBuilder()
    .nodeSettings(ClusterNodeSettings.forGossipSeedDiscoverer()
        .gossipSeedEndpoints(asList(
            new InetSocketAddress("127.0.0.1", 2113),
            new InetSocketAddress("127.0.0.1", 2213),
            new InetSocketAddress("127.0.0.1", 2313)))
        .build())
    .userCredentials("admin", "changeit")
    .build());
```

Driver uses full-duplex communication channel to server. It is recommended that only one instance per application is created.

### API Examples

All operations are handled fully asynchronously and returns [`CompletableFuture<T>`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html). For asynchronous result handling you could use [`whenComplete((result, throwable) -> { ... })`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html#whenComplete-java.util.function.BiConsumer-) or [`thenAccept(result -> { ... })`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html#thenAccept-java.util.function.Consumer-) methods on created future object. To handle result synchronously simply use [`get()`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html#get--) method on future object.

```java
// handles result asynchronously
eventstore.appendToStream("foo", ExpectedVersion.any(), asList(
    EventData.newBuilder().type("bar").jsonData("{ a : 1 }").build(),
    EventData.newBuilder().type("baz").jsonData("{ b : 2 }").build())
).thenAccept(r -> System.out.println(r.logPosition));

// handles result synchronously
eventstore.appendToStream("foo", ExpectedVersion.any(), asList(
    EventData.newBuilder().type("bar").jsonData("{ a : 1 }").build(),
    EventData.newBuilder().type("baz").jsonData("{ b : 2 }").build())
).thenAccept(r -> System.out.println(r.logPosition)).get();
```

#### Writing events

```java
eventstore.appendToStream("foo", ExpectedVersion.any(), asList(
    EventData.newBuilder()
        .type("bar")
        .data(new byte[]{1, 2, 3, 4, 5})
        .metadata(new byte[]{6, 7, 8, 9, 0})
        .build(),
    EventData.newBuilder()
        .eventId(UUID.randomUUID())
        .type("baz")
        .data("dummy content")
        .build(),
    EventData.newBuilder()
        .type("qux")
        .jsonData("{ a : 1 }")
        .build()))
    .thenAccept(r -> System.out.println(r.logPosition));
```

#### Transactional writes

```java
try (Transaction t = eventstore.startTransaction("foo", ExpectedVersion.any()).get()) {
    t.write(asList(EventData.newBuilder().type("bar").jsonData("{ a : 1 }").build()));
    t.write(asList(EventData.newBuilder().type("baz").jsonData("{ b : 2 }").build()));
    t.commit();
} catch (Exception e) {
    e.printStackTrace();
}
```

```java
eventstore.startTransaction("foo", ExpectedVersion.any()).thenAccept(t -> {
    t.write(asList(EventData.newBuilder().type("bar").jsonData("{ a : 1 }").build()));
    t.write(asList(EventData.newBuilder().type("baz").jsonData("{ b : 2 }").build()));
    t.rollback();
});
```

#### Reading a single event

```java
eventstore.readEvent("foo", 1, false).thenAccept(e ->
    System.out.format("id: '%s'; type: '%s'; data: '%s'",
        e.event.originalEvent().eventId,
        e.event.originalEvent().eventType,
        e.event.originalEvent().data));
```

#### Reading stream events forwards

```java
eventstore.readStreamEventsForward("foo", 10, 5, false).thenAccept(e ->
    e.events.forEach(i -> System.out.format("#%d  id: '%s'; type: '%s'; data: '%s'\n",
        i.originalEvent().eventNumber,
        i.originalEvent().eventId,
        i.originalEvent().eventType,
        new String(i.originalEvent().data))));
```

#### Reading stream events backwards

```java
eventstore.readStreamEventsBackward("foo", 10, 5, false).thenAccept(e ->
    e.events.forEach(i -> System.out.format("#%d  id: '%s'; type: '%s'; data: '%s'\n",
        i.originalEvent().eventNumber,
        i.originalEvent().eventId,
        i.originalEvent().eventType,
        new String(i.originalEvent().data))));
```

#### Reading all events forwards

```java
eventstore.readAllEventsForward(Position.START, 10, false).thenAccept(e ->
    e.events.forEach(i -> System.out.format("@%s  id: '%s'; type: '%s'; data: '%s'\n",
        i.originalPosition,
        i.originalEvent().eventId,
        i.originalEvent().eventType,
        new String(i.originalEvent().data))));
```

#### Reading all events backwards

```java
eventstore.readAllEventsBackward(Position.END, 10, false).thenAccept(e ->
    e.events.forEach(i -> System.out.format("@%s  id: '%s'; type: '%s'; data: '%s'\n",
        i.originalPosition,
        i.originalEvent().eventId,
        i.originalEvent().eventType,
        new String(i.originalEvent().data))));
```

#### Subscribes to stream (volatile subscription)

```java
CompletableFuture<Subscription> volatileSubscription = eventstore.subscribeToStream("foo", false,
    new VolatileSubscriptionListener() {
        @Override
        public void onEvent(Subscription subscription, ResolvedEvent event) {
            System.out.println(event.originalEvent().eventType);
        }

        @Override
        public void onClose(Subscription subscription, SubscriptionDropReason reason, Exception exception) {
            System.out.println("Subscription closed: " + reason);
        }
    });

volatileSubscription.get().close();
```

```java
CompletableFuture<Subscription> volatileSubscription = eventstore.subscribeToStream("foo", false, (s, e) ->
    System.out.println(e.originalEvent().eventType)
);

volatileSubscription.get().close();
```

#### Subscribes to ALL stream (volatile subscription)

```java
CompletableFuture<Subscription> volatileSubscription = eventstore.subscribeToAll(false,
    new VolatileSubscriptionListener() {
        @Override
        public void onEvent(Subscription subscription, ResolvedEvent event) {
            System.out.println(event.originalEvent().eventType);
        }

        @Override
        public void onClose(Subscription subscription, SubscriptionDropReason reason, Exception exception) {
            System.out.println("Subscription closed: " + reason);
        }
    });

volatileSubscription.get().close();
```

```java
CompletableFuture<Subscription> volatileSubscription = eventstore.subscribeToAll(false, (s, e) -> 
    System.out.println(e.originalEvent().eventType)
);

volatileSubscription.get().close();
```

#### Subscribes to stream from event number (catch-up subscription)

```java
CatchUpSubscription catchupSubscription = eventstore.subscribeToStreamFrom("foo", 3, false,
    new CatchUpSubscriptionListener() {
        @Override
        public void onLiveProcessingStarted(CatchUpSubscription subscription) {
            System.out.println("Live processing started!");
        }

        @Override
        public void onEvent(CatchUpSubscription subscription, ResolvedEvent event) {
            System.out.println(event.originalEvent().eventType);
        }

        @Override
        public void onClose(CatchUpSubscription subscription, SubscriptionDropReason reason, Exception exception) {
            System.out.println("Subscription closed: " + reason);
        }
    });

catchupSubscription.close();
```

```java
CatchUpSubscription catchupSubscription = eventstore.subscribeToStreamFrom("foo", 3, false, (s, e) ->
    System.out.println(e.originalEvent().eventType)
);

catchupSubscription.close();
```

#### Subscribes to ALL stream from event position (catch-up subscription)

```java
CatchUpSubscription catchupSubscription = eventstore.subscribeToAllFrom(Position.START, false,
    new CatchUpSubscriptionListener() {
        @Override
        public void onLiveProcessingStarted(CatchUpSubscription subscription) {
            System.out.println("Live processing started!");
        }

        @Override
        public void onEvent(CatchUpSubscription subscription, ResolvedEvent event) {
            System.out.println(event.originalEvent().eventType);
        }

        @Override
        public void onClose(CatchUpSubscription subscription, SubscriptionDropReason reason, Exception exception) {
            System.out.println("Subscription closed: " + reason);
        }
    });

catchupSubscription.close();
```

```java
CatchUpSubscription catchupSubscription = eventstore.subscribeToAllFrom(Position.of(1, 1), false, (s, e) ->
    System.out.println(e.originalEvent().eventType)
);

catchupSubscription.close();
```

#### Subscribes to persistent subscription

```java
PersistentSubscription persistentSubscription = eventstore.subscribeToPersistent("foo", "group", 
    new PersistentSubscriptionListener() {
        @Override
        public void onEvent(PersistentSubscription subscription, ResolvedEvent event) {
            System.out.println(event.originalEvent().eventType);
        }
    
        @Override
        public void onClose(PersistentSubscription subscription, SubscriptionDropReason reason, Exception exception) {
            System.out.println("Subscription closed: " + reason);
        }
    });

persistentSubscription.close();
```

```java
PersistentSubscription persistentSubscription = eventstore.subscribeToPersistent("foo", "group", (s, e) ->
    System.out.println(e.originalEvent().eventType)
);

persistentSubscription.stop(Duration.ofSeconds(3));
```

#### Creates persistent subscription

```java
eventstore.createPersistentSubscription("foo", "group", PersistentSubscriptionSettings.newBuilder()
    .resolveLinkTos(false)
    .historyBufferSize(20)
    .liveBufferSize(10)
    .minCheckPointCount(10)
    .maxCheckPointCount(1000)
    .checkPointAfter(Duration.ofSeconds(2))
    .maxRetryCount(500)
    .maxSubscriberCount(5)
    .messageTimeout(Duration.ofSeconds(30))
    .readBatchSize(500)
    .startFromCurrent()
    .timingStatistics(false)
    .namedConsumerStrategy(SystemConsumerStrategy.ROUND_ROBIN)
    .build()
).thenAccept(r -> System.out.println(r.status));
```

```java
eventstore.createPersistentSubscription("bar", "group").thenAccept(r -> System.out.println(r.status));
```

#### Updates persistent subscription

```java
eventstore.updatePersistentSubscription("foo", "group", PersistentSubscriptionSettings.newBuilder()
    .maxRetryCount(200)
    .readBatchSize(100)
    .build()
).thenAccept(r -> System.out.println(r.status));
```

#### Deletes persistent subscription

```java
eventstore.deletePersistentSubscription("bar", "group").thenAccept(r -> System.out.println(r.status));
```

#### Deletes stream

```java
eventstore.deleteStream("bar", ExpectedVersion.any()).thenAccept(r -> System.out.println(r.logPosition));
```

#### Sets stream metadata

```java
eventstore.setStreamMetadata("foo", ExpectedVersion.any(), StreamMetadata.newBuilder()
    .aclReadRoles(asList("eric", "kyle", "stan", "kenny"))
    .cacheControl(Duration.ofMinutes(10))
    .maxAge(Duration.ofDays(1))
    .customProperty("baz", "1")
    .customProperty("bar", "2")
    .build()
).thenAccept(r -> System.out.println(r.logPosition));
```

```java
eventstore.setStreamMetadata("foo", ExpectedVersion.any(), StreamMetadata.empty())
    .thenAccept(r -> System.out.println(r.logPosition));
```

#### Gets stream metadata

```java
eventstore.getStreamMetadata("foo").thenAccept(r ->
    System.out.format("deleted: %s, version: %s, stream: %s\nmetadata: %s\n",
        r.isStreamDeleted,
        r.metastreamVersion,
        r.stream,
        r.streamMetadata.toJson()));
```

```java
eventstore.getStreamMetadataAsRawBytes("foo").thenAccept(r ->
    System.out.format("deleted: %s, version: %s, stream: %s\nmetadata-bytes: %s\n",
        r.isStreamDeleted,
        r.metastreamVersion,
        r.stream,
        r.streamMetadata));
```

#### Sets system settings

```java
StreamAcl userStreamAcl = new StreamAcl(
    asList("eric", "kyle", "stan", "kenny"),
    asList("butters"),
    asList("$admins"),
    asList("victoria", "mackey"),
    asList("randy"));

StreamAcl systemStreamAcl = new StreamAcl(
    asList("$admins"),
    asList("$all"),
    asList("$admins"),
    null,
    asList("$all"));

eventstore.setSystemSettings(new SystemSettings(userStreamAcl, systemStreamAcl))
    .thenAccept(r -> System.out.println(r.logPosition));
```
