package lt.msemys.esjc.task;

public class CloseConnection implements Task {
    public final String reason;
    public final Exception exception;

    public CloseConnection(String reason) {
        this(reason, null);
    }

    public CloseConnection(String reason, Exception exception) {
        this.reason = reason;
        this.exception = exception;
    }
}
