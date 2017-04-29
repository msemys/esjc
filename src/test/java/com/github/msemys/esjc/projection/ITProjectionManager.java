package com.github.msemys.esjc.projection;

import com.github.msemys.esjc.*;
import com.github.msemys.esjc.http.HttpOperationTimedOutException;
import com.github.msemys.esjc.rule.Retryable;
import com.github.msemys.esjc.rule.RetryableMethodRule;
import com.github.msemys.esjc.system.SystemProjections;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.github.msemys.esjc.projection.ProjectionMode.*;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static com.github.msemys.esjc.util.Strings.newString;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITProjectionManager extends AbstractIntegrationTest {

    private ProjectionManager projectionManager;

    @Rule
    public RetryableMethodRule retryableMethodRule = new RetryableMethodRule();

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

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        int oneTimeProjectionCount = projectionManager.findByMode(ONE_TIME).join().size();

        projectionManager.create(projection, query, ONE_TIME).join();

        assertEquals(oneTimeProjectionCount + 1, projectionManager.findByMode(ONE_TIME).join().size());

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

        projectionManager.create(projection, query, TRANSIENT).join();

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

        projectionManager.create(projection, query, CONTINUOUS).join();

        Projection result = projectionManager.findByMode(CONTINUOUS).join().stream()
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

        projectionManager.create(projection, query, ProjectionSettings.newBuilder()
            .mode(CONTINUOUS)
            .emit(true)
            .trackEmittedStreams(true)
            .build()
        ).join();

        Projection result = projectionManager.findByMode(CONTINUOUS).join().stream()
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

        projectionManager.create(projection, query, CONTINUOUS).join();
        projectionManager.disable(projection).join();

        Projection result = projectionManager.getStatus(projection).join();

        assertNotNull(result);
        assertThat(result.status, containsString("Stopped"));
    }

    @Test
    public void failsToDisableProjectionWhenProjectionDoesNotExists() {
        try {
            projectionManager.disable(UUID.randomUUID().toString()).join();
            fail("should fail with 'ProjectionNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(ProjectionNotFoundException.class));
        }
    }

    @Test
    public void enablesProjection() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.create(projection, query, CONTINUOUS).join();
        projectionManager.disable(projection).join();
        projectionManager.enable(projection).join();

        Projection result = projectionManager.getStatus(projection).join();

        assertNotNull(result);
        assertThat(result.status, containsString("Running"));
    }

    @Test
    public void failsToEnableProjectionWhenProjectionDoesNotExists() {
        try {
            projectionManager.enable(UUID.randomUUID().toString()).join();
            fail("should fail with 'ProjectionNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(ProjectionNotFoundException.class));
        }
    }

    @Test
    public void abortsProjection() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.create(projection, query, CONTINUOUS).join();
        projectionManager.abort(projection).join();

        Projection result = projectionManager.getStatus(projection).join();

        assertNotNull(result);
        assertThat(result.status, containsString("Aborted"));
    }

    @Test
    public void failsToAbortProjectionWhenProjectionDoesNotExists() {
        try {
            projectionManager.abort(UUID.randomUUID().toString()).join();
            fail("should fail with 'ProjectionNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(ProjectionNotFoundException.class));
        }
    }

    @Test
    public void resetsProjection() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.create(projection, query, CONTINUOUS).join();

        projectionManager.awaitStatus(projection, p -> p.eventsProcessedAfterRestart == 2, Duration.ofSeconds(15));
        projectionManager.disable(projection).join();
        projectionManager.awaitStatus(projection, p -> p.status.contains("Stopped"), Duration.ofSeconds(15));

        assertEquals(2, projectionManager.getStatus(projection).join().eventsProcessedAfterRestart);

        projectionManager.reset(projection).join();

        assertEquals(0, projectionManager.getStatus(projection).join().eventsProcessedAfterRestart);
    }

    @Test
    public void failsToResetProjectionWhenProjectionDoesNotExists() {
        try {
            projectionManager.reset(UUID.randomUUID().toString()).join();
            fail("should fail with 'ProjectionNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(ProjectionNotFoundException.class));
        }
    }

    @Test
    public void listsAllProjections() {
        List<Projection> projections = projectionManager.findAll().join();
        assertFalse(projections.isEmpty());
    }

    @Test
    public void listsOneTimeProjections() {
        String query = createStandardQuery(UUID.randomUUID().toString());

        projectionManager.create(UUID.randomUUID().toString(), query, ONE_TIME).join();

        List<Projection> projections = projectionManager.findByMode(ONE_TIME).join();
        assertFalse(projections.isEmpty());
    }

    @Test
    public void listsContinuousProjections() {
        String query = createStandardQuery(UUID.randomUUID().toString());
        String projection = "projection-" + UUID.randomUUID().toString();

        projectionManager.create(projection, query, CONTINUOUS).join();

        assertTrue(projectionManager.findByMode(CONTINUOUS).join().stream()
            .anyMatch(p -> p.effectiveName.equals(projection)));
    }

    @Test
    public void getsProjectionStateWhileRunning() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.create(projection, query, CONTINUOUS).join();

        String state = projectionManager.getState(projection).join();

        assertFalse(isNullOrEmpty(state));
    }

    @Test
    public void failsToGetStateWhenProjectionDoesNotExists() {
        try {
            projectionManager.getState(UUID.randomUUID().toString()).join();
            fail("should fail with 'ProjectionNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(ProjectionNotFoundException.class));
        }
    }

    @Test
    public void getsProjectionStatusWhileRunning() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.create(projection, query, CONTINUOUS).join();

        Projection result = projectionManager.getStatus(projection).join();

        assertNotNull(result);
    }

    @Test
    public void failsToGetStatusWhenProjectionDoesNotExists() {
        try {
            projectionManager.getStatus(UUID.randomUUID().toString()).join();
            fail("should fail with 'ProjectionNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(ProjectionNotFoundException.class));
        }
    }

    @Test
    public void getsProjectionResultWhileRunning() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.create(projection, query, CONTINUOUS).join();

        String result = projectionManager.getResult(projection).join();

        assertEquals("{\"count\":1}", result);
    }

    @Test
    public void failsToGetResultWhenProjectionDoesNotExists() {
        try {
            projectionManager.getResult(UUID.randomUUID().toString()).join();
            fail("should fail with 'ProjectionNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(ProjectionNotFoundException.class));
        }
    }

    @Test
    public void getsProjectionQueryWhileRunning() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.create(projection, query, CONTINUOUS).join();

        String result = projectionManager.getQuery(projection).join();

        assertEquals(query, result);
    }

    @Test
    public void failsToGetQueryWhenProjectionDoesNotExists() {
        try {
            projectionManager.getQuery(UUID.randomUUID().toString()).join();
            fail("should fail with 'ProjectionNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(ProjectionNotFoundException.class));
        }
    }

    @Test
    @Retryable(HttpOperationTimedOutException.class)
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
            projectionManager.create(projection, query, CONTINUOUS).join();

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

        projectionManager.create(projection, query, CONTINUOUS).join();

        String statistics = projectionManager.getStatistics(projection).join();

        assertFalse(isNullOrEmpty(statistics));
    }

    @Test
    public void failsToGetStatisticsWhenProjectionDoesNotExists() {
        try {
            projectionManager.getStatistics(UUID.randomUUID().toString()).join();
            fail("should fail with 'ProjectionNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(ProjectionNotFoundException.class));
        }
    }

    @Test
    public void updatesProjectionQuery() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String originalQuery = createStandardQuery(stream);
        String newQuery = createStandardQuery("DifferentStream");

        projectionManager.create(projection, originalQuery, CONTINUOUS).join();
        projectionManager.updateQuery(projection, newQuery).join();

        String result = projectionManager.getQuery(projection).join();

        assertEquals(newQuery, result);
    }

    @Test
    public void failsToUpdateQueryWhenProjectionDoesNotExists() {
        String query = createStandardQuery(generateStreamName());

        try {
            projectionManager.updateQuery(UUID.randomUUID().toString(), query).join();
            fail("should fail with 'ProjectionNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(ProjectionNotFoundException.class));
        }
    }

    @Test
    public void deletesProjection() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.create(projection, query, CONTINUOUS).join();
        assertTrue(projectionManager.findAll().join().stream().anyMatch(p -> p.name.equals(projection)));

        projectionManager.disable(projection).join();
        projectionManager.delete(projection).join();
        assertFalse(projectionManager.findAll().join().stream().anyMatch(p -> p.name.equals(projection)));
    }

    @Test
    public void failsToDeleteWhenProjectionDoesNotExists() {
        try {
            projectionManager.delete(UUID.randomUUID().toString()).join();
            fail("should fail with 'ProjectionNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(ProjectionNotFoundException.class));
        }
    }

    @Test
    public void waitsForProjectionCompletedStatus() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvents(20)).join();

        String projection = "projection-" + stream;
        String query = createStandardQuery(stream);

        projectionManager.create(projection, query, TRANSIENT);

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
