package wiki.runescape.oldschool.pathfinder.server;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;

import java.util.Set;

public record FindPathRequest(
    Coordinate from,
    Coordinate to,
    Set<String> blacklist,

    String algorithm
) {
}
