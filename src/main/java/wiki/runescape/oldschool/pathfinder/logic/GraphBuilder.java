package wiki.runescape.oldschool.pathfinder.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wiki.runescape.oldschool.pathfinder.data_deserialization.DataDeserializer;
import wiki.runescape.oldschool.pathfinder.data_deserialization.jsonClasses.TransportJson;

import java.io.IOException;
import java.util.*;

public class GraphBuilder {


    public static final String WALK_NORTH_WEST = "walk north west";
    public static final String WALK_SOUTH_EAST = "walk south east";
    public static final String WALK_SOUTH_WEST = "walk south west";
    public static final String WALK_NORTH_EAST = "walk north east";
    public static final String WALK_EAST = "walk east";
    public static final String WALK_WEST = "walk west";
    public static final String WALK_NORTH = "walk north";
    public static final String WALK_SOUTH = "walk south";

    private final Logger logger = LoggerFactory.getLogger(GraphBuilder.class);

    /**
     * Reads the json files and builds a graph structure
     *
     * @return The graph
     * @throws IOException can't read files
     */
    public Graph buildGraph() throws IOException {
        final Logger logger = LoggerFactory.getLogger(GraphBuilder.class);

        // Deserialize Json
        final Map<Coordinate, DataDeserializer.TileObstacles> tiles = new DataDeserializer().deserializeMovementData();
        logger.info("deserialize data");

        // Build walkable graph
        final Map<Coordinate, GraphVertex> graphVertices = this.mapOfTilesToWalkableGraph(tiles);
        logger.info("link walkable vertices");

        // Deserialize transport Data
        final TransportJson[] cooksTransportData = new DataDeserializer().deserializeTransportData();
        logger.info("deserialize cooks transport data");

        // process transports
        final Set<Teleport> teleports = this.addTransports(graphVertices, cooksTransportData);
        logger.info("process cooks transports");

        // Deserialize Skretzo Data
        final TransportJson[] skretzoTransports = new DataDeserializer().deserializeSkretzoData();
        logger.info("deserialize skretzo transport data");

        // process skretzo transports
        this.addTransports(graphVertices, skretzoTransports);
        logger.info("process skretzo transports");

        return new Graph(graphVertices, teleports);
    }

    /**
     * @param tileMap A Map of Tiles
     * @return A Graph linking all walkable tiles together
     */
    private Map<Coordinate, GraphVertex> mapOfTilesToWalkableGraph(Map<Coordinate, DataDeserializer.TileObstacles> tileMap) {
        Map<Coordinate, GraphVertex> graph = new HashMap<>();

        // Put tiles into graph
        for (Map.Entry<Coordinate, DataDeserializer.TileObstacles> tile : tileMap.entrySet()) {
            final Coordinate coordinate = tile.getKey();
            final GraphVertex vertex = new GraphVertex(coordinate);
            graph.put(coordinate, vertex);
        }

        // Pass 1
        // Link vertical/horizontal walking ways *first* so they get prioritized when pathfinding
        // This is important to avoid zig-zag paths which are the same length but not user friendly
        // Link north and east neighbours
        for(Map.Entry<Coordinate, DataDeserializer.TileObstacles> tile : tileMap.entrySet()) {
            final Coordinate coordinate = tile.getKey();
            final GraphVertex vertex = graph.get(coordinate);

            // North
            final GraphVertex northVertex = graph.get(coordinate.moveNorth());
            if (northVertex != null && this.canMoveNorth(coordinate, tileMap)) {
                vertex.addEdgeTo(northVertex, (byte) 1, WALK_NORTH);
                northVertex.addEdgeTo(vertex, (byte) 1, WALK_SOUTH);
            }

            // East
            final GraphVertex eastVertex = graph.get(coordinate.moveEast());
            if (eastVertex != null && this.canMoveEast(coordinate, tileMap)) {
                vertex.addEdgeTo(eastVertex, (byte) 1, WALK_EAST);
                eastVertex.addEdgeTo(vertex, (byte) 1, WALK_WEST);
            }
        }

        // Pass 2
        // Link diagonal paths
        // Link north east and south east neighbours
        for (Map.Entry<Coordinate, DataDeserializer.TileObstacles> tile : tileMap.entrySet()) {
            final Coordinate coordinate = tile.getKey();
            final GraphVertex vertex = graph.get(coordinate);

            // North East
            final GraphVertex northEastVertex = graph.get(coordinate.moveNorth().moveEast());
            if (northEastVertex != null && this.canMoveNorthEast(coordinate, tileMap)) {
                vertex.addEdgeTo(northEastVertex, (byte) 1, WALK_NORTH_EAST);
                northEastVertex.addEdgeTo(vertex, (byte) 1, WALK_SOUTH_WEST);
            }

            // South East
            final GraphVertex southEastVertex = graph.get(coordinate.moveSouth().moveEast());
            if (southEastVertex != null && this.canMoveSouthEast(coordinate, tileMap)) {
                vertex.addEdgeTo(southEastVertex, (byte) 1, WALK_SOUTH_EAST);
                southEastVertex.addEdgeTo(vertex, (byte) 1, WALK_NORTH_WEST);
            }
        }

        return graph;
    }

    /**
     * links vertices according to point-to-point transports (fairy rings etc.).
     * Returns a Set of Teleports
     *
     * @param graph      The vertices of which just walkable ones have been linked
     * @param transports The deserialized transport data
     */
    private Set<Teleport> addTransports(Map<Coordinate, GraphVertex> graph, TransportJson[] transports) {
        final Set<Teleport> teleports = new HashSet<>();
        for (TransportJson transport : transports) {
            assert (transport.end != null);

            if (transport.start == null) {
                addTeleportIfExists(graph, teleports, transport);
            } else {
                linkVerticesIfExists(graph, transport);
            }
        }
        return teleports;
    }

    private void addTeleportIfExists(final Map<Coordinate, GraphVertex> graph,
                                            final Set<Teleport> teleports, final TransportJson transport) {
        final Coordinate tpDest = new Coordinate(transport.end.x, transport.end.y, transport.end.z);
        final GraphVertex tpDestVertex = graph.get(tpDest);
        if (tpDestVertex == null) {
            this.logger.warn("Vertex " + tpDest + " for Teleport '" + transport.title + "' is not in graph.");
            return;
        }
        teleports.add(new Teleport(tpDestVertex, transport.title, transport.duration));
    }

    /**
     * Adds an edge in the graph according to the given transport
     * Does not add duplicate edges
     *
     * @param graph The graph
     * @param transport The transportJson
     */
    private void linkVerticesIfExists(final Map<Coordinate, GraphVertex> graph, final TransportJson transport) {
        final Coordinate startCoord = new Coordinate(transport.start.x, transport.start.y, transport.start.z);
        final Coordinate endCoord = new Coordinate(transport.end.x, transport.end.y, transport.end.z);
        final GraphVertex startVertex = graph.get(startCoord);
        final GraphVertex endVertex = graph.get(endCoord);

        if (startVertex == null || endVertex == null) {
            this.logger.warn("Vertices for Transport '" + transport.title + "' from " + transport.start + " to " + transport.end + " are not in graph.");
            return;
        }

        final Optional<GraphEdge> existingEdge = startVertex.neighbors.stream().filter(e -> e.to().coordinate.equals(endCoord)).findAny();
        if (existingEdge.isPresent()) {
            this.logger.warn("Duplicate edge " + startCoord + "->" + endCoord + ": "
                    + "Existing edge: '" + existingEdge.get().methodOfMovement() + "' cost " + existingEdge.get().cost() + "; "
                    + "Ignoring new edge: '" + transport.title + "' cost " + transport.duration + ".");
            return;
        }
        startVertex.addEdgeTo(endVertex, transport.duration, transport.title);
    }

    private boolean canMoveNorth(Coordinate coordinate, Map<Coordinate, DataDeserializer.TileObstacles> tileMap) {
        final DataDeserializer.TileObstacles northTileObstacles = tileMap.get(coordinate.moveNorth());
        final DataDeserializer.TileObstacles originTileObstacles = tileMap.get(coordinate);
        return northTileObstacles != null && !originTileObstacles.northBlocked && !northTileObstacles.southBlocked;
    }

    private boolean canMoveEast(Coordinate coordinate, Map<Coordinate, DataDeserializer.TileObstacles> tileMap) {
        final DataDeserializer.TileObstacles eastTileObstacles = tileMap.get(coordinate.moveEast());
        final DataDeserializer.TileObstacles originTileObstacles = tileMap.get(coordinate);
        return eastTileObstacles != null && !originTileObstacles.eastBlocked && !eastTileObstacles.westBlocked;
    }

    private boolean canMoveSouth(Coordinate coordinate, Map<Coordinate, DataDeserializer.TileObstacles> tileMap) {
        final DataDeserializer.TileObstacles southTileObstacles = tileMap.get(coordinate.moveSouth());
        final DataDeserializer.TileObstacles originTileObstacles = tileMap.get(coordinate);
        return southTileObstacles != null && !originTileObstacles.southBlocked && !southTileObstacles.northBlocked;
    }

    private boolean canMoveWest(Coordinate coordinate, Map<Coordinate, DataDeserializer.TileObstacles> tileMap) {
        final DataDeserializer.TileObstacles westTileObstacles = tileMap.get(coordinate.moveWest());
        final DataDeserializer.TileObstacles originTileObstacles = tileMap.get(coordinate);
        return westTileObstacles != null && !originTileObstacles.westBlocked && !westTileObstacles.eastBlocked;
    }

    private boolean canMoveNorthEast(Coordinate coordinate, Map<Coordinate, DataDeserializer.TileObstacles> tileMap) {
        return this.canMoveEast(coordinate, tileMap) &&
                this.canMoveNorth(coordinate.moveEast(), tileMap) &&
                this.canMoveNorth(coordinate, tileMap) &&
                this.canMoveEast(coordinate.moveNorth(), tileMap);
    }

    private boolean canMoveSouthEast(Coordinate coordinate, Map<Coordinate, DataDeserializer.TileObstacles> tileMap) {
        return this.canMoveEast(coordinate, tileMap) &&
                this.canMoveSouth(coordinate.moveEast(), tileMap) &&
                this.canMoveSouth(coordinate, tileMap) &&
                this.canMoveEast(coordinate.moveSouth(), tileMap);
    }

    private boolean canMoveSouthWest(Coordinate coordinate, Map<Coordinate, DataDeserializer.TileObstacles> tileMap) {
        return this.canMoveWest(coordinate, tileMap) &&
                this.canMoveSouth(coordinate.moveWest(), tileMap) &&
                this.canMoveSouth(coordinate, tileMap) &&
                this.canMoveWest(coordinate.moveSouth(), tileMap);
    }

    private boolean canMoveNorthWest(Coordinate coordinate, Map<Coordinate, DataDeserializer.TileObstacles> tileMap) {
        return this.canMoveWest(coordinate, tileMap) &&
                this.canMoveNorth(coordinate.moveWest(), tileMap) &&
                this.canMoveNorth(coordinate, tileMap) &&
                this.canMoveWest(coordinate.moveNorth(), tileMap);
    }

}
