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
        final MovementJson movementJson = b.deserializeJsonFile();
        final Map<Point, TileObstacles> tiles = b.jsonDataToMapOfTiles(movementJson);





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
     * @return The content of "movement.json" in an Object
     * @throws IOException
     */
    private MovementJson deserializeJsonFile() throws IOException {
        BufferedReader jsonFile = Files.newBufferedReader(Path.of(MOVEMENT_FILE_PATH), StandardCharsets.UTF_8);
        return new Gson().fromJson(jsonFile, MovementJson.class);
    }

    /**
     * This class is only used to build the graph
     */
    private class TileObstacles {
        public boolean northBlocked;
        public boolean eastBlocked;
        public boolean southBlocked;
        public boolean westBlocked;
    }
}
