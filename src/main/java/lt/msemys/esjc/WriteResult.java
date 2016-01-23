package lt.msemys.esjc;

public class WriteResult {
    public final int nextExpectedVersion;
    public final Position logPosition;

    public WriteResult(int nextExpectedVersion, Position logPosition) {
        this.nextExpectedVersion = nextExpectedVersion;
        this.logPosition = logPosition;
    }
}
