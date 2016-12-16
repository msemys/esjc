package com.github.msemys.esjc.matcher;

import com.github.msemys.esjc.EventData;
import com.github.msemys.esjc.RecordedEvent;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Iterator;
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
        Iterator<EventData> expectedIterator = expected.iterator();
        Iterator<RecordedEvent> actualIterator = actual.iterator();

        while (expectedIterator.hasNext() && actualIterator.hasNext()) {
            EventData expectedItem = expectedIterator.next();
            RecordedEvent actualItem = actualIterator.next();

            elementMatcher = RecordedEventMatcher.equalTo(expectedItem);

            if (!elementMatcher.matches(actualItem)) {
                return false;
            }

            elementIndex++;
        }

        return !expectedIterator.hasNext() && !actualIterator.hasNext();
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
        if (elementMatcher != null && elementIndex < actual.size()) {
            RecordedEvent actualItem = actual.get(elementIndex);

            mismatchDescription
                .appendText("collection has event ")
                .appendValue(actualItem.eventType)
                .appendText(" ")
                .appendValue(actualItem.eventId)
                .appendText(" at index ")
                .appendValue(elementIndex);
        } else {
            mismatchDescription.appendText("collection size is ").appendValue(actual.size());
        }
    }

    @Factory
    public static Matcher<List<RecordedEvent>> hasItems(List<EventData> items) {
        return new RecordedEventListMatcher(items);
    }

}
