package com.github.msemys.esjc.matcher;

import com.github.msemys.esjc.EventData;
import com.github.msemys.esjc.RecordedEvent;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsCollectionContaining;

import java.util.ArrayList;
import java.util.List;

import static com.github.msemys.esjc.util.Strings.newString;
import static org.hamcrest.core.AllOf.allOf;

public class RecordedEventMatcher extends TypeSafeMatcher<RecordedEvent> {

    private final EventData expected;

    public RecordedEventMatcher(EventData expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(RecordedEvent actual) {
        if (!expected.eventId.equals(actual.eventId)) {
            return false;
        }

        if (!expected.type.equals(actual.eventType)) {
            return false;
        }

        String expectedDataString = newString(expected.data);
        String expectedMetadataString = newString(expected.metadata);

        String actualDataString = newString(actual.data);
        String actualMetadataDataString = newString(actual.metadata);

        return expectedDataString.equals(actualDataString) && expectedMetadataString.equals(actualMetadataDataString);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("event ")
            .appendValue(expected.type)
            .appendText(" ")
            .appendValue(expected.eventId);
    }

    @Factory
    public static Matcher<RecordedEvent> equalTo(EventData item) {
        return new RecordedEventMatcher(item);
    }

    @Factory
    public static Matcher<Iterable<? super RecordedEvent>> hasItem(EventData item) {
        return new IsCollectionContaining<>(equalTo(item));
    }

    @Factory
    public static Matcher<Iterable<RecordedEvent>> hasItems(Iterable<EventData> items) {
        List<Matcher<? super Iterable<RecordedEvent>>> all = new ArrayList<>();
        items.forEach(item -> all.add(hasItem(item)));
        return allOf(all);
    }

}
