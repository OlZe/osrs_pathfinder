package wiki.runescape.oldschool.server;

import wiki.runescape.oldschool.logic.Coordinate;

import java.util.Set;

public record PathRequestJson(
    Coordinate from,
    Coordinate to,
    Set<String> blacklist
) {
}
