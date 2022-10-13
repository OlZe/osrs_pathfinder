package myImplementation;

import com.google.gson.Gson;
import myImplementation.jsonClasses.movement.MovementJson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class WalkableGraphBuilder {
    public static void main(String[] args) throws IOException {

        WalkableGraphBuilder walkableGraphBuilder = new WalkableGraphBuilder();
        MovementJson movementJson = walkableGraphBuilder.deserializeJson();
        System.out.println(movementJson);
        System.out.println(movementJson.walkable.length);
        System.out.println(movementJson.obstaclePositions.length);
        System.out.println(movementJson.obstacleValues.length);
    }

    private MovementJson deserializeJson() throws IOException {
        BufferedReader jsonFile = Files.newBufferedReader(Path.of("movement.json"), StandardCharsets.UTF_8);
        return new Gson().fromJson(jsonFile, MovementJson.class);
    }
}
