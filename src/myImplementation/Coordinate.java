package myImplementation;

public record Coordinate(int x, int y) {

    public Coordinate move(int x, int y) {
        return new Coordinate(this.x + x, this.y + y);
    }

    public Coordinate moveNorth() {
        return this.move(0, 1);
    }

    public Coordinate moveEast() {
        return this.move(1, 0);
    }

    public Coordinate moveSouth() {
        return this.move(0, -1);
    }

    public Coordinate moveWest() {
        return this.move(-1, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinate coordinate = (Coordinate) o;
        return x == coordinate.x && y == coordinate.y;
    }

    @Override
    public String toString() {
        return "(" + this.x + "," + this.y + ")";
    }
}
