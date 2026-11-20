import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HealthCheck {
    private HealthCheck() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.exit(2);
        }
        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build()
                .send(HttpRequest.newBuilder(URI.create(args[0])).timeout(Duration.ofSeconds(4)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        System.exit(response.statusCode() >= 200 && response.statusCode() < 300 ? 0 : 1);
    }
}
