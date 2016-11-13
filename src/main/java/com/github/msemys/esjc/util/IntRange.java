package com.github.msemys.esjc.util;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;

public class IntRange {
    public enum Type {
        /**
         * (a..b) = { x | a < x < b }
         */
        OPEN,

        /**
         * [a..b] = { x | a <= x <= b }
         */
        CLOSED,

        /**
         * (a..b] = { x | a < x <= b }
         */
        OPEN_CLOSED,

        /**
         * [a..b) = { x | a <= x < b }
         */
        CLOSED_OPEN
    }

    public final int min;
    public final int max;
    public final Type type;
    private String string;

    IntRange(int min, int max, Type type) {
        if (type == Type.OPEN) {
            checkArgument(min < max, "min should be less than max");
        } else {
            checkArgument(min <= max, "min should be less or equal to max");
        }

        this.min = min;
        this.max = max;
        this.type = type;
    }

    public boolean contains(int value) {
        switch (type) {
            case OPEN:
                return min < value && value < max;
            case CLOSED:
                return min <= value && value <= max;
            case OPEN_CLOSED:
                return min < value && value <= max;
            case CLOSED_OPEN:
                return min <= value && value < max;
            default:
                throw new IllegalStateException("Unexpected type: " + type);
        }
    }

    @Override
    public String toString() {
        if (string == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(type == Type.OPEN || type == Type.OPEN_CLOSED ? "(" : "[");
            sb.append(min == Integer.MIN_VALUE ? "-infinity" : min);
            sb.append("..");
            sb.append(max == Integer.MAX_VALUE ? "infinity" : max);
            sb.append(type == Type.OPEN || type == Type.CLOSED_OPEN ? ")" : "]");
            string = sb.toString();
        }
        return string;
    }

}
