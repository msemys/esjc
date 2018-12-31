package com.github.msemys.esjc.runner;

import com.github.msemys.esjc.EventStore;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.TestWithParameters;

import static com.github.msemys.esjc.util.Preconditions.checkState;

public class EventStoreRunnerWithParameters extends BlockJUnit4ClassRunnerWithParameters {

    private final EventStore eventstore;

    public EventStoreRunnerWithParameters(TestWithParameters test) throws InitializationError {
        super(test);

        Object parameter = test.getParameters().get(0);

        checkState(parameter instanceof EventStore, "test parameter should be type of '%s'", EventStore.class.getName());

        eventstore = (EventStore) parameter;
    }

    @Override
    public void run(RunNotifier notifier) {
        try {
            super.run(notifier);
        } finally {
            eventstore.shutdown();
        }
    }
}
