package lt.msemys.esjc.event;

public class ErrorOccurred implements Event {

    public final Exception exception;

    public ErrorOccurred(Exception exception) {
        this.exception = exception;
    }

}
