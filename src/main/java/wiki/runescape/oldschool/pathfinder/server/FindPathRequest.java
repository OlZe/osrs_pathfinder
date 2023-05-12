package wiki.runescape.oldschool.pathfinder.server;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;

import java.util.HashSet;

public record FindPathRequest(
    Coordinate from,
    Coordinate to,
    HashSet<String> blacklist,

    String algorithm
) {
}
