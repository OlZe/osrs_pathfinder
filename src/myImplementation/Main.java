package myImplementation;

import java.io.IOException;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {

        final WalkableGraphBuilder builder = new WalkableGraphBuilder();
        final Map<Point, GraphNode> graph = builder.readFileAndBuildGraph();




        System.out.println("exit");
    }
}
