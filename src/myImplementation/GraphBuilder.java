package myImplementation;

import myImplementation.jsonClasses.CoordinateJson;
import myImplementation.jsonClasses.MovementJson;
import myImplementation.jsonClasses.TransportJson;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphBuilder {

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
        final Map<Coordinate, GraphBuilder.TileObstacles> tiles = this.movementDataToMapOfTiles(movementJson);
        log.lap("build tile map");

        // Build walkable graph
        final Map<Coordinate, GraphVertex> graphVertices = this.mapOfTilesToWalkableGraph(tiles);
        log.lap("link walkable vertices");

        // Deserialize Transport Data
        final TransportJson[] transportsJson = new DataDeserializer().deserializeTransportData();
        log.lap("deserialize transport data");

        // Add Transports to Graph
        this.addTransports(graphVertices, transportsJson);
        log.lap("link vertices by transports");

        // find teleports
        final Set<Teleport> teleports = this.findTeleports(transportsJson);
        log.lap("find teleports");

        // Deserialize Skretzo Data
        final TransportJson[] skretzoTransports = new DataDeserializer().deserializeSkretzoData();
        log.lap("deserialize skretzo data");

        // Add skretzo transports
        this.addTransports(graphVertices, skretzoTransports);
        log.lap("link vertices by skretzo transports");

        log.end();
        return new Graph(graphVertices, teleports);
    }

    /**
     * Finds teleports in transport data
     *
     * @param transportsJson The deserialized transport data
     * @return All teleports
     */
    private Set<Teleport> findTeleports(TransportJson[] transportsJson) {
        Set<Teleport> teleports = new HashSet<>();
        for (TransportJson transport : transportsJson) {
            if (transport.start != null) {
                // This is a point-to-point transport, not a teleport
                continue;
            }
            if (transport.end.z != 0) {
                // This teleport is out of bounds, skip it
                continue;
            }
            final Coordinate teleportCoordinate = new Coordinate(transport.end.x, transport.end.y);
            teleports.add(new Teleport(teleportCoordinate, transport.title, transport.duration));
        }
        return teleports;
    }

    /**
     * links vertices according to point-to-point transports (fairy rings etc.).
     * Does NOT include teleports!
     *
     * @param graph      The vertices of which just walkable ones have been linked
     * @param transports The deserialized transport data
     */
    private void addTransports(Map<Coordinate, GraphVertex> graph, TransportJson[] transports) {
        for (TransportJson transport : transports) {
            if (transport.start == null) {
                // This transport is a teleport, not a transport
                continue;
            }

            if (transport.start.z != 0 || transport.end.z != 0) {
                // This transport is out of bounds, skip
                continue;
            }

            final GraphVertex start = graph.get(new Coordinate(transport.start.x, transport.start.y));
            final GraphVertex end = graph.get(new Coordinate(transport.end.x, transport.end.y));

            if (start == null || end == null) {
                // This transport starts from or leads to nowhere
                // System.out.println("Vertex for Transport " + transport.toString() + " doesn't exist.");
                continue;
            }

            final boolean duplicateEdgeFound = start.neighbors.stream().anyMatch(e -> e.to().coordinate.equals(end.coordinate));
            if(duplicateEdgeFound) {
                // System.out.println("Edge from Vertex " + start + " to Vertex " + end + " already exists. Duplicate Transport in data?");
                continue;
            }

            start.addEdgeTo(end, transport.duration, transport.title);
        }
    }

    /**
     * @param tileMap A Map of Tiles
     * @return A Graph linking all walkable tiles together
     */
    private Map<Coordinate, GraphVertex> mapOfTilesToWalkableGraph(Map<Coordinate, TileObstacles> tileMap) {
        Map<Coordinate, GraphVertex> graph = new HashMap<>();

        for (Map.Entry<Coordinate, TileObstacles> tile : tileMap.entrySet()) {
            final Coordinate coordinate = tile.getKey();

            final GraphVertex vertex = new GraphVertex(coordinate);
            graph.put(coordinate, vertex);

            // Check if there are neighbouring vertices, if so link them if traversable
            // North
            final GraphVertex northVertex = graph.get(coordinate.moveNorth());
            if (northVertex != null && this.canMoveNorth(coordinate, tileMap)) {
                vertex.addEdgeTo(northVertex, (byte) 1, "walk north");
                northVertex.addEdgeTo(vertex, (byte) 1, "walk south");
            }

            // East
            final GraphVertex eastVertex = graph.get(coordinate.moveEast());
            if (eastVertex != null && this.canMoveEast(coordinate, tileMap)) {
                vertex.addEdgeTo(eastVertex, (byte) 1, "walk east");
                eastVertex.addEdgeTo(vertex, (byte) 1, "walk west");
            }

            // South
            final GraphVertex southVertex = graph.get(coordinate.moveSouth());
            if (southVertex != null && this.canMoveSouth(coordinate, tileMap)) {
                vertex.addEdgeTo(southVertex, (byte) 1, "walk south");
                southVertex.addEdgeTo(vertex, (byte) 1, "walk north");
            }

            // West
            final GraphVertex westVertex = graph.get(coordinate.moveWest());
            if (westVertex != null && this.canMoveWest(coordinate, tileMap)) {
                vertex.addEdgeTo(westVertex, (byte) 1, "walk west");
                westVertex.addEdgeTo(vertex, (byte) 1, "walk east");
            }

            // North East
            final GraphVertex northEastVertex = graph.get(coordinate.moveNorth().moveEast());
            if (northEastVertex != null && this.canMoveNorthEast(coordinate, tileMap)) {
                vertex.addEdgeTo(northEastVertex, (byte) 1, "walk north east");
                northEastVertex.addEdgeTo(vertex, (byte) 1, "walk south west");
            }

            // South East
            final GraphVertex southEastVertex = graph.get(coordinate.moveSouth().moveEast());
            if (southEastVertex != null && this.canMoveSouthEast(coordinate, tileMap)) {
                vertex.addEdgeTo(southEastVertex, (byte) 1, "walk south east");
                southEastVertex.addEdgeTo(vertex, (byte) 1, "walk north west");
            }

            // South West
            final GraphVertex southWestVertex = graph.get(coordinate.moveSouth().moveWest());
            if (southWestVertex != null && this.canMoveSouthWest(coordinate, tileMap)) {
                vertex.addEdgeTo(southWestVertex, (byte) 1, "walk south west");
                southWestVertex.addEdgeTo(vertex, (byte) 1, "walk north east");
            }

            // North West
            final GraphVertex northWestVertex = graph.get(coordinate.moveNorth().moveWest());
            if (northWestVertex != null && this.canMoveNorthWest(coordinate, tileMap)) {
                vertex.addEdgeTo(northWestVertex, (byte) 1, "walk north west");
                northWestVertex.addEdgeTo(vertex, (byte) 1, "walk south east");
            }
        }

        return graph;
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
            final Coordinate coordinate = new Coordinate(tileJson.x, tileJson.y);
            final TileObstacles tile = new TileObstacles();
            tiles.put(coordinate, tile);
        }

        // Parse obstacles
        // obstacleValues[i] tells if there are obstacles on the tile with the relevant coordinate at obstaclePositions[i]
        assert (movementJson.obstaclePositions.length == movementJson.obstacleValues.length);
        for (int i = 0; i < movementJson.obstaclePositions.length; i++) {
            final CoordinateJson obstacleCoordinateJson = movementJson.obstaclePositions[i];
            final Coordinate obstacleCoordinate = new Coordinate(obstacleCoordinateJson.x, obstacleCoordinateJson.y);
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
