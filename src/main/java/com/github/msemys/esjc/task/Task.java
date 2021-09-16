package com.github.msemys.esjc.task;

public interface Task {

    default void fail(Exception exception) {
        // do nothing
    }

}
