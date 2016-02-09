package lt.msemys.esjc.task;

import lt.msemys.esjc.operation.Operation;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;

public class StartOperation implements Task {
    public final Operation operation;

    public StartOperation(Operation operation) {
        checkNotNull(operation, "operation");
        this.operation = operation;
    }
}
