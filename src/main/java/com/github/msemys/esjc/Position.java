package com.github.msemys.esjc;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;

/**
 * Structure referring to a potential logical record position in the Event Store transaction file.
 */
public class Position implements Comparable<Position> {

    /**
     * Position representing the start of the transaction file.
     */
    public static final Position START = new Position(0, 0);

    /**
     * Position representing the end of the transaction file.
     */
    public static final Position END = new Position(-1, -1);

    /**
     * The commit position of the record.
     */
    public final long commitPosition;

    /**
     * The prepare position of the record.
     */
    public final long preparePosition;

    /**
     * Creates a new instance with the specified commit and prepare positions.
     * <p>
     * It is not guaranteed that the position is actually the start of a record in the transaction file.
     * </p>
     *
     * @param commitPosition  the commit position of the record.
     * @param preparePosition the prepare position of the record.
     */
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

    /**
     * Creates a new instance with the specified commit and prepare positions.
     * <p>
     * It is not guaranteed that the position is actually the start of a record in the transaction file.
     * </p>
     *
     * @param commitPosition  the commit position of the record.
     * @param preparePosition the prepare position of the record.
     */
    public static Position of(long commitPosition, long preparePosition) {
        return new Position(commitPosition, preparePosition);
    }

}
