package wiki.runescape.oldschool.pathfinder.data_deserialization;

import com.google.gson.Gson;
import wiki.runescape.oldschool.pathfinder.data_deserialization.jsonClasses.CoordinateJson;
import wiki.runescape.oldschool.pathfinder.data_deserialization.jsonClasses.TransportJson;
import wiki.runescape.oldschool.pathfinder.logic.Coordinate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DataDeserializer {
    private final static String MOVEMENT_ZIP_FILE_PATH = "src/main/resources/movement.csv.zip";
    private final static String MOVEMENT_ZIP_ENTRY = "movement.csv";
    private final static String TRANSPORTS_FILE_PATH = "src/main/resources/transports.json";
    private final static String SKRETZO_FILE_PATH = "src/main/resources/skretzo_data.txt";

    /**
     * Reads the file "movement.csv.zip"
     *
     * @return The content of "movement.csv" in an Object
     * @throws IOException can't read file
     */
    public Map<Coordinate, TileObstacles> deserializeMovementData() throws IOException {
        try (final ZipFile zipFile = new ZipFile(MOVEMENT_ZIP_FILE_PATH)) {
            final ZipEntry zipFileEntry = zipFile.getEntry(MOVEMENT_ZIP_ENTRY);
            assert (zipFileEntry != null);
            try (final InputStream in = zipFile.getInputStream(zipFileEntry);
                 final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

                final HashMap<Coordinate, TileObstacles> tiles = new HashMap<>();
                reader.lines()
                        .filter(line -> !line.startsWith("#"))    // Filter comments
                        .map(line -> line.split(",")) // Separate comma values
                        .forEach(tileData -> {
                            assert(tileData.length == 7);
                            final Coordinate coordinate = new Coordinate(
                                    Integer.parseInt(tileData[0]),
                                    Integer.parseInt(tileData[1]),
                                    Integer.parseInt(tileData[2]));
                            final TileObstacles obstacles = new TileObstacles();
                            obstacles.northBlocked = Boolean.parseBoolean(tileData[3]);
                            obstacles.eastBlocked = Boolean.parseBoolean(tileData[4]);
                            obstacles.southBlocked = Boolean.parseBoolean(tileData[5]);
                            obstacles.westBlocked = Boolean.parseBoolean(tileData[6]);
                            tiles.put(coordinate, obstacles);
                        });
                return tiles;
            }
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


    public static class TileObstacles {
        public boolean northBlocked;
        public boolean eastBlocked;
        public boolean southBlocked;
        public boolean westBlocked;
    }
}
