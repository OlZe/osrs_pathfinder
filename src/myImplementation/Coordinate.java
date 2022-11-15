package myImplementation;

public record Coordinate(int x, int y, int z) {

    public Coordinate move(int x, int y, int z) {
        return new Coordinate(this.x + x, this.y + y, this.z + z);
    }

    public Coordinate moveNorth() {
        return this.move(0, 1, 0);
    }

    public Coordinate moveEast() {
        return this.move(1, 0, 0);
    }

    public Coordinate moveSouth() {
        return this.move(0, -1, 0);
    }

    public Coordinate moveWest() {
        return this.move(-1, 0, 0);
    }

    @Override
    public String toString() {
        return "(" + this.x + "," + this.y + "," + this.z + ")";
    }
}
