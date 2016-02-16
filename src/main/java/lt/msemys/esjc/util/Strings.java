package lt.msemys.esjc.util;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Strings {
    public static final String EMPTY = "";

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static String newString(byte[] bytes) {
        return (bytes == null || bytes.length == 0) ? EMPTY : new String(bytes, UTF_8);
    }

    public static byte[] toBytes(String string) {
        if (string == null) {
            return null;
        } else if (string.isEmpty()) {
            return EmptyArrays.EMPTY_BYTES;
        } else {
            return string.getBytes(UTF_8);
        }
    }

    public static String defaultIfEmpty(String string, String defaultString) {
        return isNullOrEmpty(string) ? defaultString : string;
    }

}
