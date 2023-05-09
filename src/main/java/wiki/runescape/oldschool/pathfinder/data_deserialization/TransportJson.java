package wiki.runescape.oldschool.pathfinder.data_deserialization;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;

public record TransportJson(Coordinate from,
                            Coordinate to,
                            String title,
                            byte duration) {

}
