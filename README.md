# EventStore Java Client 

[![Build Status](https://app.travis-ci.com/msemys/esjc.svg?branch=master)](https://app.travis-ci.com/msemys/esjc)
[![Version](https://img.shields.io/maven-central/v/com.github.msemys/esjc.svg?label=version)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.msemys%22%20AND%20a%3A%22esjc%22)
[![Javadocs](https://www.javadoc.io/badge/com.github.msemys/esjc.svg)](https://www.javadoc.io/doc/com.github.msemys/esjc) 

This is [EventStore](https://geteventstore.com/) driver for Java, that uses [Netty](http://netty.io/) for network communication and [GSON](https://github.com/google/gson) for object serialization/deserialization to JSON (e.g.: stream metadata, cluster information dto). Client logic implementation is the same as in the original client for .NET platform.

## Requirements

* Java 8
* EventStore 4.0.0 - 5.0.11 (esjc v2)
* EventStore 3.2.0 - 3.9.4 (esjc v1)


## Maven Dependency

* EventStore v4 (or higher) compatible
```xml
<dependency>
    <groupId>com.github.msemys</groupId>
    <artifactId>esjc</artifactId>
    <version>2.6.0</version>
</dependency>
```

* EventStore v3 compatible
```xml
<dependency>
    <groupId>com.github.msemys</groupId>
    <artifactId>esjc</artifactId>
    <version>1.8.1</version>
</dependency>
```


## Build and run tests
```
$ ./scripts/generate-ssl-cert.sh
$ docker-compose up -d
$ mvn clean verify
```

## Usage

### Creating a Client Instance

Client instances are created using a builder class. The examples below demonstrate how to create default client with singe-node and cluster-node configuration.


* creates a single-node client

```java
EventStore eventstore = EventStoreBuilder.newBuilder()
    .singleNodeAddress("127.0.0.1", 1113)
    .userCredentials("admin", "changeit")
    .build();
```

* creates a cluster-node client (using gossip seeds)

```java
EventStore eventstore = EventStoreBuilder.newBuilder()
    .clusterNodeUsingGossipSeeds(cluster -> cluster
        .gossipSeedEndpoints(asList(
            new InetSocketAddress("127.0.0.1", 2113),
            new InetSocketAddress("127.0.0.1", 2213),
            new InetSocketAddress("127.0.0.1", 2313))))
    .userCredentials("admin", "changeit")
    .build();
```

* creates a cluster-node client (using dns)

```java
EventStore eventstore = EventStoreBuilder.newBuilder()
    .clusterNodeUsingDns(cluster -> cluster.dns("mycluster.com"))
    .userCredentials("admin", "changeit")
    .build();
```

Driver uses full-duplex communication channel to server. It is recommended that only one instance per application is created.

### SSL

In order to use secure channel between the client and server, first of all we need to enable SSL on server side by providing TCP secure port and server certificate.

* create private key file and self-signed certificate request (for testing purposes)

```
openssl req \
  -x509 -sha256 -nodes -days 365 -subj "/CN=test.com" \
  -newkey rsa:2048 -keyout domain.pem -out domain.csr
```

* export private key file and self-signed certificate request to PKCS#12 archive

```
openssl pkcs12 -export -inkey domain.pem -in domain.csr -out domain.p12
```

* start server with encrypted TCP connection 

```
./run-node.sh --ext-secure-tcp-port 1119 --certificate-file domain.p12
```

Now we are ready to connect to single-node or cluster-node using secure channel. On the client side we are able to
verify server certificate (check either signing certificate or CN and expiration date only) or accept any server
certificate without verification.

```java
// creates a client with secure connection to server whose certificate is trusted by the given certificate file
EventStore eventstore = EventStoreBuilder.newBuilder()
    .singleNodeAddress("127.0.0.1", 1119)
    .useSslConnection(new File("domain.csr"))
    .userCredentials("admin", "changeit")
    .build();

// creates a client with secure connection to server whose certificate Common Name (CN) matches 'test.com'
EventStore eventstore = EventStoreBuilder.newBuilder()
    .singleNodeAddress("127.0.0.1", 1119)
    .useSslConnection("test.com")
    .userCredentials("admin", "changeit")
    .build();

// creates a client with secure connection to server without certificate verification
EventStore eventstore = EventStoreBuilder.newBuilder()
    .singleNodeAddress("127.0.0.1", 1119)
    .useSslConnection()
    .userCredentials("admin", "changeit")
    .build();
```

### API Examples

All operations are handled fully asynchronously and returns [`CompletableFuture<T>`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html). For asynchronous result handling you could use [`whenComplete((result, throwable) -> { ... })`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html#whenComplete-java.util.function.BiConsumer-) or [`thenAccept(result -> { ... })`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html#thenAccept-java.util.function.Consumer-) methods on created future object. To handle result synchronously simply use [`get()`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html#get--) or [`join()`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html#join--) methods on future object.

```java
// handles result asynchronously
eventstore.appendToStream("foo", ExpectedVersion.ANY, asList(
    EventData.newBuilder().type("bar").jsonData("{ a : 1 }").build(),
    EventData.newBuilder().type("baz").jsonData("{ b : 2 }").build())
).thenAccept(r -> System.out.println(r.logPosition));

// handles result synchronously
eventstore.appendToStream("foo", ExpectedVersion.ANY, asList(
    EventData.newBuilder().type("bar").jsonData("{ a : 1 }").build(),
    EventData.newBuilder().type("baz").jsonData("{ b : 2 }").build())
).thenAccept(r -> System.out.println(r.logPosition)).get();
```

#### Writing events

```java
eventstore.appendToStream("foo", ExpectedVersion.ANY, asList(
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
        .build())
).thenAccept(r -> System.out.println(r.logPosition));
```

```java
eventstore.appendToStream("foo", 2,
    EventData.newBuilder()
        .type("quux")
        .data(new byte[0])
        .metadata(new byte[0])
        .build()
).thenAccept(r -> System.out.println(r.logPosition));
```

```java
eventstore.tryAppendToStream("foo", ExpectedVersion.ANY,
    EventData.newBuilder()
        .type("corge")
        .build()
).thenAccept(r -> {
    if (r.status == WriteStatus.Success) {
        System.out.println(r.logPosition);
    } else {
        System.err.println(r.status);
    }
});
```

#### Transactional writes

```java
try (Transaction t = eventstore.startTransaction("foo", ExpectedVersion.ANY).get()) {
    t.write(EventData.newBuilder().type("bar").jsonData("{ a : 1 }").build());
    t.write(EventData.newBuilder().type("baz").jsonData("{ b : 2 }").build());
    t.write(asList(
        EventData.newBuilder().type("qux").jsonData("{ c : 3 }").build(),
        EventData.newBuilder().type("quux").jsonData("{ d : 4 }").build())
    );
    t.commit();
} catch (Exception e) {
    e.printStackTrace();
}
```

```java
eventstore.startTransaction("foo", ExpectedVersion.ANY).thenAccept(t -> {
    t.write(EventData.newBuilder().type("bar").jsonData("{ a : 1 }").build());
    t.write(EventData.newBuilder().type("baz").jsonData("{ b : 2 }").build());
    t.write(asList(
        EventData.newBuilder().type("qux").jsonData("{ c : 3 }").build(),
        EventData.newBuilder().type("quux").jsonData("{ d : 4 }").build())
    );
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

#### Iterates over stream events forward to the end of stream

```java
eventstore.iterateStreamEventsForward("foo", 10, 500, false).forEachRemaining(e ->
    System.out.format("#%d  id: '%s'; type: '%s'; data: '%s'\n",
        e.originalEvent().eventNumber,
        e.originalEvent().eventId,
        e.originalEvent().eventType,
        new String(e.originalEvent().data)));
```

#### Iterates over stream events backward to the beginning of stream

```java
eventstore.iterateStreamEventsBackward("foo", 10, 500, false).forEachRemaining(e ->
    System.out.format("#%d  id: '%s'; type: '%s'; data: '%s'\n",
        e.originalEvent().eventNumber,
        e.originalEvent().eventId,
        e.originalEvent().eventType,
        new String(e.originalEvent().data)));
```

#### Iterates over all events forward to the end of ALL stream

```java
eventstore.iterateAllEventsForward(Position.START, 500, false).forEachRemaining(e ->
    System.out.format("@%s  id: '%s'; type: '%s'; data: '%s'\n",
        e.originalPosition,
        e.originalEvent().eventId,
        e.originalEvent().eventType,
        new String(e.originalEvent().data)));
```

#### Iterates over all events backward to the beginning of ALL stream

```java
eventstore.iterateAllEventsBackward(Position.END, 500, false).forEachRemaining(e ->
    System.out.format("@%s  id: '%s'; type: '%s'; data: '%s'\n",
        e.originalPosition,
        e.originalEvent().eventId,
        e.originalEvent().eventType,
        new String(e.originalEvent().data)));
```

#### Streaming events forward to the end of stream

```java
eventstore.streamEventsForward("foo", 10, 500, false)
    .filter(e -> e.originalEvent().eventType.equals("bar"))
    .forEach(e -> System.out.format("#%d  id: '%s'; type: '%s'; data: '%s'\n",
        e.originalEvent().eventNumber,
        e.originalEvent().eventId,
        e.originalEvent().eventType,
        new String(e.originalEvent().data)));
```

#### Streaming events backward to the beginning of stream

```java
eventstore.streamEventsBackward("foo", 10, 500, false)
    .filter(e -> e.originalEvent().eventType.equals("bar"))
    .forEach(e -> System.out.format("#%d  id: '%s'; type: '%s'; data: '%s'\n",
        e.originalEvent().eventNumber,
        e.originalEvent().eventId,
        e.originalEvent().eventType,
        new String(e.originalEvent().data)));
```

#### Streaming all events forward to the end of ALL stream

```java
eventstore.streamAllEventsForward(Position.START, 500, false)
    .filter(e -> !e.originalEvent().eventType.startsWith("$"))
    .forEach(e -> System.out.format("@%s  id: '%s'; type: '%s'; data: '%s'\n",
        e.originalPosition,
        e.originalEvent().eventId,
        e.originalEvent().eventType,
        new String(e.originalEvent().data)));
```

#### Streaming all events backward to the beginning of ALL stream

```java
eventstore.streamAllEventsBackward(Position.END, 500, false)
    .filter(e -> !e.originalEvent().eventType.startsWith("$"))
    .forEach(e -> System.out.format("@%s  id: '%s'; type: '%s'; data: '%s'\n",
        e.originalPosition,
        e.originalEvent().eventId,
        e.originalEvent().eventType,
        new String(e.originalEvent().data)));
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
CatchUpSubscription catchupSubscription = eventstore.subscribeToStreamFrom("foo", 3L,
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
CatchUpSubscription catchupSubscription = eventstore.subscribeToStreamFrom("foo", 3L, (s, e) ->
    System.out.println(e.originalEvent().eventType)
);

catchupSubscription.close();
```

#### Subscribes to ALL stream from event position (catch-up subscription)

```java
CatchUpSubscription catchupSubscription = eventstore.subscribeToAllFrom(Position.START,
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
CatchUpSubscription catchupSubscription = eventstore.subscribeToAllFrom(Position.of(1, 1), (s, e) ->
    System.out.println(e.originalEvent().eventType)
);

catchupSubscription.close();
```

#### Subscribes to persistent subscription

```java
CompletableFuture<PersistentSubscription> persistentSubscription = eventstore.subscribeToPersistent("foo", "group", 
    new PersistentSubscriptionListener() {
        @Override
        public void onEvent(PersistentSubscription subscription, RetryableResolvedEvent event) {
            System.out.println(event.originalEvent().eventType);
        }
    
        @Override
        public void onClose(PersistentSubscription subscription, SubscriptionDropReason reason, Exception exception) {
            System.out.println("Subscription closed: " + reason);
        }
    });

persistentSubscription.get().close();
```

```java
CompletableFuture<PersistentSubscription> persistentSubscription = eventstore.subscribeToPersistent("foo", "group", (s, e) ->
    System.out.println(e.originalEvent().eventType)
);

persistentSubscription.get().stop(Duration.ofSeconds(3));
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
eventstore.deleteStream("bar", ExpectedVersion.ANY).thenAccept(r -> System.out.println(r.logPosition));
```

#### Sets stream metadata

```java
eventstore.setStreamMetadata("foo", ExpectedVersion.ANY, StreamMetadata.newBuilder()
    .aclReadRoles(asList("eric", "kyle", "stan", "kenny"))
    .cacheControl(Duration.ofMinutes(10))
    .maxAge(Duration.ofDays(1))
    .customProperty("baz", "dummy text")
    .customProperty("bar", 2)
    .customProperty("quux", 3.4)
    .customProperty("quuux", true)
    .build()
).thenAccept(r -> System.out.println(r.logPosition));
```

```java
eventstore.setStreamMetadata("foo", ExpectedVersion.ANY, StreamMetadata.empty())
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
StreamAcl userStreamAcl = StreamAcl.newBuilder()
    .readRoles(asList("eric", "kyle", "stan", "kenny"))
    .writeRoles(asList("butters"))
    .deleteRoles(asList("$admins"))
    .metaReadRoles(asList("victoria", "mackey"))
    .metaWriteRoles(asList("randy"))
    .build();

StreamAcl systemStreamAcl = StreamAcl.newBuilder()
    .readRoles(asList("$admins"))
    .writeRoles(asList("$all"))
    .deleteRoles(asList("$admins"))
    .metaWriteRoles(asList("$all"))
    .build();

eventstore.setSystemSettings(SystemSettings.newBuilder()
    .userStreamAcl(userStreamAcl)
    .systemStreamAcl(systemStreamAcl)
    .build()
).thenAccept(r -> System.out.println(r.logPosition));
```
