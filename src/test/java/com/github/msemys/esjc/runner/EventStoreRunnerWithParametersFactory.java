package com.github.msemys.esjc.runner;

import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

public class EventStoreRunnerWithParametersFactory implements ParametersRunnerFactory {

    @Override
    public Runner createRunnerForTestWithParameters(TestWithParameters test) throws InitializationError {
        return new EventStoreRunnerWithParameters(test);
    }

}
