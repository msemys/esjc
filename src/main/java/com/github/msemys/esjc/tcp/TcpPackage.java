package com.github.msemys.esjc.tcp;

import com.github.msemys.esjc.util.EmptyArrays;
import com.github.msemys.esjc.util.Strings;

import java.util.Arrays;
import java.util.UUID;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static com.github.msemys.esjc.util.Preconditions.checkState;
import static com.github.msemys.esjc.util.UUIDConverter.toBytes;
import static com.github.msemys.esjc.util.UUIDConverter.toUUID;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOfRange;

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
            byte[] loginBytes = Strings.toBytes(login);
            byte[] passwordBytes = Strings.toBytes(password);

            checkArgument(loginBytes.length < 256, "Login serialized length should be less than 256 bytes (but is %d).", loginBytes.length);
            checkArgument(passwordBytes.length < 256, "Password serialized length should be less than 256 bytes (but is %d).", passwordBytes.length);

            result = createTcpPackage(MANDATORY_SIZE + 2 + loginBytes.length + passwordBytes.length + data.length);

            result[AUTH_OFFSET] = (byte) loginBytes.length;
            System.arraycopy(loginBytes, 0, result, AUTH_OFFSET + 1, loginBytes.length);

            final int passwordOffset = AUTH_OFFSET + 1 + loginBytes.length;
            result[passwordOffset] = (byte) passwordBytes.length;
            System.arraycopy(passwordBytes, 0, result, passwordOffset + 1, passwordBytes.length);
        } else {
            result = createTcpPackage(MANDATORY_SIZE + data.length);
        }

        System.arraycopy(data, 0, result, result.length - data.length, data.length);

        return result;
    }

    public static TcpPackage of(byte[] data) {
        checkArgument(data.length >= MANDATORY_SIZE, "Data too short, length: %d", data.length);

        TcpCommand command = TcpCommand.of(data[COMMAND_OFFSET]);
        TcpFlag flag = TcpFlag.of(data[FLAG_OFFSET]);
        UUID correlationId = toUUID(copyOfRange(data, CORRELATION_OFFSET, CORRELATION_OFFSET + UUID_SIZE));

        int headerSize = MANDATORY_SIZE;

        String login = null;
        String password = null;

        if (flag == TcpFlag.Authenticated) {
            final int loginLength = data[AUTH_OFFSET];

            checkState(AUTH_OFFSET + 1 + loginLength + 1 < data.length, "Login length is too big, it doesn't fit into TcpPackage.");
            login = new String(data, AUTH_OFFSET + 1, loginLength, UTF_8);

            final int passwordOffset = AUTH_OFFSET + 1 + loginLength;
            final int passwordLength = data[passwordOffset];

            checkState(passwordOffset + 1 + passwordLength <= data.length, "Password length is too big, it doesn't fit into TcpPackage.");
            password = new String(data, passwordOffset + 1, passwordLength, UTF_8);

            headerSize += 1 + loginLength + 1 + passwordLength;
        }

        byte[] message = copyOfRange(data, headerSize, data.length);

        return newBuilder()
                .command(command)
                .flag(flag)
                .correlationId(correlationId)
                .login(login)
                .password(password)
                .data(message)
                .build();
    }

    private byte[] createTcpPackage(int size) {
        byte[] result = new byte[size];
        result[COMMAND_OFFSET] = command.value;
        result[FLAG_OFFSET] = flag.value;
        System.arraycopy(toBytes(correlationId), 0, result, CORRELATION_OFFSET, UUID_SIZE);

        return result;
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

        public Builder command(TcpCommand command) {
            this.command = command;
            return this;
        }

        public Builder flag(TcpFlag flag) {
            this.flag = flag;
            return this;
        }

        public Builder correlationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder login(String login) {
            this.login = login;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder data(byte[] data) {
            this.data = data;
            return this;
        }

        public TcpPackage build() {
            checkNotNull(command, "TCP command is not provided.");

            if (flag == null) {
                flag = TcpFlag.None;
            }

            checkNotNull(correlationId, "Correlation ID is not provided.");

            if (flag == TcpFlag.Authenticated) {
                checkNotNull(login, "Login is not provided for authorized TcpPackage.");
                checkNotNull(password, "Password is not provided for authorized TcpPackage.");
            } else {
                checkArgument(login == null, "Login provided for non-authorized TcpPackage.");
                checkArgument(password == null, "Password provided for non-authorized TcpPackage.");
            }

            if (data == null) {
                data = EmptyArrays.EMPTY_BYTES;
            }

            return new TcpPackage(this);
        }
    }

}