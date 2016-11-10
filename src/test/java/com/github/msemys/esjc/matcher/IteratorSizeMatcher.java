package com.github.msemys.esjc.matcher;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import java.util.Iterator;

public class IteratorSizeMatcher extends TypeSafeMatcher<Iterator<?>> {
    private final int expected;
    private int actual = 0;

    public IteratorSizeMatcher(int expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(Iterator<?> item) {
        while (item.hasNext()) {
            item.next();
            actual++;
        }
        return expected == actual;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("size ").appendValue(expected);
    }

    @Override
    protected void describeMismatchSafely(Iterator<?> item, Description description) {
        description.appendText("was ").appendValue(actual);
    }

    @Factory
    public static IteratorSizeMatcher hasSize(int size) {
        return new IteratorSizeMatcher(size);
    }

}
