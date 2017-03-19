package com.github.msemys.esjc.projection;

import com.github.msemys.esjc.*;
import com.github.msemys.esjc.system.SystemProjections;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static com.github.msemys.esjc.util.Strings.newString;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

public class ITProjectionsManager extends AbstractIntegrationTest {

    private ProjectionManager projectionManager;

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        projectionManager = ProjectionManagerBuilder.newBuilder()
            .address("127.0.0.1", 2113)
            .userCredentials("admin", "changeit")
            .operationTimeout(Duration.ofSeconds(20))
            .build();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        projectionManager.shutdown();
    }

    @Test
    public void createsOneTimeProjection() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        int oneTimeProjectionCount = projectionManager.listOneTime().join().size();

        String query = createStandardQuery(stream);

        projectionManager.createOneTime(query).join();

        assertEquals(oneTimeProjectionCount + 1, projectionManager.listOneTime().join().size());
    }

    @Test
    public void createsNamedOneTimeProjection() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        int oneTimeProjectionCount = projectionManager.listOneTime().join().size();

        projectionManager.createOneTime(projection, query).join();

        assertEquals(oneTimeProjectionCount + 1, projectionManager.listOneTime().join().size());

        Projection result = projectionManager.getStatus(projection).join();
        assertNotNull(result);
    }

    @Test
    public void createsTransientProjection() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.createTransient(projection, query).join();

        Projection result = projectionManager.getStatus(projection).join();

        assertNotNull(result);
    }

    @Test
    public void createsContinuousProjection() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String emittedStream = "emittedStream-" + stream;
        String query = createEmittingQuery(stream, emittedStream);

        projectionManager.createContinuous(projection, query).join();

        Projection result = projectionManager.listContinuous().join().stream()
            .filter(p -> p.effectiveName.equals(projection))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(String.format("Projection '%s' not found", projection)));

        assertNotNull(result);

        EventReadResult event = eventstore.readEvent("$projections-" + result.name, 0, true).join();

        assertThat(newString(event.event.event.data), containsString("\"emitEnabled\": true"));
    }

    @Test
    public void createsContinuousProjectionWithTrackEmittedStreams() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String emittedStream = "emittedStream-" + stream;
        String query = createEmittingQuery(stream, emittedStream);

        projectionManager.createContinuous(projection, query, true).join();

        Projection result = projectionManager.listContinuous().join().stream()
            .filter(p -> p.effectiveName.equals(projection))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(String.format("Projection '%s' not found", projection)));

        assertNotNull(result);

        EventReadResult event = eventstore.readEvent("$projections-" + result.name, 0, true).join();

        assertThat(newString(event.event.event.data), containsString("\"trackEmittedStreams\": true"));
    }

    @Test
    public void disablesProjection() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.createContinuous(projection, query).join();
        projectionManager.disable(projection).join();

        Projection result = projectionManager.getStatus(projection).join();

        assertNotNull(result);
        assertThat(result.status, containsString("Stopped"));
    }

    @Test
    public void enablesProjection() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.createContinuous(projection, query).join();
        projectionManager.disable(projection).join();
        projectionManager.enable(projection).join();

        Projection result = projectionManager.getStatus(projection).join();

        assertNotNull(result);
        assertEquals("Running", result.status);
    }

    @Test
    public void abortsProjection() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.createContinuous(projection, query).join();
        projectionManager.abort(projection).join();

        Projection result = projectionManager.getStatus(projection).join();

        assertNotNull(result);
        assertThat(result.status, containsString("Aborted"));
    }

    @Test
    public void listsAllProjections() {
        List<Projection> projections = projectionManager.listAll().join();
        assertFalse(projections.isEmpty());
    }

    @Test
    public void listsOneTimeProjections() {
        String query = createStandardQuery(UUID.randomUUID().toString());

        projectionManager.createOneTime(query).join();

        List<Projection> projections = projectionManager.listOneTime().join();
        assertFalse(projections.isEmpty());
    }

    @Test
    public void listsContinuousProjections() {
        String query = createStandardQuery(UUID.randomUUID().toString());
        String projection = "projection-" + UUID.randomUUID().toString();

        projectionManager.createContinuous(projection, query).join();

        assertTrue(projectionManager.listContinuous().join().stream()
            .anyMatch(p -> p.effectiveName.equals(projection)));
    }

    @Test
    public void getsProjectionStateWhileRunning() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.createContinuous(projection, query).join();

        String state = projectionManager.getState(projection).join();

        assertFalse(isNullOrEmpty(state));
    }

    @Test
    public void getsProjectionStatusWhileRunning() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.createContinuous(projection, query).join();

        Projection result = projectionManager.getStatus(projection).join();

        assertNotNull(result);
    }

    @Test
    public void getsProjectionResultWhileRunning() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.createContinuous(projection, query).join();

        String result = projectionManager.getResult(projection).join();

        assertEquals("{\"count\":1}", result);
    }

    @Test
    public void getsProjectionQueryWhileRunning() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.createContinuous(projection, query).join();

        String result = projectionManager.getQuery(projection).join();

        assertEquals(query, result);
    }

    @Test
    @Ignore
    public void getsPartitionStateAndResult() {
        final String stream1 = "account-" + UUID.randomUUID().toString().replace("-", "");
        final String stream2 = "account-" + UUID.randomUUID().toString().replace("-", "");

        eventstore.appendToStream(stream1, ExpectedVersion.NO_STREAM, asList(
            credited(1.5f),
            debited(3f),
            credited(1.5f),
            credited(19.82f),
            credited(0.18f)
        )).join();

        eventstore.appendToStream(stream2, ExpectedVersion.NO_STREAM, asList(
            credited(8.17f),
            credited(1.83f),
            debited(5.0f),
            credited(10f),
            credited(25f),
            debited(15f),
            credited(74.99f)
        )).join();

        String projection = "account-details-" + UUID.randomUUID().toString();
        String query = "fromCategory(\"account\")                      \n"
            + "    .foreachStream()                                    \n"
            + "    .when({                                             \n"
            + "          \"$init\":    function(s, e) {                \n"
            + "                            return { balance: 0, creditCount: 0, debitCount: 0 }; \n"
            + "                        },                              \n"
            + "          \"Credited\": function(s, e) {                \n"
            + "                            s.balance += e.body.amount; \n"
            + "                            s.creditCount++;            \n"
            + "                        },                              \n"
            + "          \"Debited\":  function(s, e) {                \n"
            + "                            s.balance -= e.body.amount; \n"
            + "                            s.debitCount++;             \n"
            + "                        }                               \n"
            + "    })                                                  \n"
            + "    .transformBy(function(s, e) {                       \n"
            + "            return {\"availableBalance\": s.balance };  \n"
            + "    });                                                 \n";

        try {
            projectionManager.enable(SystemProjections.BY_CATEGORY).join();
            projectionManager.createContinuous(projection, query).join();

            boolean completed = projectionManager.awaitStatus(projection, p -> p.eventsProcessedAfterRestart >= 12, Duration.ofSeconds(30));
            assertTrue("Projection '" + projection + "' is not completed", completed);

            String state1 = projectionManager.getPartitionState(projection, stream1).join();
            assertEquals("{\"balance\":20,\"creditCount\":4,\"debitCount\":1}", state1);

            String state2 = projectionManager.getPartitionState(projection, stream2).join();
            assertEquals("{\"balance\":99.99,\"creditCount\":5,\"debitCount\":2}", state2);

            String result1 = projectionManager.getPartitionResult(projection, stream1).join();
            assertEquals("{\"availableBalance\":20}", result1);

            String result2 = projectionManager.getPartitionResult(projection, stream2).join();
            assertEquals("{\"availableBalance\":99.99}", result2);
        } finally {
            projectionManager.disable(SystemProjections.BY_CATEGORY).join();
        }
    }

    @Test
    public void getsProjectionStatistics() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.createContinuous(projection, query).join();

        String statistics = projectionManager.getStatistics(projection).join();

        assertFalse(isNullOrEmpty(statistics));
    }

    @Test
    public void updatesProjectionQuery() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String originalQuery = createStandardQuery(stream);
        String newQuery = createStandardQuery("DifferentStream");

        projectionManager.createContinuous(projection, originalQuery).join();
        projectionManager.updateQuery(projection, newQuery).join();

        String result = projectionManager.getQuery(projection).join();

        assertEquals(newQuery, result);
    }

    @Test
    public void deletesProjection() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.createContinuous(projection, query).join();
        assertTrue(projectionManager.listAll().join().stream().anyMatch(p -> p.name.equals(projection)));

        projectionManager.disable(projection).join();
        projectionManager.delete(projection).join();
        assertFalse(projectionManager.listAll().join().stream().anyMatch(p -> p.name.equals(projection)));
    }

    @Test
    public void waitsForProjectionCompletedStatus() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvents(20)).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.createTransient(projection, query);

        boolean completed = projectionManager.awaitStatus(projection, p -> p.status.contains("Completed"), Duration.ofSeconds(30));
        assertTrue("Projection '" + projection + "' is not completed", completed);

        Projection result = projectionManager.getStatus(projection).join();

        assertThat(result.status, containsString("Completed"));
    }

    private static String createStandardQuery(String stream) {
        return "fromStream(\"" + stream + "\")  \n"
            + "  .when({                        \n"
            + "    \"$any\":function(s,e) {     \n"
            + "                s.count = 1;     \n"
            + "                return s;        \n"
            + "             }                   \n"
            + "  });";
    }

    private static String createEmittingQuery(String stream, String emittingStream) {
        return "fromStream(\"" + stream + "\")  \n"
            + "     .when({                     \n"
            + "       \"$any\":function(s,e) {  \n"
            + "                   emit(\"" + emittingStream + "\", \"emittedEvent\", e); \n"
            + "                }                \n"
            + "     });";
    }

    private static EventData credited(float amount) {
        return EventData.newBuilder()
            .type("Credited")
            .jsonData(String.format("{ \"amount\": %.2f }", amount))
            .build();
    }

    private static EventData debited(float amount) {
        return EventData.newBuilder()
            .type("Debited")
            .jsonData(String.format("{ \"amount\": %.2f }", amount))
            .build();
    }

}
