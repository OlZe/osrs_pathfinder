package wiki.runescape.oldschool;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    private static final Coordinate LUMBRIDGE_SPAWN = new Coordinate(3222, 3218, 0);
    private static final Coordinate VARROCK_SQUARE = new Coordinate(3213, 3429, 0);
    private static final Coordinate GE_SPIRIT_TREE = new Coordinate(3182, 3507, 0);
    private static final Coordinate TREE_GNOME_VILLAGE = new Coordinate(2534, 3166, 0);
    private static final Coordinate EDGEVILLE_WILDERNESS_DITCH = new Coordinate(3086, 3520, 0);
    private static final int PORT = 8100;

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/path.json", new PathHttpHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server listening on port " + PORT + "...");
    }
}
