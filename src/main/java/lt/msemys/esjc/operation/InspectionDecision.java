package lt.msemys.esjc.operation;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.ClientAPI/SystemData/InspectionDecision.cs">EventStore.ClientAPI/SystemData/InspectionDecision.cs</a>
 */
public enum InspectionDecision {
    DoNothing,
    EndOperation,
    Retry,
    Reconnect,
    Subscribed
}
