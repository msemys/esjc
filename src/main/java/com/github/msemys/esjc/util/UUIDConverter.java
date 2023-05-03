package com.github.msemys.esjc.util;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDConverter {

    public static byte[] toBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return adaptEndianness(bb.array());
    }

    public static UUID toUUID(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(adaptEndianness(bytes));
        long mostSignificantBits = bb.getLong();
        long leastSignificantBits = bb.getLong();
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    /*
     * Adapt between mixed-endian UUID encoding (C#/EventStore) and big-endian (Java).
     * https://stackoverflow.com/questions/45671415/c-sharp-why-does-system-guid-flip-the-bytes-in-a-byte-array
     */
    private static byte[] adaptEndianness(byte[] bytes) {
        switchBytes(bytes, 0, 3);
        switchBytes(bytes, 1, 2);
        switchBytes(bytes, 4, 5);
        switchBytes(bytes, 6, 7);
        return bytes;
    }

    private static void switchBytes(byte[] bytes, int i, int j) {
        byte b = bytes[i];
        bytes[i] = bytes[j];
        bytes[j] = b;
    }

}
