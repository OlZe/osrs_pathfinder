package wiki.runescape.oldschool.pathfinder.server;

import org.springframework.web.bind.annotation.*;
import wiki.runescape.oldschool.pathfinder.logic.graph.Graph;
import wiki.runescape.oldschool.pathfinder.logic.graph.GraphBuilder;
import wiki.runescape.oldschool.pathfinder.logic.pathfinder.*;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingArrayQueue;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingBucketQueue;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingPriorityQueue;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class PathController {

    private final Graph graph = new GraphBuilder().buildGraph();
    private final wiki.runescape.oldschool.pathfinder.logic.graph.unweighted.Graph unweightedGraph = new wiki.runescape.oldschool.pathfinder.logic.graph.unweighted.GraphBuilder().buildUnweightedGraph(graph);

    private final Map<String, Pathfinder> ALGORITHM_STRING_TO_CLASS = Map.of(
            "Dijkstra / PriorityQueue", new PathfinderDijkstra(graph, PathfindingPriorityQueue.class),
            "Dijkstra / BucketQueue", new PathfinderDijkstra(graph, PathfindingBucketQueue.class),
            "Dijkstra / ArrayQueue", new PathfinderDijkstra(graph, PathfindingArrayQueue.class),
            "ReverseDijkstra / PriorityQueue", new PathfinderDijkstraReverse(graph, PathfindingPriorityQueue.class),
            "ReverseDijkstra / BucketQueue", new PathfinderDijkstraReverse(graph, PathfindingBucketQueue.class),
            "ReverseDijkstra / ArrayQueue", new PathfinderDijkstraReverse(graph, PathfindingArrayQueue.class),
            "ForwardReverseDijkstra / PriorityQueue", new PathfinderDijkstraForwardReverse(graph, PathfindingPriorityQueue.class),
            "ForwardReverseDijkstra / BucketQueue", new PathfinderDijkstraForwardReverse(graph, PathfindingBucketQueue.class),
            "ForwardReverseDijkstra / ArrayQueue", new PathfinderDijkstraForwardReverse(graph, PathfindingArrayQueue.class),
            "Breadth-First-Search / UnweightedQueue", new PathfinderBFS(unweightedGraph)
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
}

