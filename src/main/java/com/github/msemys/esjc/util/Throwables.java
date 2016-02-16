package com.github.msemys.esjc.util;

public class Throwables {

    public static RuntimeException propagate(Throwable throwable) {
        return (throwable instanceof RuntimeException) ? (RuntimeException) throwable : new RuntimeException(throwable);
    }

}
