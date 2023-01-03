package wiki.runescape.oldschool.pathfinder.logic;

import wiki.runescape.oldschool.pathfinder.data_deserialization.DataDeserializer;
import wiki.runescape.oldschool.pathfinder.TimeLogger;
import wiki.runescape.oldschool.pathfinder.data_deserialization.jsonClasses.CoordinateJson;
import wiki.runescape.oldschool.pathfinder.data_deserialization.jsonClasses.MovementJson;
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

    /**
     * Reads the json files and builds a graph structure
     *
     * @return The graph
     * @throws IOException can't read files
     */
    public Graph buildGraph() throws IOException {
        TimeLogger log = new TimeLogger();
        log.start("Graph Builder");

        // Deserialize Json
        final MovementJson movementJson = new DataDeserializer().deserializeMovementData();
        log.lap("deserialize movement data");

        // Build Tile Map
        final Map<Coordinate, TileObstacles> tiles = this.movementDataToMapOfTiles(movementJson);
        log.lap("build tile map");

        // Build walkable graph
        final Map<Coordinate, GraphVertex> graphVertices = this.mapOfTilesToWalkableGraph(tiles);
        log.lap("link walkable vertices");

        // Deserialize transport Data
        final TransportJson[] cooksTransportData = new DataDeserializer().deserializeTransportData();
        log.lap("deserialize cooks transport data");

        // process transports
        final Set<Teleport> teleports = this.addTransports(graphVertices, cooksTransportData);
        log.lap("process cooks transports");

        // Deserialize Skretzo Data
        final TransportJson[] skretzoTransports = new DataDeserializer().deserializeSkretzoData();
        log.lap("deserialize skretzo transport data");

        // process skretzo transports
        this.addTransports(graphVertices, skretzoTransports);
        log.lap("process skretzo transports");

        log.end();
        return new Graph(graphVertices, teleports);
    }

    /**
     * @param tileMap A Map of Tiles
     * @return A Graph linking all walkable tiles together
     */
    private Map<Coordinate, GraphVertex> mapOfTilesToWalkableGraph(Map<Coordinate, TileObstacles> tileMap) {
        Map<Coordinate, GraphVertex> graph = new HashMap<>();

        // Put tiles into graph
        for (Map.Entry<Coordinate, TileObstacles> tile : tileMap.entrySet()) {
            final Coordinate coordinate = tile.getKey();
            final GraphVertex vertex = new GraphVertex(coordinate);
            graph.put(coordinate, vertex);
        }

        // Pass 1
        // Link vertical/horizontal walking ways *first* so they get prioritized when pathfinding
        // This is important to avoid zig-zag paths which are the same length but not user friendly
        // Link north and east neighbours
        for(Map.Entry<Coordinate, TileObstacles> tile : tileMap.entrySet()) {
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
        for (Map.Entry<Coordinate, TileObstacles> tile : tileMap.entrySet()) {
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

    private static void addTeleportIfExists(final Map<Coordinate, GraphVertex> graph,
                                            final Set<Teleport> teleports, final TransportJson transport) {
        final Coordinate tpDest = new Coordinate(transport.end.x, transport.end.y, transport.end.z);
        final GraphVertex tpDestVertex = graph.get(tpDest);
        if (tpDestVertex == null) {
            System.err.println("Vertex " + tpDest + " for Teleport '" + transport.title + "' is not in graph.");
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
    private static void linkVerticesIfExists(final Map<Coordinate, GraphVertex> graph, final TransportJson transport) {
        final Coordinate startCoord = new Coordinate(transport.start.x, transport.start.y, transport.start.z);
        final Coordinate endCoord = new Coordinate(transport.end.x, transport.end.y, transport.end.z);
        final GraphVertex startVertex = graph.get(startCoord);
        final GraphVertex endVertex = graph.get(endCoord);

        if (startVertex == null || endVertex == null) {
            System.err.println("Vertices for Transport '" + transport.title + "' from " + transport.start + " to " + transport.end + " are not in graph.");
            return;
        }

        final Optional<GraphEdge> existingEdge = startVertex.neighbors.stream().filter(e -> e.to().coordinate.equals(endCoord)).findAny();
        if (existingEdge.isPresent()) {
            System.err.println("Duplicate edge " + startCoord + "->" + endCoord + ": "
                    + "Existing edge: '" + existingEdge.get().methodOfMovement() + "' cost " + existingEdge.get().cost() + "; "
                    + "Ignoring new edge: '" + transport.title + "' cost " + transport.duration + ".");
            return;
        }
        startVertex.addEdgeTo(endVertex, transport.duration, transport.title);
    }

    private boolean canMoveNorth(Coordinate coordinate, Map<Coordinate, TileObstacles> tileMap) {
        final TileObstacles northTileObstacles = tileMap.get(coordinate.moveNorth());
        final TileObstacles originTileObstacles = tileMap.get(coordinate);
        return northTileObstacles != null && !originTileObstacles.northBlocked && !northTileObstacles.southBlocked;
    }

    private boolean canMoveEast(Coordinate coordinate, Map<Coordinate, TileObstacles> tileMap) {
        final TileObstacles eastTileObstacles = tileMap.get(coordinate.moveEast());
        final TileObstacles originTileObstacles = tileMap.get(coordinate);
        return eastTileObstacles != null && !originTileObstacles.eastBlocked && !eastTileObstacles.westBlocked;
    }

    private boolean canMoveSouth(Coordinate coordinate, Map<Coordinate, TileObstacles> tileMap) {
        final TileObstacles southTileObstacles = tileMap.get(coordinate.moveSouth());
        final TileObstacles originTileObstacles = tileMap.get(coordinate);
        return southTileObstacles != null && !originTileObstacles.southBlocked && !southTileObstacles.northBlocked;
    }

    private boolean canMoveWest(Coordinate coordinate, Map<Coordinate, TileObstacles> tileMap) {
        final TileObstacles westTileObstacles = tileMap.get(coordinate.moveWest());
        final TileObstacles originTileObstacles = tileMap.get(coordinate);
        return westTileObstacles != null && !originTileObstacles.westBlocked && !westTileObstacles.eastBlocked;
    }

    private boolean canMoveNorthEast(Coordinate coordinate, Map<Coordinate, TileObstacles> tileMap) {
        return this.canMoveEast(coordinate, tileMap) &&
                this.canMoveNorth(coordinate.moveEast(), tileMap) &&
                this.canMoveNorth(coordinate, tileMap) &&
                this.canMoveEast(coordinate.moveNorth(), tileMap);
    }

    private boolean canMoveSouthEast(Coordinate coordinate, Map<Coordinate, TileObstacles> tileMap) {
        return this.canMoveEast(coordinate, tileMap) &&
                this.canMoveSouth(coordinate.moveEast(), tileMap) &&
                this.canMoveSouth(coordinate, tileMap) &&
                this.canMoveEast(coordinate.moveSouth(), tileMap);
    }

    private boolean canMoveSouthWest(Coordinate coordinate, Map<Coordinate, TileObstacles> tileMap) {
        return this.canMoveWest(coordinate, tileMap) &&
                this.canMoveSouth(coordinate.moveWest(), tileMap) &&
                this.canMoveSouth(coordinate, tileMap) &&
                this.canMoveWest(coordinate.moveSouth(), tileMap);
    }

    private boolean canMoveNorthWest(Coordinate coordinate, Map<Coordinate, TileObstacles> tileMap) {
        return this.canMoveWest(coordinate, tileMap) &&
                this.canMoveNorth(coordinate.moveWest(), tileMap) &&
                this.canMoveNorth(coordinate, tileMap) &&
                this.canMoveWest(coordinate.moveNorth(), tileMap);
    }

    /**
     * Processes the rather stupidly made json format into a single data structure containing all Tile coordinates and Obstacles
     *
     * @param movementJson The populated Object representing movement.json
     * @return A map where each coordinate has its own obstacle Data
     */
    private Map<Coordinate, TileObstacles> movementDataToMapOfTiles(MovementJson movementJson) {
        HashMap<Coordinate, TileObstacles> tiles = new HashMap<>();

        // Parse walkable json tiles
        for (CoordinateJson tileJson : movementJson.walkable) {
            final Coordinate coordinate = new Coordinate(tileJson.x, tileJson.y, tileJson.z);
            final TileObstacles tile = new TileObstacles();
            tiles.put(coordinate, tile);
        }

        // Parse obstacles
        // obstacleValues[i] tells if there are obstacles on the tile with the relevant coordinate at obstaclePositions[i]
        assert (movementJson.obstaclePositions.length == movementJson.obstacleValues.length);
        for (int i = 0; i < movementJson.obstaclePositions.length; i++) {
            final CoordinateJson obstacleCoordinateJson = movementJson.obstaclePositions[i];
            final Coordinate obstacleCoordinate = new Coordinate(obstacleCoordinateJson.x, obstacleCoordinateJson.y, obstacleCoordinateJson.z);
            final TileObstacles tile = tiles.get(obstacleCoordinate);

            if (tile != null) {
                final byte obstacleValueJson = movementJson.obstacleValues[i];
                assert (obstacleValueJson >= 0);
                assert (obstacleValueJson <= 15);
                tile.northBlocked = (obstacleValueJson & 1) == 1;
                tile.eastBlocked = (obstacleValueJson & 2) == 2;
                tile.southBlocked = (obstacleValueJson & 4) == 4;
                tile.westBlocked = (obstacleValueJson & 8) == 8;
            }
        }

        return tiles;
    }

    /**
     * This class is only used to build the graph
     */
    private static class TileObstacles {
        public boolean northBlocked;
        public boolean eastBlocked;
        public boolean southBlocked;
        public boolean westBlocked;
    }
}
