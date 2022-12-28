package wiki.runescape.oldschool.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import wiki.runescape.oldschool.logic.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalTime;

public class PathHttpHandler implements HttpHandler {

    private final Graph graph = new GraphBuilder().buildGraph();
    private final PathFinder pathFinder = new PathFinder();
    private final Gson gson = new Gson();

    public PathHttpHandler() throws IOException {
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        System.out.println(LocalTime.now() + " " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

        final PathRequestJson requestBody;
        try (final Reader requestBodyReader = new InputStreamReader(exchange.getRequestBody())){
            requestBody = gson.fromJson(requestBodyReader, PathRequestJson.class);
        } catch (Exception e) {
            returnError(exchange, "Could not parse request body");
            throw new RuntimeException(e);
        }

        if(requestBody.from() == null || requestBody.to() == null || requestBody.blacklist() == null) {
            returnError(exchange, "Request body is missing fields");
        }

        if (!graph.isWalkable(requestBody.from()) && !graph.isWalkable(requestBody.to())) {
            returnError(exchange, "Start and end tile are not walkable");
            return;
        }
        if (!graph.isWalkable(requestBody.from())) {
            returnError(exchange, "Start tile is not walkable");
            return;
        }
        if (!graph.isWalkable(requestBody.to())) {
            returnError(exchange, "End tile is not walkable");
            return;
        }

        final PathFinderResult path = pathFinder.findPath(graph, requestBody.from(), requestBody.to(), requestBody.blacklist());
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
}
