package wiki.runescape.oldschool.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalTime;

/**
 * @param <T> The Type of the request body
 */
public abstract class JsonHttpHandler<T> implements HttpHandler {
    private final Gson gson = new Gson();
    private final Class<T> classOfRequestBody;

    protected JsonHttpHandler(final Class<T> classOfRequestBody) {
        // This stupid workaround is needed because I need the class of T when reading json
        this.classOfRequestBody = classOfRequestBody;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        System.out.println(LocalTime.now() + " " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

        byte[] replyBody = {};
        int replyStatusCode = 400;
        try {
            final Reader requestBodyReader = new InputStreamReader(exchange.getRequestBody());
            final T requestBody = this.gson.fromJson(requestBodyReader, classOfRequestBody);
            final Reply reply = this.handle(new Request<>(requestBody));
            replyBody = gson.toJson(reply.body).getBytes();
            if(!reply.isError) {
                replyStatusCode = 200;
            }
        } catch (Exception e) {
            replyBody = this.gson.toJson(e.getMessage()).getBytes();
        } finally {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(replyStatusCode, replyBody.length);
            exchange.getResponseBody().write(replyBody);
            exchange.close();
        }
    }

    protected abstract Reply handle(Request<T> request);


    public record Request<T>(T requestBody) {
    }

    public record Reply(boolean isError, Object body) {
    }

}
