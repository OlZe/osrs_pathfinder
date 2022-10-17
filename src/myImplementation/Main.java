package myImplementation;

import java.io.IOException;

public class Main {
    private static final Point LUMBRIDGE_SPAWN = new Point(3222,3218);
    private static final Point VARROCK_SQUARE = new Point(3213,3429);
    private static final Point GE_SPIRIT_TREE = new Point(3182,3507);
    private static final Point TREE_GNOME_VILLAGE = new Point(2534,3166);


    public static void main(String[] args) throws IOException {

        final GraphBuilder builder = new GraphBuilder();
        final Graph graph = builder.buildGraph();

        final PathFinder pathFinder = new PathFinder();

        // Infinite loop for debugging
        while(true) {
            Point start = new Point(3228, 3219);
            Point end = new Point(3228, 3219);
            PathFinderResult result = pathFinder.findPathBfs(graph, start, end);

            System.out.println("Need a command here so I can debug and see the result variable");
        }

    }
}
