package com.github.msemys.esjc.matcher;

import com.github.msemys.esjc.EventData;
import com.github.msemys.esjc.RecordedEvent;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.List;

public class RecordedEventListMatcher extends TypeSafeMatcher<List<RecordedEvent>> {
    private final List<EventData> expected;
    private Matcher<RecordedEvent> elementMatcher;
    private int elementIndex;

    public RecordedEventListMatcher(List<EventData> expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(List<RecordedEvent> actual) {
        if (expected.size() == actual.size()) {
            for (int i = 0; i < expected.size(); i++) {
                EventData expectedItem = expected.get(i);
                RecordedEvent actualItem = actual.get(i);

                elementMatcher = RecordedEventMatcher.equalTo(expectedItem);
                elementIndex = i;

                if (!elementMatcher.matches(actualItem)) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a collection with size ").appendValue(expected.size());

        if (elementMatcher != null) {
            description
                .appendText(" contains ")
                .appendDescriptionOf(elementMatcher)
                .appendText(" at index ")
                .appendValue(elementIndex);
        }
    }

    @Override
    protected void describeMismatchSafely(List<RecordedEvent> actual, Description mismatchDescription) {
        mismatchDescription.appendText("collection with size ").appendValue(actual.size());

        if (elementMatcher != null && elementIndex < actual.size()) {
            RecordedEvent actualItem = actual.get(elementIndex);

            mismatchDescription
                .appendText(" has event ")
                .appendValue(actualItem.eventType)
                .appendText(" ")
                .appendValue(actualItem.eventId)
                .appendText(" at index ")
                .appendValue(elementIndex);
        }
    }

    @Factory
    public static Matcher<List<RecordedEvent>> containsInOrder(List<EventData> items) {
        return new RecordedEventListMatcher(items);
    }

}
