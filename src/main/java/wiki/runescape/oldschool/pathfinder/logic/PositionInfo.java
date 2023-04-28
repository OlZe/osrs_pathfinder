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
        /** Wilderness Level > 30 */
        ABOVE30,
        /** Wilderness Level  > 20 and <= 30 */
        BETWEEN20AND30,
        /** Wilderness Level <= 20 */
        BELOW20
    }
}
