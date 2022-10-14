package myImplementation;

import java.io.IOException;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {

        final WalkableGraphBuilder builder = new WalkableGraphBuilder();
        final Map<Point, GraphNode> graph = builder.readFileAndBuildGraph();

        final PathFinder pathFinder = new PathFinder();

        // Infinite loop for debugging
        while(true) {
            GraphNode start = graph.get(new Point(3228, 3219));
            Point end = new Point(3228, 3219);
            PathFinderResult result = pathFinder.findPathBfs(start, end);

            System.out.println("Need a command here so I can debug and see the result variable");
        }

    }
}
