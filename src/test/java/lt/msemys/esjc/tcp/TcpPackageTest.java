package lt.msemys.esjc.tcp;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOfRange;
import static org.junit.Assert.*;

public class TcpPackageTest {

    private static final Logger logger = LoggerFactory.getLogger(TcpPackageTest.class);

    private static final String DATA = "{ test : 123 }";

    @Test
    public void convertsNonAuthorizedTcpPackageToByteArray() {
        TcpPackage tcpPackage = TcpPackage.newBuilder()
                .withCommand(TcpCommand.HeartbeatRequestCommand)
                .withCorrelationId(UUID.randomUUID())
                .withData(DATA.getBytes(UTF_8))
                .build();

        logger.debug(tcpPackage.toString());

        byte[] result = tcpPackage.toByteArray();

        assertNotNull(result);

        assertEquals(TcpCommand.HeartbeatRequestCommand.value, result[0]);
        assertEquals(TcpFlag.None.value, result[1]);
        assertEquals(tcpPackage.correlationId, convertByteArrayToUUID(copyOfRange(result, 2, 18)));
        assertEquals(DATA, new String(result, 1 + 1 + 16, DATA.length(), UTF_8));
    }

    @Test
    public void convertsAuthorizedTcpPackageToByteArray() {
        String user = "admin";
        String password = "secret";

        TcpPackage tcpPackage = TcpPackage.newBuilder()
                .withCommand(TcpCommand.HeartbeatRequestCommand)
                .withTcpFlag(TcpFlag.Authenticated)
                .withCorrelationId(UUID.randomUUID())
                .withLogin(user)
                .withPassword(password)
                .withData(DATA.getBytes(UTF_8))
                .build();

        logger.debug(tcpPackage.toString());

        byte[] result = tcpPackage.toByteArray();

        assertNotNull(result);

        assertEquals(TcpCommand.HeartbeatRequestCommand.value, result[0]);
        assertEquals(TcpFlag.Authenticated.value, result[1]);
        assertEquals(tcpPackage.correlationId, convertByteArrayToUUID(copyOfRange(result, 2, 18)));

        assertEquals(user.length(), result[2 + 16]);
        assertEquals(user, new String(result, 2 + 16 + 1, user.length(), UTF_8));

        assertEquals(password.length(), result[2 + 16 + user.length() + 1]);
        assertEquals(password, new String(result, 2 + 16 + 1 + user.length() + 1, password.length(), UTF_8));
        assertEquals(DATA, new String(result, 2 + 16 + 2 + user.length() + password.length(), DATA.length(), UTF_8));
    }

    @Test
    public void createsNonAuthorizedTcpPackageFromByteArray() {
        TcpPackage tcpPackage = TcpPackage.of(Base64.getDecoder().decode("AQANwJK9rrNK1KbPREjkylDLeyB0ZXN0IDogMTIzIH0="));

        logger.debug(tcpPackage.toString());

        assertNotNull(tcpPackage);

        assertEquals(TcpCommand.HeartbeatRequestCommand, tcpPackage.command);
        assertEquals(TcpFlag.None, tcpPackage.flag);
        assertEquals("0dc092bd-aeb3-4ad4-a6cf-4448e4ca50cb", tcpPackage.correlationId.toString());
        assertNull(tcpPackage.login);
        assertNull(tcpPackage.password);
        assertEquals(DATA, new String(tcpPackage.data, UTF_8));
    }

    @Test
    public void createsAuthorizedTcpPackageFromByteArray() {
        TcpPackage tcpPackage = TcpPackage.of(Base64.getDecoder().decode("AQG+ygybTS1MEJvY1RjaXM8ABWFkbWluBnNlY3JldHsgdGVzdCA6IDEyMyB9"));

        logger.debug(tcpPackage.toString());

        assertNotNull(tcpPackage);

        assertEquals(TcpCommand.HeartbeatRequestCommand, tcpPackage.command);
        assertEquals(TcpFlag.Authenticated, tcpPackage.flag);
        assertEquals("beca0c9b-4d2d-4c10-9bd8-d518da5ccf00", tcpPackage.correlationId.toString());
        assertEquals("admin", tcpPackage.login);
        assertEquals("secret", tcpPackage.password);
        assertEquals(DATA, new String(tcpPackage.data, UTF_8));
    }

    private static UUID convertByteArrayToUUID(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long mostSignificantBits = bb.getLong();
        long leastSignificantBits = bb.getLong();
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

}