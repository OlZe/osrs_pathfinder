package myImplementation;

import myImplementation.jsonClasses.movement.MovementJson;
import myImplementation.jsonClasses.movement.CoordinateJson;
import myImplementation.jsonClasses.movement.TransportJson;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphBuilder {

    /**
     * Reads the json file and builds a graph structure
     *
     * @return A HashMap where each coordinate is mapped to its direct neighbours through a linked list
     * @throws IOException can't read file
     */
    public Graph buildGraph() throws IOException {
        // Deserialize Json
        System.out.print("Graph Builder: deserializing movement data: ");
        final long startTimeDeserializeMovement = System.currentTimeMillis();
        final MovementJson movementJson = new DataDeserializer().deserializeMovementData();
        final long endTimeDeserializeMovement = System.currentTimeMillis();
        System.out.println((endTimeDeserializeMovement - startTimeDeserializeMovement) + "ms");

        // Build Tile Map
        System.out.print("Graph Builder: building tile map: ");
        final long startTimeTileMap = System.currentTimeMillis();
        final Map<Coordinate, GraphBuilder.TileObstacles> tiles = this.movementDataToMapOfTiles(movementJson);
        final long endTimeTileMap = System.currentTimeMillis();
        System.out.println((endTimeTileMap - startTimeTileMap) + "ms");

        // Build walkable graph
        System.out.print("Graph Builder: building walkable vertices: ");
        final long startTimeBuildGraph = System.currentTimeMillis();
        final Map<Coordinate, GraphVertex> graphVertices = this.mapOfTilesToWalkableGraph(tiles);
        final long endTimeBuildGraph = System.currentTimeMillis();
        System.out.println((endTimeBuildGraph - startTimeBuildGraph) + "ms");

        // Deserialize Transport Data
        System.out.print("Graph Builder: deserializing transports data: ");
        final long startTimeDeserializeTransports = System.currentTimeMillis();
        final TransportJson[] transportsJson = new DataDeserializer().deserializeTransportData();
        final long endTimeDeserializeTransports = System.currentTimeMillis();
        System.out.println((endTimeDeserializeTransports - startTimeDeserializeTransports) + "ms");

        // Add Transports to Graph
        System.out.print("Graph Builder: adding transports: ");
        final long startTimeAddTransports = System.currentTimeMillis();
        this.addTransports(graphVertices, transportsJson);
        final long endTimeAddTransports = System.currentTimeMillis();
        System.out.println((endTimeAddTransports - startTimeAddTransports) + "ms");

        // find starters
        System.out.print("Graph Builder: finding starters: ");
        final long startTimeFindStarters = System.currentTimeMillis();
        Set<Graph.Starter> starters = this.findStarters(transportsJson);
        final long endTimeFindStarters = System.currentTimeMillis();
        System.out.println((endTimeFindStarters - startTimeFindStarters) + "ms");

        final Graph graph = new Graph(graphVertices, starters);
        System.out.println("Graph Builder: total: " + (System.currentTimeMillis() - startTimeDeserializeMovement) + "ms");
        return graph;
    }

    /**
     * Finds starters in transport data
     * @param transportsJson The deserialized transport data
     * @return All starters
     */
    private Set<Graph.Starter> findStarters(TransportJson[] transportsJson) {
        Set<Graph.Starter> starters = new HashSet<>();
        for(TransportJson transport : transportsJson) {
            if(transport.start != null) {
                // This is a point-to-point transport, not a starter
                continue;
            }
            if(transport.end.z != 0) {
                // This starter is out of bounds, skip it
                continue;
            }
            final Coordinate starterCoordinate = new Coordinate(transport.end.x, transport.end.y);
            starters.add(new Graph.Starter(starterCoordinate, transport.title));
        }
        return starters;
    }

    /**
     * links vertices according to point-to-point transports (fairy rings etc.).
     * Does NOT include teleports!
     * @param graph The vertices of which just walkable ones have been linked
     * @param transports The deserialized transport data
     */
    private void addTransports(Map<Coordinate, GraphVertex> graph, TransportJson[] transports) {
        for(TransportJson transport : transports) {
            if(transport.start == null) {
                // This transport is a teleport, skip it
                continue;
            }

            if(transport.start.z != 0 || transport.end.z != 0) {
                // This transport is out of bounds, skip it
                continue;
            }

            final GraphVertex start = graph.get(new Coordinate(transport.start.x, transport.start.y));
            final GraphVertex end = graph.get(new Coordinate(transport.end.x, transport.end.y));
            if(start != null && end != null) {
                start.addEdgeTo(end, transport.title);
            }
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
                vertex.addEdgeTo(northVertex, "walk north");
                northVertex.addEdgeTo(vertex, "walk south");
            }

            // East
            final GraphVertex eastVertex = graph.get(coordinate.moveEast());
            if (eastVertex != null && this.canMoveEast(coordinate, tileMap)) {
                vertex.addEdgeTo(eastVertex, "walk east");
                eastVertex.addEdgeTo(vertex, "walk west");
            }

            // South
            final GraphVertex southVertex = graph.get(coordinate.moveSouth());
            if (southVertex != null && this.canMoveSouth(coordinate, tileMap)) {
                vertex.addEdgeTo(southVertex, "walk south");
                southVertex.addEdgeTo(vertex, "walk north");
            }

            // West
            final GraphVertex westVertex = graph.get(coordinate.moveWest());
            if (westVertex != null && this.canMoveWest(coordinate, tileMap)) {
                vertex.addEdgeTo(westVertex, "walk west");
                westVertex.addEdgeTo(vertex, "walk east");
            }

            // North East
            final GraphVertex northEastVertex = graph.get(coordinate.moveNorth().moveEast());
            if (northEastVertex != null && this.canMoveNorthEast(coordinate, tileMap)) {
                vertex.addEdgeTo(northEastVertex, "walk north east");
                northEastVertex.addEdgeTo(vertex, "walk south west");
            }

            // South East
            final GraphVertex southEastVertex = graph.get(coordinate.moveSouth().moveEast());
            if (southEastVertex != null && this.canMoveSouthEast(coordinate, tileMap)) {
                vertex.addEdgeTo(southEastVertex, "walk south east");
                southEastVertex.addEdgeTo(vertex, "walk north west");
            }

            // South West
            final GraphVertex southWestVertex = graph.get(coordinate.moveSouth().moveWest());
            if (southWestVertex != null && this.canMoveSouthWest(coordinate, tileMap)) {
                vertex.addEdgeTo(southWestVertex, "walk south west");
                southWestVertex.addEdgeTo(vertex, "walk north east");
            }

            // North West
            final GraphVertex northWestVertex = graph.get(coordinate.moveNorth().moveWest());
            if (northWestVertex != null && this.canMoveNorthWest(coordinate, tileMap)) {
                vertex.addEdgeTo(northWestVertex, "walk north west");
                northWestVertex.addEdgeTo(vertex, "walk south east");
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
