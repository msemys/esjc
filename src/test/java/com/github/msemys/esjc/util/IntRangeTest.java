package com.github.msemys.esjc.util;

import com.github.msemys.esjc.util.IntRange.Type;
import org.junit.Test;

import static org.junit.Assert.*;

public class IntRangeTest {

    @Test
    public void createsSingleItemClosedRange() {
        IntRange range = new IntRange(5, 5, Type.CLOSED);

        assertFalse(range.contains(4));
        assertTrue(range.contains(5));
        assertFalse(range.contains(6));
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToCreateSingleItemOpenRange() {
        new IntRange(5, 5, Type.OPEN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToCreateInvalidOpenRange() {
        new IntRange(10, 5, Type.OPEN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToCreateInvalidClosedRange() {
        new IntRange(10, 5, Type.CLOSED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToCreateInvalidOpenClosedRange() {
        new IntRange(10, 5, Type.OPEN_CLOSED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToCreateInvalidClosedOpenRange() {
        new IntRange(10, 5, Type.CLOSED_OPEN);
    }

    @Test
    public void createsOpenClosedEmptyRange() {
        IntRange range = new IntRange(5, 5, Type.OPEN_CLOSED);

        assertFalse(range.contains(4));
        assertFalse(range.contains(5));
        assertFalse(range.contains(6));
    }

    @Test
    public void createsClosedOpenEmptyRange() {
        IntRange range = new IntRange(5, 5, Type.CLOSED_OPEN);

        assertFalse(range.contains(4));
        assertFalse(range.contains(5));
        assertFalse(range.contains(6));
    }

    @Test
    public void containsItemsInOpenRange() {
        IntRange range = new IntRange(5, 7, Type.OPEN);

        assertFalse(range.contains(3));
        assertFalse(range.contains(4));
        assertFalse(range.contains(5));
        assertTrue(range.contains(6));
        assertFalse(range.contains(7));
        assertFalse(range.contains(8));
        assertFalse(range.contains(9));
    }

    @Test
    public void containsItemsInClosedRange() {
        IntRange range = new IntRange(5, 7, Type.CLOSED);

        assertFalse(range.contains(3));
        assertFalse(range.contains(4));
        assertTrue(range.contains(5));
        assertTrue(range.contains(6));
        assertTrue(range.contains(7));
        assertFalse(range.contains(8));
        assertFalse(range.contains(9));
    }

    @Test
    public void containsItemsInOpenClosedRange() {
        IntRange range = new IntRange(5, 7, Type.OPEN_CLOSED);

        assertFalse(range.contains(3));
        assertFalse(range.contains(4));
        assertFalse(range.contains(5));
        assertTrue(range.contains(6));
        assertTrue(range.contains(7));
        assertFalse(range.contains(8));
        assertFalse(range.contains(9));
    }

    @Test
    public void containsItemsInClosedOpenRange() {
        IntRange range = new IntRange(5, 7, Type.CLOSED_OPEN);

        assertFalse(range.contains(3));
        assertFalse(range.contains(4));
        assertTrue(range.contains(5));
        assertTrue(range.contains(6));
        assertFalse(range.contains(7));
        assertFalse(range.contains(8));
        assertFalse(range.contains(9));
    }

    @Test
    public void getsOpenRangeStringRepresentation() {
        assertEquals("(5..7)", new IntRange(5, 7, Type.OPEN).toString());
    }

    @Test
    public void getsClosedRangeStringRepresentation() {
        assertEquals("[5..7]", new IntRange(5, 7, Type.CLOSED).toString());
    }

    @Test
    public void getsOpenClosedRangeStringRepresentation() {
        assertEquals("(5..7]", new IntRange(5, 7, Type.OPEN_CLOSED).toString());
    }

    @Test
    public void getsClosedOpenRangeStringRepresentation() {
        assertEquals("[5..7)", new IntRange(5, 7, Type.CLOSED_OPEN).toString());
    }

    @Test
    public void displaysMinMaxIntegerValuesAsInfinityInStringRepresentation() {
        assertEquals("(-infinity..infinity)", new IntRange(Integer.MIN_VALUE, Integer.MAX_VALUE, Type.OPEN).toString());
    }

    @Test
    public void displaysMaxIntegerValueAsInfinityInStringRepresentation() {
        assertEquals("[-1..infinity]", new IntRange(-1, Integer.MAX_VALUE, Type.CLOSED).toString());
    }
}
