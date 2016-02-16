package com.github.msemys.esjc.tcp;

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