package com.github.msemys.esjc.util;

public class Preconditions {

    public static <T> T checkNotNull(T reference, String errorMessage) {
        if (reference == null) {
            throw new NullPointerException(errorMessage);
        }
        return reference;
    }

    public static <T> T checkNotNull(T reference, String errorMessage, Object... errorMessageArgs) {
        if (reference == null) {
            throw new NullPointerException(String.format(errorMessage, errorMessageArgs));
        }
        return reference;
    }

    public static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void checkArgument(boolean expression, String errorMessage, Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(errorMessage, errorMessageArgs));
        }
    }

    public static void checkState(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalStateException(errorMessage);
        }
    }

    public static void checkState(boolean expression, String errorMessage, Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalStateException(String.format(errorMessage, errorMessageArgs));
        }
    }

}
