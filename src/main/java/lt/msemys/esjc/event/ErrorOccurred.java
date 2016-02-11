package lt.msemys.esjc.event;

public class ErrorOccurred implements Event {

    public final Throwable throwable;

    public ErrorOccurred(Throwable throwable) {
        this.throwable = throwable;
    }

}
