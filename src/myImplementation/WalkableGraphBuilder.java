package myImplementation;

import com.google.gson.Gson;
import myImplementation.jsonClasses.movement.MovementJson;
import myImplementation.jsonClasses.movement.PointJson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class WalkableGraphBuilder {
    private final static String MOVEMENT_FILE_PATH = "movement.json";

    public static void main(String[] args) throws IOException {

        final WalkableGraphBuilder b = new WalkableGraphBuilder();


        long startTime = System.currentTimeMillis();
        final MovementJson movementJson = b.deserializeJsonFile();
        System.out.println("Deserializing Json: " + (System.currentTimeMillis() - startTime) + "ms");

        startTime = System.currentTimeMillis();
        final Map<Point, TileObstacles> tiles = b.jsonDataToMapOfTiles(movementJson);
        System.out.println("Building Tile Map: " + (System.currentTimeMillis() - startTime) + "ms");

        startTime = System.currentTimeMillis();
        final Map<Point, GraphNode> graph = b.mapOfTilesToLinkedGraph(tiles);
        System.out.println("Building Linked Graph: " + (System.currentTimeMillis() - startTime) + "ms");

    }

    /**
     * @param tileMap A Map of Tiles
     * @return A linked Graph
     */
    private Map<Point, GraphNode> mapOfTilesToLinkedGraph(Map<Point, TileObstacles> tileMap) {
        Map<Point, GraphNode> graph = new HashMap<>();

        for (Map.Entry<Point, TileObstacles> tile : tileMap.entrySet()) {
            final Point point = tile.getKey();

            final GraphNode node = new GraphNode(point);
            graph.put(point, node);

            // Check if there are neighbors in graph, if so link them if traversable
            // North
            final GraphNode northNode = graph.get(point.moveNorth());
            if (northNode != null && this.canMoveNorth(point, tileMap)) {
                node.linkBidirectional(northNode);
            }

            // East
            final GraphNode eastNode = graph.get(point.moveEast());
            if (eastNode != null && this.canMoveEast(point, tileMap)) {
                node.linkBidirectional(eastNode);
            }

            // South
            final GraphNode southNode = graph.get(point.moveSouth());
            if (southNode != null && this.canMoveSouth(point, tileMap)) {
                node.linkBidirectional(southNode);
            }

            // West
            final GraphNode westNode = graph.get(point.moveWest());
            if (westNode != null && this.canMoveWest(point, tileMap)) {
                node.linkBidirectional(westNode);
            }

            // North East
            final GraphNode northEastNode = graph.get(point.moveNorth().moveEast());
            if (northEastNode != null && this.canMoveNorthEast(point, tileMap)) {
                node.linkBidirectional(northEastNode);
            }

            // South East
            final GraphNode southEastNode = graph.get(point.moveSouth().moveEast());
            if (southEastNode != null && this.canMoveSouthEast(point, tileMap)) {
                node.linkBidirectional(southEastNode);
            }

            // South West
            final GraphNode southWestNode = graph.get(point.moveSouth().moveWest());
            if (southWestNode != null && this.canMoveSouthWest(point, tileMap)) {
                node.linkBidirectional(southWestNode);
            }
            
            // North West
            final GraphNode northWestNode = graph.get(point.moveNorth().moveWest());
            if (northWestNode != null && this.canMoveNorthWest(point, tileMap)) {
                node.linkBidirectional(northWestNode);
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
     *
     * @param movementJson The populated Object representing movement.json
     * @return A map where each coordinate has its own obstacle Data
     */
    private Map<Point, TileObstacles> jsonDataToMapOfTiles(MovementJson movementJson) {
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
     * Reads the file "movement.json"
     *
     * @return The content of "movement.json" in an Object
     * @throws IOException brrr
     */
    private MovementJson deserializeJsonFile() throws IOException {
        BufferedReader jsonFile = Files.newBufferedReader(Path.of(MOVEMENT_FILE_PATH), StandardCharsets.UTF_8);
        return new Gson().fromJson(jsonFile, MovementJson.class);
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
