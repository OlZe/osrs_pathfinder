package wiki.runescape.oldschool.pathfinder.logic;

public record PositionInfo(
        Coordinate coordinate,
        boolean northBlocked,
        boolean eastBlocked,
        boolean southBlocked,
        boolean westBlocked,

        WildernessLevels wildernessLevel
) {
    
    public enum WildernessLevels {
        ABOVE30,
        BETWEEN20AND30,
        BELOW20
    }
}
