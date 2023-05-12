package wiki.runescape.oldschool.pathfinder.server;

import org.springframework.web.bind.annotation.*;
import wiki.runescape.oldschool.pathfinder.logic.graph.Graph;
import wiki.runescape.oldschool.pathfinder.logic.graph.GraphBuilder;
import wiki.runescape.oldschool.pathfinder.logic.pathfinder.Pathfinder;
import wiki.runescape.oldschool.pathfinder.logic.pathfinder.PathfinderDijkstra;
import wiki.runescape.oldschool.pathfinder.logic.pathfinder.PathfinderDijkstraReverse;
import wiki.runescape.oldschool.pathfinder.logic.pathfinder.PathfinderResult;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class PathController {

    private final Graph graph = new GraphBuilder().buildGraph();

    private final Map<String, Pathfinder> ALGORITHM_STRING_TO_CLASS = Map.of(
            "dijkstra", new PathfinderDijkstra(graph),
            "dijkstra-reverse", new PathfinderDijkstraReverse(graph)
    );

    public PathController() throws IOException {
    }


    @PostMapping("api/path.json")
    public PathfinderResult getPath(@RequestBody FindPathRequest pathRequest) {

        final Pathfinder pathFinder = ALGORITHM_STRING_TO_CLASS.get(pathRequest.algorithm());

        if (pathFinder == null) {
            final String errorMsg = "Field 'algorithm' of request contains invalid value: " + pathRequest.algorithm() +
                    "\nAllowed values are: " + String.join(", ", ALGORITHM_STRING_TO_CLASS.keySet());
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

