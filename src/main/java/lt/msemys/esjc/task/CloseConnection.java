package lt.msemys.esjc.task;

public class CloseConnection implements Task {
    public final String reason;
    public final Throwable throwable;

    public CloseConnection(String reason) {
        this(reason, null);
    }

    public CloseConnection(String reason, Throwable throwable) {
        this.reason = reason;
        this.throwable = throwable;
    }
}
