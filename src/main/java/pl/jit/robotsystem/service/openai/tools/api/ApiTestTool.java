package pl.jit.robotsystem.service.openai.tools.api;

import lombok.extern.java.Log;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.jit.robotsystem.service.openai.tools.Tool;

import java.time.Duration;
import java.util.Map;

@Log
public class ApiTestTool implements Tool {

    private final WebClient webClient;

    public ApiTestTool() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    @Override
    public String getName() {
        return "api_test";
    }

    @Override
    public String getDescription() {
        return "Testuje endpoint API z hasłem. Użyj: api_test(endpoint,hasło) np. api_test(https://example.com/api,mypassword)"
                +"\n[TOOL:api_test:]";
    }

    @Override
    public String execute(String input) {
        try {
            // Parsowanie parametrów: "endpoint,hasło"
            String[] params = parseParameters(input);
            if (params.length != 2) {
                return "Błąd: Podaj endpoint i hasło oddzielone przecinkiem. Przykład: https://api.com,hasło123";
            }

            String endpoint = params[0].trim();
            String password = params[1].trim();



            if (!isValidUrl(endpoint)) {
                return "Błąd: Nieprawidłowy format URL: " + endpoint;
            }

            log.info("==> TOOL: "+getName());
            log.info(" - url: " + endpoint);
            log.info(" - pwd: " + password);
            return testApiEndpoint(endpoint, password);

        } catch (Exception e) {
            return "Błąd podczas testowania API: " + e.getMessage();
        }
    }

    private String[] parseParameters(String input) {
        // Obsługa różnych formatów:
        // "endpoint,password"
        // "endpoint, password"
        // "(endpoint,password)"
        String cleaned = input.trim();
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        return cleaned.split(",", 2);
    }

    private boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private String testApiEndpoint(String endpoint, String password) {
        try {
            // Test z metodą POST i JSON payload
            String response = webClient.post()
                    .uri(endpoint)
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of("password", password))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            return formatApiResponse(endpoint, password, response, "SUCCESS");

        } catch (WebClientResponseException e) {
            return formatApiResponse(endpoint, password, e.getResponseBodyAsString(),
                    "ERROR " + e.getStatusCode());
        } catch (Exception e) {
            return formatApiResponse(endpoint, password, e.getMessage(), "TIMEOUT/ERROR");
        }
    }

    private String formatApiResponse(String endpoint, String password, String response, String status) {
        return String.format("""
                ENDPOINT TEST RESULT:
                URL: %s
                Password: %s
                Status: %s
                Response: %s
                """, endpoint, password, status, response);
    }
}
