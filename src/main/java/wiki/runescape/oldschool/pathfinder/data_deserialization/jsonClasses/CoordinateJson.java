package wiki.runescape.oldschool.pathfinder.data_deserialization.jsonClasses;

public class CoordinateJson {
    public int x;
    public int y;
    public int z;

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }
}