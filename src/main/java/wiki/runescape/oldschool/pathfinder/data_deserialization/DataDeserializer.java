package wiki.runescape.oldschool.pathfinder.data_deserialization;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;

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

            Map<Coordinate, PositionInfo> walkableTiles = null;
            Collection<TeleportJson> teleports = null;
            Collection<TransportJson> transports = null;

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
                throw new IOException("Map data archive " + MAP_DATA_ARCHIVE_FILE + " is missing some of the required entries: "
                        + ARCHIVE_ENTRY_MOVEMENT_DATA + "," + ARCHIVE_ENTRY_TELEPORTS + "," + ARCHIVE_ENTRY_TRANSPORTS);
            }

            return new MapData(walkableTiles, teleports, transports);
        } catch (Exception e) {
            throw new IOException("An error occurred reading the map data " + MAP_DATA_ARCHIVE_FILE, e);
        }
    }

    private Map<Coordinate, PositionInfo> readMovementData(BufferedReader movementDataStream) {
        final HashMap<Coordinate, PositionInfo> tiles = new HashMap<>();
        movementDataStream.lines()
                .filter(line -> !line.startsWith("#"))    // Filter comments
                .map(line -> line.split(",")) // Separate comma values
                .forEachOrdered(positionData -> {
                    assert (positionData.length == 8);
                    final Coordinate coordinate = new Coordinate(
                            Integer.parseInt(positionData[0]),
                            Integer.parseInt(positionData[1]),
                            Integer.parseInt(positionData[2]));
                    final WildernessLevels wildernessLevel = switch (Integer.parseInt(positionData[7])) {
                        case 0 -> WildernessLevels.BELOW20;
                        case 1 -> WildernessLevels.BETWEEN20AND30;
                        case 2 -> WildernessLevels.ABOVE30;
                        default ->
                                throw new IllegalStateException("Error reading Wilderness Level: " + Integer.parseInt(positionData[7]));
                    };
                    final PositionInfo posInfo = new PositionInfo(
                            coordinate,
                            Boolean.parseBoolean(positionData[3]),
                            Boolean.parseBoolean(positionData[4]),
                            Boolean.parseBoolean(positionData[5]),
                            Boolean.parseBoolean(positionData[6]),
                            wildernessLevel
                    );
                    tiles.put(coordinate, posInfo);
                });
        return tiles;
    }

    private Collection<TransportJson> readTransports(BufferedReader transportDataStream) {
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
                    return new TransportJson(
                            from,
                            to,
                            parts[7],
                            Byte.parseByte(parts[6]));
                })
                .collect(Collectors.toList());
    }

    private Collection<TeleportJson> readTeleports(BufferedReader teleportDataStream) {
        return teleportDataStream.lines()
                .filter(line -> !(line.startsWith("#") || line.isEmpty()))
                .map(line -> {
                    final String[] parts = line.split(",");
                    assert parts.length == 6;
                    final Coordinate to = new Coordinate(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]));
                    return new TeleportJson(
                            to,
                            parts[4],
                            Byte.parseByte(parts[3]),
                            Boolean.parseBoolean(parts[5]));
                })
                .collect(Collectors.toList());
    }

    public record MapData(
            Map<Coordinate, PositionInfo> walkableTiles,
            Collection<TeleportJson> teleports,
            Collection<TransportJson> transports) {
    }
}


