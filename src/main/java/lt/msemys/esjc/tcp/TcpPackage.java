package lt.msemys.esjc.tcp;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOfRange;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.Core/Services/Transport/Tcp/TcpPackage.cs">EventStore.Core/Services/Transport/Tcp/TcpPackage.cs</a>
 */
public class TcpPackage {
    private static final int UUID_SIZE = 16;

    private static final int COMMAND_OFFSET = 0;
    private static final int FLAG_OFFSET = COMMAND_OFFSET + 1;
    private static final int CORRELATION_OFFSET = FLAG_OFFSET + 1;
    private static final int AUTH_OFFSET = CORRELATION_OFFSET + UUID_SIZE;

    private static final int MANDATORY_SIZE = AUTH_OFFSET;

    public final TcpCommand command;
    public final TcpFlag flag;
    public final UUID correlationId;
    public final String login;
    public final String password;
    public final byte[] data;

    private TcpPackage(Builder builder) {
        this.command = builder.command;
        this.flag = builder.flag;
        this.correlationId = builder.correlationId;
        this.login = builder.login;
        this.password = builder.password;
        this.data = builder.data;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public byte[] toByteArray() {
        byte[] result;

        if (flag == TcpFlag.Authenticated) {
            int loginLength = login.getBytes(UTF_8).length;
            int passwordLength = password.getBytes(UTF_8).length;

            if (loginLength > 255) {
                throw new IllegalArgumentException(String.format("Login serialized length should be less than 256 bytes (but is %d).", loginLength));
            }
            if (passwordLength > 255) {
                throw new IllegalArgumentException(String.format("Password serialized length should be less than 256 bytes (but is %d).", passwordLength));
            }

            result = createTcpPackage(MANDATORY_SIZE + 2 + loginLength + passwordLength + data.length);

            result[AUTH_OFFSET] = (byte) loginLength;
            System.arraycopy(login.getBytes(UTF_8), 0, result, AUTH_OFFSET + 1, loginLength);

            final int passwordOffset = AUTH_OFFSET + 1 + loginLength;
            result[passwordOffset] = (byte) passwordLength;
            System.arraycopy(password.getBytes(UTF_8), 0, result, passwordOffset + 1, passwordLength);
        } else {
            result = createTcpPackage(MANDATORY_SIZE + data.length);
        }

        System.arraycopy(data, 0, result, result.length - data.length, data.length);

        return result;
    }

    public static TcpPackage of(byte[] data) {
        if (data.length < MANDATORY_SIZE) {
            throw new IllegalArgumentException(String.format("Data too short, length: %d", data.length));
        }

        TcpCommand command = TcpCommand.of(data[COMMAND_OFFSET]);
        TcpFlag flag = TcpFlag.of(data[FLAG_OFFSET]);
        UUID correlationId = convertByteArrayToUUID(copyOfRange(data, CORRELATION_OFFSET, CORRELATION_OFFSET + UUID_SIZE));

        int headerSize = MANDATORY_SIZE;

        String login = null;
        String password = null;

        if (flag == TcpFlag.Authenticated) {
            final int loginLength = data[AUTH_OFFSET];

            if (AUTH_OFFSET + 1 + loginLength + 1 >= data.length) {
                throw new RuntimeException("Login length is too big, it doesn't fit into TcpPackage.");
            } else {
                login = new String(data, AUTH_OFFSET + 1, loginLength, UTF_8);
            }

            final int passwordOffset = AUTH_OFFSET + 1 + loginLength;
            final int passwordLength = data[passwordOffset];

            if (passwordOffset + 1 + passwordLength > data.length) {
                throw new RuntimeException("Password length is too big, it doesn't fit into TcpPackage.");
            } else {
                password = new String(data, passwordOffset + 1, passwordLength, UTF_8);
            }

            headerSize += 1 + loginLength + 1 + passwordLength;
        }

        byte[] message = copyOfRange(data, headerSize, data.length);

        return new Builder()
                .withCommand(command)
                .withTcpFlag(flag)
                .withCorrelationId(correlationId)
                .withLogin(login)
                .withPassword(password)
                .withData(message)
                .build();
    }

    private byte[] createTcpPackage(int size) {
        byte[] result = new byte[size];
        result[COMMAND_OFFSET] = command.value;
        result[FLAG_OFFSET] = flag.value;
        System.arraycopy(convertUUIDToByteArray(correlationId), 0, result, CORRELATION_OFFSET, UUID_SIZE);

        return result;
    }

    private static byte[] convertUUIDToByteArray(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(UUID_SIZE);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private static UUID convertByteArrayToUUID(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long mostSignificantBits = bb.getLong();
        long leastSignificantBits = bb.getLong();
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TcpPackage{");
        sb.append("command=").append(command);
        sb.append(", flag=").append(flag);
        sb.append(", correlationId=").append(correlationId);
        sb.append(", login='").append(login).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append(", data=").append(Arrays.toString(data));
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {
        private TcpCommand command;
        private TcpFlag flag;
        private UUID correlationId;
        private String login;
        private String password;
        private byte[] data;

        private Builder() {
        }

        public Builder withCommand(TcpCommand command) {
            this.command = command;
            return this;
        }

        public Builder withTcpFlag(TcpFlag flag) {
            this.flag = flag;
            return this;
        }

        public Builder withCorrelationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder withLogin(String login) {
            this.login = login;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withData(byte[] data) {
            this.data = data;
            return this;
        }

        public TcpPackage build() {
            if (command == null) {
                throw new IllegalArgumentException("TCP command is not provided.");
            }

            if (flag == null) {
                flag = TcpFlag.None;
            }

            if (correlationId == null) {
                throw new IllegalArgumentException("Correlation ID is not provided.");
            }

            if (flag == TcpFlag.Authenticated) {
                if (login == null) {
                    throw new IllegalArgumentException("Login is not provided for authorized TcpPackage.");
                }
                if (password == null) {
                    throw new IllegalArgumentException("Password is not provided for authorized TcpPackage.");
                }
            } else {
                if (login != null) {
                    throw new IllegalArgumentException("Login provided for non-authorized TcpPackage.");
                }
                if (password != null) {
                    throw new IllegalArgumentException("Password provided for non-authorized TcpPackage.");
                }
            }

            if (data == null) {
                data = new byte[0];
            }

            return new TcpPackage(this);
        }
    }

}