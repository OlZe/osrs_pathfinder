package wiki.runescape.oldschool.pathfinder.server;

import org.springframework.web.bind.annotation.*;
import wiki.runescape.oldschool.pathfinder.logic.Graph;
import wiki.runescape.oldschool.pathfinder.logic.GraphBuilder;
import wiki.runescape.oldschool.pathfinder.logic.PathFinder;
import wiki.runescape.oldschool.pathfinder.logic.PathFinderResult;

import java.io.IOException;
import java.util.Collection;

@CrossOrigin(origins = "*")
@RestController
public class PathController {

    private final Graph graph;
    private final PathFinder pathFinder;

    public PathController() throws IOException {
        this.graph = new GraphBuilder().buildGraph();
        this.pathFinder = new PathFinder();
    }

    @PostMapping("path.json")
    public PathFinderResult getPath(@RequestBody FindPathRequest pathRequest) {
        if(!this.graph.isWalkable(pathRequest.from()) || !this.graph.isWalkable(pathRequest.to())) {
            return new PathFinderResult(false, null, 0);
        }

        return this.pathFinder.findPath(this.graph, pathRequest.from(), pathRequest.to(), pathRequest.blacklist());
    }

    @GetMapping("/transports-teleports.json")
    public Collection<String> getAllTransportsTeleports() {
        return this.graph.getAllTeleportsTransports();
    }
}

