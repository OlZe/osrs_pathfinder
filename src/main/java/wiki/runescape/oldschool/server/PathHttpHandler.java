package wiki.runescape.oldschool.server;

import wiki.runescape.oldschool.logic.*;

public class PathHttpHandler extends JsonHttpHandler<PathRequestJson> {

    private final Graph graph;
    private final PathFinder pathFinder;

    public PathHttpHandler(Graph graph, PathFinder pathFinder) {
        super(PathRequestJson.class);
        this.graph = graph;
        this.pathFinder = pathFinder;
    }

    @Override
    protected Reply handle(final Request<PathRequestJson> request) {
        PathRequestJson requestBody = request.requestBody();

        if(requestBody.from() == null || requestBody.to() == null || requestBody.blacklist() == null) {
            return new Reply(true, "Request body is missing fields");
        }
        if (!graph.isWalkable(requestBody.from()) && !graph.isWalkable(requestBody.to())) {
            return new Reply(true, "Start and end tile are not walkable");
        }
        if (!graph.isWalkable(requestBody.from())) {
            return new Reply(true, "Start tile is not walkable");
        }
        if (!graph.isWalkable(requestBody.to())) {
            return new Reply(true, "End tile is not walkable");
        }

        final PathFinderResult path = pathFinder.findPath(graph, requestBody.from(), requestBody.to(), requestBody.blacklist());
        return new Reply(false, path);
    }
}
