package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;

import java.util.HashSet;

public interface Pathfinder {
    PathfinderResult findPath(Coordinate start, Coordinate end, HashSet<String> blacklist);
}
