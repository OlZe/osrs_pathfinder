package wiki.runescape.oldschool.pathfinder.server;

import org.springframework.web.bind.annotation.*;
import wiki.runescape.oldschool.pathfinder.logic.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
public class PathController {

    private final Graph graph = new GraphBuilder().buildGraph();

    private final Map<String, PathFinder> ALGORITHM_STRING_TO_CLASS = Map.of(
            "dijkstra", new PathFinderDijkstra(),
            "dijkstra-reverse", new PathFinderDijkstraReverse(graph.teleports())
    );

    public PathController() throws IOException {
    }


    @PostMapping("api/path.json")
    public PathFinder.Result getPath(@RequestBody FindPathRequest pathRequest) {
        if (!this.graph.isWalkable(pathRequest.from()) || !this.graph.isWalkable(pathRequest.to())) {
            return new PathFinder.Result(false, null, 0, 0, 0, 0);
        }

        final PathFinder pathFinder = ALGORITHM_STRING_TO_CLASS.get(pathRequest.algorithm());
        if (pathFinder == null) {
            throw new IllegalArgumentException("Field 'algorithm' of request contains invalid value: " + pathRequest.algorithm() +
                    "\nAllowed values are: " + ALGORITHM_STRING_TO_CLASS.keySet().stream().collect(Collectors.joining(", ")));
        }

        return pathFinder.findPath(this.graph, pathRequest.from(), pathRequest.to(), pathRequest.blacklist());
    }

    @GetMapping("api/transports-teleports.json")
    public Collection<String> getAllTransportsTeleports() {
        return this.graph.getAllTeleportsTransports();
    }
}

