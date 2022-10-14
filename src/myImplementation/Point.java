package myImplementation;

public record Point(int x, int y) {

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
    public String toString() {
        return "(" + this.x + "," + this.y + ")";
    }
}
