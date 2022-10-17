package myImplementation;

import myImplementation.jsonClasses.movement.MovementJson;
import myImplementation.jsonClasses.movement.PointJson;
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
        final Map<Point, GraphBuilder.TileObstacles> tiles = this.movementDataToMapOfTiles(movementJson);
        final long endTimeTileMap = System.currentTimeMillis();
        System.out.println((endTimeTileMap - startTimeTileMap) + "ms");

        // Build walkable graph
        System.out.print("Graph Builder: building walkable nodes: ");
        final long startTimeBuildGraph = System.currentTimeMillis();
        final Map<Point, GraphNode> graphNodes = this.mapOfTilesToWalkableGraph(tiles);
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
        this.addTransports(graphNodes, transportsJson);
        final long endTimeAddTransports = System.currentTimeMillis();
        System.out.println((endTimeAddTransports - startTimeAddTransports) + "ms");

        // find starters
        System.out.print("Graph Builder: finding starters: ");
        final long startTimeFindStarters = System.currentTimeMillis();
        Set<Graph.Starter> starters = this.findStarters(transportsJson);
        final long endTimeFindStarters = System.currentTimeMillis();
        System.out.println((endTimeFindStarters - startTimeFindStarters) + "ms");

        final Graph graph = new Graph(graphNodes, starters);
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
            final Point starterCoordinate = new Point(transport.end.x, transport.end.y);
            starters.add(new Graph.Starter(starterCoordinate, transport.title));
        }
        return starters;
    }

    /**
     * links nodes according to point-to-point transports (fairy rings etc.).
     * Does NOT include teleports!
     * @param graph The nodes of which just walkable ones have been linked
     * @param transports The deserialized transport data
     */
    private void addTransports(Map<Point, GraphNode> graph, TransportJson[] transports) {
        for(TransportJson transport : transports) {
            if(transport.start == null) {
                // This transport is a teleport, skip it
                continue;
            }

            if(transport.start.z != 0 || transport.end.z != 0) {
                // This transport is out of bounds, skip it
                continue;
            }

            final GraphNode start = graph.get(new Point(transport.start.x, transport.start.y));
            final GraphNode end = graph.get(new Point(transport.end.x, transport.end.y));
            if(start != null && end != null) {
                start.linkTo(end, transport.title);
            }
        }
    }

    /**
     * @param tileMap A Map of Tiles
     * @return A Graph linking all walkable tiles together
     */
    private Map<Point, GraphNode> mapOfTilesToWalkableGraph(Map<Point, TileObstacles> tileMap) {
        Map<Point, GraphNode> graph = new HashMap<>();

        for (Map.Entry<Point, TileObstacles> tile : tileMap.entrySet()) {
            final Point point = tile.getKey();

            final GraphNode node = new GraphNode(point);
            graph.put(point, node);

            // Check if there are neighbouring nodes, if so link them if traversable
            // North
            final GraphNode northNode = graph.get(point.moveNorth());
            if (northNode != null && this.canMoveNorth(point, tileMap)) {
                node.linkTo(northNode, "walk north");
                northNode.linkTo(node, "walk south");
            }

            // East
            final GraphNode eastNode = graph.get(point.moveEast());
            if (eastNode != null && this.canMoveEast(point, tileMap)) {
                node.linkTo(eastNode, "walk east");
                eastNode.linkTo(node, "walk west");
            }

            // South
            final GraphNode southNode = graph.get(point.moveSouth());
            if (southNode != null && this.canMoveSouth(point, tileMap)) {
                node.linkTo(southNode, "walk south");
                southNode.linkTo(node, "walk north");
            }

            // West
            final GraphNode westNode = graph.get(point.moveWest());
            if (westNode != null && this.canMoveWest(point, tileMap)) {
                node.linkTo(westNode, "walk west");
                westNode.linkTo(node, "walk east");
            }

            // North East
            final GraphNode northEastNode = graph.get(point.moveNorth().moveEast());
            if (northEastNode != null && this.canMoveNorthEast(point, tileMap)) {
                node.linkTo(northEastNode, "walk north east");
                northEastNode.linkTo(node, "walk south west");
            }

            // South East
            final GraphNode southEastNode = graph.get(point.moveSouth().moveEast());
            if (southEastNode != null && this.canMoveSouthEast(point, tileMap)) {
                node.linkTo(southEastNode, "walk south east");
                southEastNode.linkTo(node, "walk north west");
            }

            // South West
            final GraphNode southWestNode = graph.get(point.moveSouth().moveWest());
            if (southWestNode != null && this.canMoveSouthWest(point, tileMap)) {
                node.linkTo(southWestNode, "walk south west");
                southWestNode.linkTo(node, "walk north east");
            }

            // North West
            final GraphNode northWestNode = graph.get(point.moveNorth().moveWest());
            if (northWestNode != null && this.canMoveNorthWest(point, tileMap)) {
                node.linkTo(northWestNode, "walk north west");
                northWestNode.linkTo(node, "walk south east");
            }
        }

        return graph;
    }

    private boolean canMoveNorth(Point point, Map<Point, TileObstacles> tileMap) {
        final TileObstacles northTileObstacles = tileMap.get(point.moveNorth());
        final TileObstacles originTileObstacles = tileMap.get(point);
        return northTileObstacles != null && !originTileObstacles.northBlocked && !northTileObstacles.southBlocked;
    }

    private boolean canMoveEast(Point point, Map<Point, TileObstacles> tileMap) {
        final TileObstacles eastTileObstacles = tileMap.get(point.moveEast());
        final TileObstacles originTileObstacles = tileMap.get(point);
        return eastTileObstacles != null && !originTileObstacles.eastBlocked && !eastTileObstacles.westBlocked;
    }

    private boolean canMoveSouth(Point point, Map<Point, TileObstacles> tileMap) {
        final TileObstacles southTileObstacles = tileMap.get(point.moveSouth());
        final TileObstacles originTileObstacles = tileMap.get(point);
        return southTileObstacles != null && !originTileObstacles.southBlocked && !southTileObstacles.northBlocked;
    }

    private boolean canMoveWest(Point point, Map<Point, TileObstacles> tileMap) {
        final TileObstacles westTileObstacles = tileMap.get(point.moveWest());
        final TileObstacles originTileObstacles = tileMap.get(point);
        return westTileObstacles != null && !originTileObstacles.westBlocked && !westTileObstacles.eastBlocked;
    }

    private boolean canMoveNorthEast(Point point, Map<Point, TileObstacles> tileMap) {
        return this.canMoveEast(point, tileMap) &&
                this.canMoveNorth(point.moveEast(), tileMap) &&
                this.canMoveNorth(point, tileMap) &&
                this.canMoveEast(point.moveNorth(), tileMap);
    }

    private boolean canMoveSouthEast(Point point, Map<Point, TileObstacles> tileMap) {
        return this.canMoveEast(point, tileMap) &&
                this.canMoveSouth(point.moveEast(), tileMap) &&
                this.canMoveSouth(point, tileMap) &&
                this.canMoveEast(point.moveSouth(), tileMap);
    }

    private boolean canMoveSouthWest(Point point, Map<Point, TileObstacles> tileMap) {
        return this.canMoveWest(point, tileMap) &&
                this.canMoveSouth(point.moveWest(), tileMap) &&
                this.canMoveSouth(point, tileMap) &&
                this.canMoveWest(point.moveSouth(), tileMap);
    }

    private boolean canMoveNorthWest(Point point, Map<Point, TileObstacles> tileMap) {
        return this.canMoveWest(point, tileMap) &&
                this.canMoveNorth(point.moveWest(), tileMap) &&
                this.canMoveNorth(point, tileMap) &&
                this.canMoveWest(point.moveNorth(), tileMap);
    }

    /**
     * Processes the rather stupidly made json format into a single data structure containing all Tile coordinates and Obstacles
     * @param movementJson The populated Object representing movement.json
     * @return A map where each coordinate has its own obstacle Data
     */
    private Map<Point, TileObstacles> movementDataToMapOfTiles(MovementJson movementJson) {
        HashMap<Point, TileObstacles> tiles = new HashMap<>();

        // Parse walkable json tiles
        for (PointJson tileJson : movementJson.walkable) {
            final Point coordinate = new Point(tileJson.x, tileJson.y);
            final TileObstacles tile = new TileObstacles();
            tiles.put(coordinate, tile);
        }

        // Parse obstacles
        // obstacleValues[i] tells if there are obstacles on the tile with the relevant coordinate at obstaclePositions[i]
        assert (movementJson.obstaclePositions.length == movementJson.obstacleValues.length);
        for (int i = 0; i < movementJson.obstaclePositions.length; i++) {
            final PointJson obstacleCoordinateJson = movementJson.obstaclePositions[i];
            final Point obstacleCoordinate = new Point(obstacleCoordinateJson.x, obstacleCoordinateJson.y);
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
