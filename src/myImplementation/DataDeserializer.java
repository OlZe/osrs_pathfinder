package myImplementation;

import com.google.gson.Gson;
import myImplementation.jsonClasses.MovementJson;
import myImplementation.jsonClasses.TransportJson;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataDeserializer {
    private final static String MOVEMENT_FILE_PATH = "movement.json";
    private final static String TRANSPORTS_FILE_PATH = "transports.json";

    /**
     * Reads the file "movement.json"
     * @return The content of "movement.json" in an Object
     * @throws IOException can't read file
     */
    public MovementJson deserializeMovementData() throws IOException {
        BufferedReader jsonFile = Files.newBufferedReader(Path.of(MOVEMENT_FILE_PATH), StandardCharsets.UTF_8);
        return new Gson().fromJson(jsonFile, MovementJson.class);
    }

    /**
     * Reads the file "transports.json"
     * @return The content of "transports.json" in an Object
     * @throws IOException can't read file
     */
    public TransportJson[] deserializeTransportData() throws IOException {
        BufferedReader jsonFile = Files.newBufferedReader(Path.of(TRANSPORTS_FILE_PATH), StandardCharsets.UTF_8);
        return new Gson().fromJson(jsonFile, TransportJson[].class);
    }




}
