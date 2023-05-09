package wiki.runescape.oldschool.pathfinder.data_deserialization;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;

public record PositionInfo(
        Coordinate coordinate,
        boolean northBlocked,
        boolean eastBlocked,
        boolean southBlocked,
        boolean westBlocked,

        WildernessLevels wildernessLevel
) {

}
