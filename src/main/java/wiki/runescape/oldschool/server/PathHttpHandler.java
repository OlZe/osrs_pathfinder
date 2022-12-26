package wiki.runescape.oldschool.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import wiki.runescape.oldschool.logic.*;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PathHttpHandler implements HttpHandler {

    private final Graph graph = new GraphBuilder().buildGraph();
    private final PathFinder pathFinder = new PathFinder();
    private final Gson gson = new Gson();

    public PathHttpHandler() throws IOException {
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        System.out.println(LocalTime.now() + " " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

        final Map<String, String> urlParams;
        try {
            urlParams = convertQueryStringToMap(exchange.getRequestURI().getQuery());
        } catch (Exception e) {
            returnError(exchange, "Cannot parse query parameters");
            return;
        }

        if (!urlParams.containsKey("from") || !urlParams.containsKey("to")) {
            returnError(exchange, "Expected query parameters 'from' and 'to'");
            return;
        }

        final Coordinate from;
        final Coordinate to;
        try {
            from = this.convertCoordinateStringToObject(urlParams.get("from"));
            to = this.convertCoordinateStringToObject(urlParams.get("to"));
        } catch (NumberFormatException e) {
            returnError(exchange, "Cannot parse a given coordinate. Expected format: 'int,int,int'");
            return;
        }

        if (!graph.isWalkable(from) && !graph.isWalkable(to)) {
            returnError(exchange, "Start and end tile are not walkable");
            return;
        }
        if (!graph.isWalkable(from)) {
            returnError(exchange, "Start tile is not walkable");
            return;
        }
        if (!graph.isWalkable(to)) {
            returnError(exchange, "End tile is not walkable");
            return;
        }

        final PathFinderResult path = pathFinder.findPath(graph, from, to);
        final byte[] response = gson.toJson(path).getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();

    }

    private static void returnError(final HttpExchange exchange, final String errorMessage) throws IOException {
        final byte[] errorMessageBytes = errorMessage.getBytes();
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(400, errorMessageBytes.length);
        exchange.getResponseBody().write(errorMessageBytes);
        exchange.close();
    }

    private static Map<String, String> convertQueryStringToMap(final String query) {
        Map<String, String> map = new HashMap<>();
        Arrays.stream(query                                  // "from=0,0,0&to=1,1,1"
                        .split("&"))                            // "from=0,0,0"  "to=1,1,1"
                .map(s -> s.split("="))                 // "'from' '0,0,0'"  "'to' '1,1,1'"
                .forEachOrdered(q -> map.put(q[0], q[1]));
        return map;
    }

    private Coordinate convertCoordinateStringToObject(final String coordinate) {
        final String[] coordinates = coordinate.split(",");
        if (coordinates.length != 3) {
            throw new NumberFormatException();
        }
        return new Coordinate(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2]));
    }


}
