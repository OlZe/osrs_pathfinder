package wiki.runescape.oldschool.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import wiki.runescape.oldschool.logic.Graph;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Collection;

public class TransportsTeleportsHttpHandler implements HttpHandler {

    private final Graph graph;
    private final Gson gson = new Gson();

    public TransportsTeleportsHttpHandler(final Graph graph) {
        this.graph = graph;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        System.out.println(LocalTime.now() + " " + exchange.getRequestURI() + " " + exchange.getRequestURI());

        Collection<String> allTransportsTeleports = graph.getAllTeleportsTransports();
        final byte[] response = gson.toJson(allTransportsTeleports).getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
