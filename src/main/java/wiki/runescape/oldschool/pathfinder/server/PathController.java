package wiki.runescape.oldschool.pathfinder.server;

import org.springframework.web.bind.annotation.*;
import wiki.runescape.oldschool.pathfinder.logic.*;

import java.io.IOException;
import java.util.Collection;

@CrossOrigin(origins = "*")
@RestController
public class PathController {

    private final Graph graph = new GraphBuilder().buildGraph();
    private final PathFinder pathFinder = new PathFinderDijkstraReverse();

    public PathController() throws IOException {
    }


    @PostMapping("api/path.json")
    public PathFinder.Result getPath(@RequestBody FindPathRequest pathRequest) {
        if(!this.graph.isWalkable(pathRequest.from()) || !this.graph.isWalkable(pathRequest.to())) {
            return new PathFinder.Result(false, null, 0);
        }

        return this.pathFinder.findPath(this.graph, pathRequest.from(), pathRequest.to(), pathRequest.blacklist());
    }

    @GetMapping("api/transports-teleports.json")
    public Collection<String> getAllTransportsTeleports() {
        return this.graph.getAllTeleportsTransports();
    }
}

