package lt.msemys.esjc.util;

public class Preconditions {

    public static <T> T checkNotNull(T reference, String errorMessage) {
        if (reference == null) {
            throw new NullPointerException(errorMessage);
        }
        return reference;
    }

    public static <T> T checkNotNull(T reference, String errorMessage, Object... errorMessageArgs) {
        return checkNotNull(reference, String.format(errorMessage, errorMessageArgs));
    }

    public static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void checkArgument(boolean expression, String errorMessage, Object... errorMessageArgs) {
        checkArgument(expression, String.format(errorMessage, errorMessageArgs));
    }

    public static void checkState(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalStateException(errorMessage);
        }
    }

    public static void checkState(boolean expression, String errorMessage, Object... errorMessageArgs) {
        checkState(expression, String.format(errorMessage, errorMessageArgs));
    }

}
