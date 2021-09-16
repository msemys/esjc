package com.github.msemys.esjc.task;

import com.github.msemys.esjc.operation.Operation;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

public class StartOperation implements Task {
    public final Operation operation;

    public StartOperation(Operation operation) {
        checkNotNull(operation, "operation is null");
        this.operation = operation;
    }

    @Override
    public void fail(Exception exception) {
        operation.fail(exception);
    }

}
