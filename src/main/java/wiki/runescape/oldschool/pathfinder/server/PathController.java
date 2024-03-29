package wiki.runescape.oldschool.pathfinder.server;

import org.springframework.web.bind.annotation.*;
import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.graph.Graph;
import wiki.runescape.oldschool.pathfinder.logic.graph.GraphBuilder;
import wiki.runescape.oldschool.pathfinder.logic.pathfinder.*;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingArrayQueue;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingBucketQueue;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingPriorityQueue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

@CrossOrigin(origins = "*")
@RestController
public class PathController {

    private final Graph graph = new GraphBuilder().buildGraph();
    private final wiki.runescape.oldschool.pathfinder.logic.graph.unweighted.Graph unweightedGraph = new wiki.runescape.oldschool.pathfinder.logic.graph.unweighted.GraphBuilder().buildUnweightedGraph(graph);

    private final Map<String, Pathfinder> ALGORITHM_STRING_TO_CLASS = Map.of(
            "Dijkstra / PriorityQueue", new PathfinderDijkstra(graph, PathfindingPriorityQueue.class),
            "Dijkstra / BucketQueue", new PathfinderDijkstra(graph, PathfindingBucketQueue.class),
            "Dijkstra / ArrayQueue", new PathfinderDijkstra(graph, PathfindingArrayQueue.class),
            "Dijkstra-Backwards / PriorityQueue", new PathfinderDijkstraBackwards(graph, PathfindingPriorityQueue.class),
            "Dijkstra-Backwards / BucketQueue", new PathfinderDijkstraBackwards(graph, PathfindingBucketQueue.class),
            "Dijkstra-Backwards / ArrayQueue", new PathfinderDijkstraBackwards(graph, PathfindingArrayQueue.class),
            "BFS / UnweightedQueue", new PathfinderBfs(unweightedGraph),
            "BFS-Backwards / UnweightedQueue", new PathfinderBfsBackwards(unweightedGraph),
            "BFS-MeetInMiddle / UnweightedQueue", new PathfinderBfsMeetInMiddle(unweightedGraph),
            "BFS-MeetAtTeleport / UnweightedQueue", new PathfinderBfsMeetAtTeleport(unweightedGraph)
    );

    public PathController() throws IOException {
    }


    @PostMapping("api/path.json")
    public PathfinderResult getPath(@RequestBody FindPathRequest pathRequest) {

        final Pathfinder pathFinder = ALGORITHM_STRING_TO_CLASS.get(pathRequest.algorithm());

        if (pathFinder == null) {
            final String errorMsg = "Field 'algorithm' of request contains invalid value: "
                    + pathRequest.algorithm() + "\nAllowed values are: "
                    + String.join(", ", ALGORITHM_STRING_TO_CLASS.keySet());
            return new PathfinderResult(false, null, 0, 0, 0, 0, errorMsg);
        }

        return pathFinder.findPath(pathRequest.from(), pathRequest.to(), pathRequest.blacklist());
    }

    @GetMapping("api/transports-teleports.json")
    public Collection<String> getAllTransportsTeleports() {
        return this.graph.getAllTeleportsTransports();
    }

    @GetMapping("api/algorithms.json")
    public Collection<String> getAllAlgorithms() {
        return ALGORITHM_STRING_TO_CLASS.keySet();
    }

    @GetMapping("api/debug/randomStartEndCoordinates.json")
    public CoordinatePair getRandomStartEndVertices() {
        final ArrayList<Coordinate> coordinates = new ArrayList<>(this.graph.vertices().keySet());
        final Random random = new Random();
        final int i1 = random.nextInt(coordinates.size());
        final int i2 = random.nextInt(coordinates.size());
        return new CoordinatePair(coordinates.get(i1), coordinates.get(i2));
    }


    public record CoordinatePair(
            Coordinate start,
            Coordinate end
    ) {
    }
}

