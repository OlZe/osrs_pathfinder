package wiki.runescape.oldschool;

import com.google.gson.Gson;
import wiki.runescape.oldschool.jsonClasses.CoordinateJson;
import wiki.runescape.oldschool.jsonClasses.MovementJson;
import wiki.runescape.oldschool.jsonClasses.TransportJson;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class DataDeserializer {
    private final static String MOVEMENT_FILE_PATH = "src/main/resources/movement.json";
    private final static String TRANSPORTS_FILE_PATH = "src/main/resources/transports.json";
    private final static String SKRETZO_FILE_PATH = "src/main/resources/skretzo_data.txt";

    /**
     * Reads the file "movement.json"
     *
     * @return The content of "movement.json" in an Object
     * @throws IOException can't read file
     */
    public MovementJson deserializeMovementData() throws IOException {
        try (BufferedReader jsonFile = Files.newBufferedReader(Path.of(MOVEMENT_FILE_PATH), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(jsonFile, MovementJson.class);
        }
    }

    /**
     * Reads the file "transports.json"
     *
     * @return The content of "transports.json" in an Object
     * @throws IOException can't read file
     */
    public TransportJson[] deserializeTransportData() throws IOException {
        try (BufferedReader jsonFile = Files.newBufferedReader(Path.of(TRANSPORTS_FILE_PATH), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(jsonFile, TransportJson[].class);
        }
    }

    /**
     * Reads skretzo's data file and parses them into TransportJson[]
     *
     * @return The content of the file in an object. WARNING transportJson[i].duration is ALWAYS 1
     */
    public TransportJson[] deserializeSkretzoData() throws IOException {
        try (BufferedReader file = Files.newBufferedReader(Path.of(SKRETZO_FILE_PATH), StandardCharsets.UTF_8)) {
            // Filter comments
            final Stream<String> lines = file.lines().filter(line -> !(line.startsWith("#") || line.isEmpty()));
            final Stream<TransportJson> transports = lines.map(line -> {
                final String[] parts = line.split("\t");
                final String[] parts_startCoordinate = parts[0].split(" ");
                final String[] parts_endCoordinate = parts[1].split(" ");
                final String methodOfMovement = parts[2];
                final CoordinateJson startCoordinate = new CoordinateJson();
                startCoordinate.x = Integer.parseInt(parts_startCoordinate[0]);
                startCoordinate.y = Integer.parseInt(parts_startCoordinate[1]);
                startCoordinate.z = Integer.parseInt(parts_startCoordinate[2]);
                final CoordinateJson endCoordinate = new CoordinateJson();
                endCoordinate.x = Integer.parseInt(parts_endCoordinate[0]);
                endCoordinate.y = Integer.parseInt(parts_endCoordinate[1]);
                endCoordinate.z = Integer.parseInt(parts_endCoordinate[2]);
                final TransportJson transport = new TransportJson();
                transport.start = startCoordinate;
                transport.end = endCoordinate;
                transport.title = methodOfMovement;
                transport.duration = (byte) 1; // TODO Skretzo's data does not include duration
                return transport;
            });
            return transports.toArray(TransportJson[]::new);
        }
    }


}
