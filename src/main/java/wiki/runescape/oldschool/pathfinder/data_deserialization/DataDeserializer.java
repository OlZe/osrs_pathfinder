package wiki.runescape.oldschool.pathfinder.data_deserialization;

import com.google.gson.Gson;
import wiki.runescape.oldschool.pathfinder.data_deserialization.jsonClasses.CoordinateJson;
import wiki.runescape.oldschool.pathfinder.data_deserialization.jsonClasses.TransportJson;
import wiki.runescape.oldschool.pathfinder.logic.Coordinate;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DataDeserializer {
    private final static String MOVEMENT_ZIP_FILE_PATH = "movement.csv.zip";
    private final static String MOVEMENT_ZIP_ENTRY = "movement.csv";
    private final static String TRANSPORTS_FILE_PATH = "transports.json";
    private final static String SKRETZO_FILE_PATH = "skretzo_data.txt";

    /**
     * Reads the file "movement.csv.zip"
     *
     * @return The content of "movement.csv" in an Object
     * @throws IOException can't read file
     */
    public Map<Coordinate, TileObstacles> deserializeMovementData() throws IOException {
        final InputStream resource = DataDeserializer.class.getClassLoader().getResourceAsStream(MOVEMENT_ZIP_FILE_PATH);
        if (resource == null) {
            throw new FileNotFoundException(MOVEMENT_ZIP_FILE_PATH);
        }
        final ZipInputStream zipIn = new ZipInputStream(resource);
        final ZipEntry zipFileEntry = zipIn.getNextEntry();

        if(zipFileEntry == null || !zipFileEntry.getName().equals(MOVEMENT_ZIP_ENTRY)) {
            throw new FileNotFoundException("Could not find zip entry " + MOVEMENT_ZIP_ENTRY + " in zip file " + MOVEMENT_ZIP_FILE_PATH);
        }

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(zipIn))) {
            final HashMap<Coordinate, TileObstacles> tiles = new HashMap<>();
            reader.lines()
                    .filter(line -> !line.startsWith("#"))    // Filter comments
                    .map(line -> line.split(",")) // Separate comma values
                    .forEach(tileData -> {
                        assert (tileData.length == 7);
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

    /**
     * Reads the file "transports.json"
     *
     * @return The content of "transports.json" in an Object
     * @throws IOException can't read file
     */
    public TransportJson[] deserializeTransportData() throws IOException {
        final InputStream resource = DataDeserializer.class.getClassLoader().getResourceAsStream(TRANSPORTS_FILE_PATH);
        if(resource == null) {
            throw new FileNotFoundException(TRANSPORTS_FILE_PATH);
        }
        try (BufferedReader jsonFile = new BufferedReader(new InputStreamReader(resource))) {
            return new Gson().fromJson(jsonFile, TransportJson[].class);
        }
    }

    /**
     * Reads skretzo's data file and parses them into TransportJson[]
     *
     * @return The content of the file in an object. WARNING transportJson[i].duration is ALWAYS 1
     */
    public TransportJson[] deserializeSkretzoData() throws IOException {
        final InputStream resource = DataDeserializer.class.getClassLoader().getResourceAsStream(SKRETZO_FILE_PATH);
        if(resource == null) {
            throw new FileNotFoundException(SKRETZO_FILE_PATH);
        }
        try (BufferedReader file = new BufferedReader(new InputStreamReader(resource))) {
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
