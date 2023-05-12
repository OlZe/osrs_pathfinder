package wiki.runescape.oldschool.pathfinder.logic.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wiki.runescape.oldschool.pathfinder.data_deserialization.DataDeserializer;
import wiki.runescape.oldschool.pathfinder.data_deserialization.PositionInfo;
import wiki.runescape.oldschool.pathfinder.data_deserialization.TeleportJson;
import wiki.runescape.oldschool.pathfinder.data_deserialization.TransportJson;
import wiki.runescape.oldschool.pathfinder.logic.Coordinate;

import java.io.IOException;
import java.util.*;

public class GraphBuilder {
    public static final String WALK_PREFIX = "walk";
    public static final String WALK_NORTH_WEST = WALK_PREFIX + " north west";
    public static final String WALK_SOUTH_EAST = WALK_PREFIX + " south east";
    public static final String WALK_SOUTH_WEST = WALK_PREFIX + " south west";
    public static final String WALK_NORTH_EAST = WALK_PREFIX + " north east";
    public static final String WALK_EAST = WALK_PREFIX + " east";
    public static final String WALK_WEST = WALK_PREFIX + " west";
    public static final String WALK_NORTH = WALK_PREFIX + " north";
    public static final String WALK_SOUTH = WALK_PREFIX + " south";

    private final Logger logger = LoggerFactory.getLogger(GraphBuilder.class);

    public Graph buildGraph() throws IOException {
        final Logger logger = LoggerFactory.getLogger(GraphBuilder.class);

        // Deserialize map data
        logger.info("deserialize map data");
        final DataDeserializer.MapData mapData = new DataDeserializer().deserializeMapData();
        logger.info("Read " + mapData.walkableTiles().size() + " tiles");
        logger.info("Read " + mapData.teleports().size() + " teleports");
        logger.info("Read " + mapData.transports().size() + " transports");

        // Build walkable graph
        logger.info("link vertices by walkability");
        final Map<Coordinate, GraphVertex> graphVertices = this.mapOfTilesToWalkableGraph(mapData.walkableTiles());

        // Process transports
        logger.info("link vertices by transports");
        this.addTransports(graphVertices, mapData.transports());

        // Process teleports
        logger.info("link teleports to vertices");
        Collection<Teleport> teleports = this.makeTeleports(graphVertices, mapData.teleports());

        logger.info("done");
        return new Graph(graphVertices, teleports);
    }

    private Collection<Teleport> makeTeleports(final Map<Coordinate, GraphVertex> graphVertices, final Collection<TeleportJson> teleportsJson) {
        List<Teleport> teleports = new LinkedList<>();
        for(TeleportJson teleportJson : teleportsJson) {
            final GraphVertex to = graphVertices.get(teleportJson.to());
            if(to == null) {
                logger.warn("Vertex for Teleport " + teleportJson + " does not exist in graph");
                continue;
            }
            teleports.add(new Teleport(
                    to,
                    teleportJson.title(),
                    teleportJson.duration(),
                    teleportJson.canTeleportUpTo30Wildy()
            ));
        }
        return teleports;
    }


    /**
     * @param tileMap A Map of Tiles
     * @return A Graph linking all walkable tiles together
     */
    private Map<Coordinate, GraphVertex> mapOfTilesToWalkableGraph(Map<Coordinate, PositionInfo> tileMap) {
        Map<Coordinate, GraphVertex> graph = new HashMap<>();

        // Put tiles into graph
        tileMap.forEach((coordinate, value) -> {
            final GraphVertex vertex = new GraphVertex(coordinate, new LinkedList<>(), new LinkedList<>(), value.wildernessLevel());
            graph.put(coordinate, vertex);
        });

        // Pass 1
        // Link vertical/horizontal walking ways *first* so they get prioritized when pathfinding
        // This is important to avoid zigzag paths which are the same length but not user-friendly
        tileMap.forEach((coordinate, value) -> {
            final GraphVertex vertex = graph.get(coordinate);

            // North
            final GraphVertex northVertex = graph.get(coordinate.moveNorth());
            if (northVertex != null && this.canMoveNorth(coordinate, tileMap)) {
                vertex.addEdgeTo(northVertex, 0.5f , WALK_NORTH, true);
                northVertex.addEdgeTo(vertex, 0.5f, WALK_SOUTH, true);
            }

            // East
            final GraphVertex eastVertex = graph.get(coordinate.moveEast());
            if (eastVertex != null && this.canMoveEast(coordinate, tileMap)) {
                vertex.addEdgeTo(eastVertex, 0.5f, WALK_EAST, true);
                eastVertex.addEdgeTo(vertex, 0.5f, WALK_WEST, true);
            }
        });

        // Pass 2
        // Link diagonal paths
        tileMap.forEach((coordinate, value) -> {
            final GraphVertex vertex = graph.get(coordinate);

            // North East
            final GraphVertex northEastVertex = graph.get(coordinate.moveNorth().moveEast());
            if (northEastVertex != null && this.canMoveNorthEast(coordinate, tileMap)) {
                vertex.addEdgeTo(northEastVertex, 0.5f, WALK_NORTH_EAST, true);
                northEastVertex.addEdgeTo(vertex, 0.5f, WALK_SOUTH_WEST, true);
            }

            // South East
            final GraphVertex southEastVertex = graph.get(coordinate.moveSouth().moveEast());
            if (southEastVertex != null && this.canMoveSouthEast(coordinate, tileMap)) {
                vertex.addEdgeTo(southEastVertex, 0.5f, WALK_SOUTH_EAST, true);
                southEastVertex.addEdgeTo(vertex, 0.5f, WALK_NORTH_WEST, true);
            }
        });

        return graph;
    }

    /**
     * links vertices according to point-to-point transports.
     *
     * @param graph      The vertices of which just walkable ones have been linked
     * @param transports The deserialized transport data
     */
    private void addTransports(Map<Coordinate, GraphVertex> graph, Collection<TransportJson> transports) {
        transports.forEach(transport -> {
            final GraphVertex fromVertex = graph.get(transport.from());
            final GraphVertex toVertex = graph.get(transport.to());

            if (fromVertex == null || toVertex == null) {
                this.logger.warn("Vertices for Transport '" + transport.title() + "' from " + transport.from() + " to " + transport.to() + " are not in graph.");
                return;
            }

            // Duplicate edge detection
            final Optional<GraphEdge> duplicateEdge = fromVertex.edgesOut().stream()
                    .filter(edge -> edge.to().coordinate().equals(toVertex.coordinate()))
                    .findAny();

            if (duplicateEdge.isPresent()) {
                this.logger.warn("Duplicate edge " + fromVertex.coordinate() + "->" + toVertex.coordinate() + ": "
                        + "Existing edge: '" + duplicateEdge.get().title() + "' cost " + duplicateEdge.get().cost() + "; "
                        + "Ignoring new edge: '" + transport.title() + "' cost " + transport.duration() + ".");
                return;
            }
            fromVertex.addEdgeTo(toVertex, transport.duration(), transport.title(), false);
        });
    }

    private boolean canMoveNorth(Coordinate coordinate, Map<Coordinate, PositionInfo> tileMap) {
        final PositionInfo northTileObstacles = tileMap.get(coordinate.moveNorth());
        final PositionInfo originTileObstacles = tileMap.get(coordinate);
        return northTileObstacles != null && !originTileObstacles.northBlocked() && !northTileObstacles.southBlocked();
    }

    private boolean canMoveEast(Coordinate coordinate, Map<Coordinate, PositionInfo> tileMap) {
        final PositionInfo eastTileObstacles = tileMap.get(coordinate.moveEast());
        final PositionInfo originTileObstacles = tileMap.get(coordinate);
        return eastTileObstacles != null && !originTileObstacles.eastBlocked() && !eastTileObstacles.westBlocked();
    }

    private boolean canMoveSouth(Coordinate coordinate, Map<Coordinate, PositionInfo> tileMap) {
        final PositionInfo southTileObstacles = tileMap.get(coordinate.moveSouth());
        final PositionInfo originTileObstacles = tileMap.get(coordinate);
        return southTileObstacles != null && !originTileObstacles.southBlocked() && !southTileObstacles.northBlocked();
    }

    private boolean canMoveWest(Coordinate coordinate, Map<Coordinate, PositionInfo> tileMap) {
        final PositionInfo westTileObstacles = tileMap.get(coordinate.moveWest());
        final PositionInfo originTileObstacles = tileMap.get(coordinate);
        return westTileObstacles != null && !originTileObstacles.westBlocked() && !westTileObstacles.eastBlocked();
    }

    private boolean canMoveNorthEast(Coordinate coordinate, Map<Coordinate, PositionInfo> tileMap) {
        return this.canMoveEast(coordinate, tileMap) &&
                this.canMoveNorth(coordinate.moveEast(), tileMap) &&
                this.canMoveNorth(coordinate, tileMap) &&
                this.canMoveEast(coordinate.moveNorth(), tileMap);
    }

    private boolean canMoveSouthEast(Coordinate coordinate, Map<Coordinate, PositionInfo> tileMap) {
        return this.canMoveEast(coordinate, tileMap) &&
                this.canMoveSouth(coordinate.moveEast(), tileMap) &&
                this.canMoveSouth(coordinate, tileMap) &&
                this.canMoveEast(coordinate.moveSouth(), tileMap);
    }

    private boolean canMoveSouthWest(Coordinate coordinate, Map<Coordinate, PositionInfo> tileMap) {
        return this.canMoveWest(coordinate, tileMap) &&
                this.canMoveSouth(coordinate.moveWest(), tileMap) &&
                this.canMoveSouth(coordinate, tileMap) &&
                this.canMoveWest(coordinate.moveSouth(), tileMap);
    }

    private boolean canMoveNorthWest(Coordinate coordinate, Map<Coordinate, PositionInfo> tileMap) {
        return this.canMoveWest(coordinate, tileMap) &&
                this.canMoveNorth(coordinate.moveWest(), tileMap) &&
                this.canMoveNorth(coordinate, tileMap) &&
                this.canMoveWest(coordinate.moveNorth(), tileMap);
    }

}
