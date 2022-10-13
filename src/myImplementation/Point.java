package myImplementation;

import java.util.Objects;

public class Point {
    public final int x;
    public final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point move(int x, int y) {
        return new Point(this.x + x, this.y + y);
    }

    public Point moveNorth() {
        return this.move(0, 1);
    }
    public Point moveEast() {
        return this.move(1, 0);
    }

    public Point moveSouth() {
        return this.move(0, -1);
    }

    public Point moveWest() {
        return this.move(-1, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + this.x + "," + this.y + ")";
    }
}
