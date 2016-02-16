package com.github.msemys.esjc;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;

public class Position implements Comparable<Position> {

    public static final Position START = new Position(0, 0);
    public static final Position END = new Position(-1, -1);

    public final long commitPosition;
    public final long preparePosition;

    public Position(long commitPosition, long preparePosition) {
        checkArgument(commitPosition >= preparePosition, "The commit position cannot be less than the prepare position");

        this.commitPosition = commitPosition;
        this.preparePosition = preparePosition;
    }

    @Override
    public int compareTo(Position that) {
        if (this.commitPosition < that.commitPosition ||
                (this.commitPosition == that.commitPosition && this.preparePosition < that.preparePosition)) {
            return -1;
        } else if (this.commitPosition > that.commitPosition ||
                (this.commitPosition == that.commitPosition && this.preparePosition > that.preparePosition)) {
            return 1;
        } else if (this.commitPosition == that.commitPosition && this.preparePosition == that.preparePosition) {
            return 0;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (commitPosition != position.commitPosition) return false;
        return preparePosition == position.preparePosition;

    }

    @Override
    public int hashCode() {
        int result = (int) (commitPosition ^ (commitPosition >>> 32));
        result = 31 * result + (int) (preparePosition ^ (preparePosition >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("%d/%d", commitPosition, preparePosition);
    }

    public static Position of(long commitPosition, long preparePosition) {
        return new Position(commitPosition, preparePosition);
    }

}
