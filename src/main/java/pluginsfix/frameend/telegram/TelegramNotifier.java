package pluginsfix.frameend.telegram;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class TelegramNotifier {
    private final HttpClient httpClient;
    private final String botToken;
    private final String chatId;
    private final boolean enabled;

    public TelegramNotifier(boolean enabled, String botToken, String chatId) {
        this.enabled = enabled && botToken != null && !botToken.isBlank() && chatId != null && !chatId.isBlank();
        this.botToken = botToken;
        this.chatId = chatId;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public CompletableFuture<Boolean> sendAlert(String text) {
        if (!enabled) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
                String url = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + encodedText + "&parse_mode=HTML";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
            } catch (Exception e) {
                return false;
            }
        });
    }

    public boolean isEnabled() {
        return enabled;
    }
}
