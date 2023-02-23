package wiki.runescape.oldschool.pathfinder.data_deserialization;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.DirectionalBlockers;
import wiki.runescape.oldschool.pathfinder.logic.Teleport;
import wiki.runescape.oldschool.pathfinder.logic.Transport;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DataDeserializer {
    private final static String MAP_DATA_ARCHIVE_FILE = "mapdata.zip";
    private final static String ARCHIVE_ENTRY_MOVEMENT_DATA = "movement.csv";
    private final static String ARCHIVE_ENTRY_TELEPORTS = "teleports.csv";
    private final static String ARCHIVE_ENTRY_TRANSPORTS = "transports.csv";


    public MapData deserializeMapData() throws IOException {
        try (final InputStream mapDataInStream = DataDeserializer.class.getClassLoader().getResourceAsStream(MAP_DATA_ARCHIVE_FILE)) {
            if (mapDataInStream == null) {
                throw new FileNotFoundException(MAP_DATA_ARCHIVE_FILE);
            }
            final ZipInputStream zipIn = new ZipInputStream(mapDataInStream);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(zipIn));

            Map<Coordinate, DirectionalBlockers> walkableTiles = null;
            Collection<Teleport> teleports = null;
            Collection<Transport> transports = null;

            ZipEntry currentEntry = zipIn.getNextEntry();
            while (currentEntry != null) {
                switch (currentEntry.getName()) {
                    case ARCHIVE_ENTRY_MOVEMENT_DATA -> walkableTiles = this.readMovementData(reader);
                    case ARCHIVE_ENTRY_TELEPORTS -> teleports = this.readTeleports(reader);
                    case ARCHIVE_ENTRY_TRANSPORTS -> transports = this.readTransports(reader);
                    default -> throw new IOException("Map data archive " + MAP_DATA_ARCHIVE_FILE
                            + " contains unrecognized file: " + currentEntry.getName());
                }
                zipIn.closeEntry();
                currentEntry = zipIn.getNextEntry();
            }
            if (walkableTiles == null || teleports == null || transports == null) {
                throw new IOException("Map data archive " + MAP_DATA_ARCHIVE_FILE + " is missing some of the required entires: "
                        + ARCHIVE_ENTRY_MOVEMENT_DATA + "," + ARCHIVE_ENTRY_TELEPORTS + "," + ARCHIVE_ENTRY_TRANSPORTS);
            }

            return new MapData(walkableTiles, teleports, transports);
        }
        catch (Exception e) {
            throw new IOException("An error occurred reading the map data " + MAP_DATA_ARCHIVE_FILE, e);
        }
    }

    private Map<Coordinate, DirectionalBlockers> readMovementData(BufferedReader movementDataStream) {
        final HashMap<Coordinate, DirectionalBlockers> tiles = new HashMap<>();
        movementDataStream.lines()
                .filter(line -> !line.startsWith("#"))    // Filter comments
                .map(line -> line.split(",")) // Separate comma values
                .forEachOrdered(tileData -> {
                    assert (tileData.length == 7);
                    final Coordinate coordinate = new Coordinate(
                            Integer.parseInt(tileData[0]),
                            Integer.parseInt(tileData[1]),
                            Integer.parseInt(tileData[2]));
                    final DirectionalBlockers obstacles = new DirectionalBlockers(
                            Boolean.parseBoolean(tileData[3]),
                            Boolean.parseBoolean(tileData[4]),
                            Boolean.parseBoolean(tileData[5]),
                            Boolean.parseBoolean(tileData[6])
                    );
                    tiles.put(coordinate, obstacles);
                });
        return tiles;
    }

    private Collection<Transport> readTransports(BufferedReader transportDataStream) {
        return transportDataStream.lines()
                .filter(line -> !(line.startsWith("#") || line.isEmpty()))
                .map(line -> {
                    final String[] parts = line.split(",");
                    assert parts.length == 8;
                    final Coordinate from = new Coordinate(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]));
                    final Coordinate to = new Coordinate(
                            Integer.parseInt(parts[3]),
                            Integer.parseInt(parts[4]),
                            Integer.parseInt(parts[5]));
                    return new Transport(
                            from,
                            to,
                            parts[7],
                            Byte.parseByte(parts[6]));
                })
                .collect(Collectors.toList());
    }

    private Collection<Teleport> readTeleports(BufferedReader teleportDataStream) {
        return teleportDataStream.lines()
                .filter(line -> !(line.startsWith("#") || line.isEmpty()))
                .map(line -> {
                    final String[] parts = line.split(",");
                    assert parts.length == 5;
                    final Coordinate to = new Coordinate(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]));
                    return new Teleport(
                            to,
                            parts[4],
                            Byte.parseByte(parts[3]));
                })
                .collect(Collectors.toList());
    }

    public record MapData(
            Map<Coordinate, DirectionalBlockers> walkableTiles,
            Collection<Teleport> teleports,
            Collection<Transport> transports) {
    }
}


