package com.github.msemys.esjc;

public class ExpectedVersion {
    private static final ExpectedVersion NO_STREAM = new ExpectedVersion(-1);
    private static final ExpectedVersion ANY = new ExpectedVersion(-2);

    public final int value;

    private ExpectedVersion(int value) {
        this.value = value;
    }

    public static ExpectedVersion of(int value) {
        return new ExpectedVersion(value);
    }

    public static ExpectedVersion noStream() {
        return NO_STREAM;
    }

    public static ExpectedVersion any() {
        return ANY;
    }
}
