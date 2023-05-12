package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.graph.Graph;

import java.util.Set;

public abstract class Pathfinder {

    final protected Graph graph;

    protected Pathfinder(final Graph graph) {
        this.graph = graph;
    }


    public abstract PathfinderResult findPath(Coordinate start, Coordinate end, Set<String> blacklist);

}
