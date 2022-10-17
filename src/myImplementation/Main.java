package myImplementation;

import java.io.IOException;

public class Main {
    private static final Coordinate LUMBRIDGE_SPAWN = new Coordinate(3222,3218);
    private static final Coordinate VARROCK_SQUARE = new Coordinate(3213,3429);
    private static final Coordinate GE_SPIRIT_TREE = new Coordinate(3182,3507);
    private static final Coordinate TREE_GNOME_VILLAGE = new Coordinate(2534,3166);


    public static void main(String[] args) throws IOException {

        final GraphBuilder builder = new GraphBuilder();
        final Graph graph = builder.buildGraph();

        final PathFinder pathFinder = new PathFinder();

        // Infinite loop for debugging
        while(true) {
            Coordinate start = new Coordinate(3228, 3219);
            Coordinate end = new Coordinate(3228, 3219);
            PathFinderResult result = pathFinder.findPathBfs(graph, start, end);

            System.out.println("Need a command here so I can debug and see the result variable");
        }

    }
}
