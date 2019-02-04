package com.github.msemys.esjc.util;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Guid: 35918bc9-196d-40ea-9779-889d79b753f0
 * C9 8B 91 35 6D 19 EA 40 97 79 88 9D 79 B7 53 F0
 * example from https://docs.microsoft.com/en-us/dotnet/api/system.guid.tobytearray?view=netframework-4.7
 */
public class UUIDConverterTest {

    @Test
    public void toUUID() {
        byte[] uuidBytes = toByteArray("C9 8B 91 35 6D 19 EA 40 97 79 88 9D 79 B7 53 F0");

        UUID uuid = UUIDConverter.toUUID(uuidBytes);

        assertEquals(UUID.fromString("35918bc9-196d-40ea-9779-889d79b753f0"), uuid);
    }

    @Test
    public void toBytes() {
        UUID uuid = UUID.fromString("35918bc9-196d-40ea-9779-889d79b753f0");

        byte[] uuidBytes = UUIDConverter.toBytes(uuid);

        assertArrayEquals(toByteArray("C9 8B 91 35 6D 19 EA 40 97 79 88 9D 79 B7 53 F0"), uuidBytes);
    }

    private byte[] toByteArray(String hexString) {
        String[] hexArray = hexString.split(" ");
        byte[] bytes = new byte[hexArray.length];
        for (int i = 0; i<hexArray.length; i++) {
            bytes[i] = toByte(hexArray[i]);
        }
        return bytes;
    }

    private byte toByte(String hexString) {
        char[] charArray = hexString.toCharArray();
        Integer i = (Character.digit(charArray[0], 16) << 4) + Character.digit(charArray[1], 16);
        return i.byteValue();
    }
}
