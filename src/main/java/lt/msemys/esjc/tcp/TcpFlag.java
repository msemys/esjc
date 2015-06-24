package lt.msemys.esjc.tcp;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.Core/Services/Transport/Tcp/TcpPackage.cs">EventStore.Core/Services/Transport/Tcp/TcpPackage.cs</a>
 */
public enum TcpFlag {

    None(0x00),

    Authenticated(0x01);

    public final byte value;

    TcpFlag(int value) {
        this.value = (byte) value;
    }

    public static TcpFlag of(byte value) {
        for (TcpFlag f : TcpFlag.values()) {
            if (f.value == value) {
                return f;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported TCP flag %s", Integer.toHexString(value)));
    }

}