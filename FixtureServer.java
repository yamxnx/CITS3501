import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class FixtureServer {
    public static void main(String[] args) throws IOException {
        byte[] body = Files.readAllBytes(Path.of("invalid-policy.pdf"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8765), 0);
        server.createContext("/invalid-policy.pdf", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/pdf");
            exchange.getResponseHeaders().set(
                    "Content-Disposition", "attachment; filename=\"invalid-policy.pdf\"");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        System.out.println("Serving http://127.0.0.1:8765/invalid-policy.pdf (Ctrl-C to stop)");
    }
}
