package pl.jit.robotsystem.service.openai.tools.gps;

import lombok.extern.java.Log;
import org.springframework.web.reactive.function.client.WebClient;
import pl.jit.robotsystem.service.openai.tools.Tool;

import java.util.logging.Level;

@Log
public class GpsTool implements Tool {

    private final WebClient webClient;

    public GpsTool(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public String getName() {
        return "gps";
    }

    @Override
    public String getDescription() {
        return "Umożliwia pozyskanie lokalizacji danej osoby na podstawie jego identyfikatora, użyj 28 by otrzymać koordynaty osoby z identyfikatorem 28";
    }

    @Override
    public String execute(String input) {
        try {
            String cleanInput = input.trim();
            log.info("GPS Tool: szukam lokalizacji dla ID: " + cleanInput);

            Integer userId = Integer.parseInt(cleanInput);
            GpsRequest request = new GpsRequest(userId);

            GpsResponse response = webClient.post()
                    .uri("/gps")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GpsResponse.class)
                    .block();

            if (response != null) {
                if (response.code() == 0 && response.message() != null) {
                    String result = String.format("GPS_SUCCESS: Znaleziono pozycję dla ID %s - LAT: %s, LON: %s",
                            cleanInput,
                            response.message().lat(),
                            response.message().lon());
                    log.info("GPS Tool: " + result);
                    return result;
                } else {
                    String error = String.format("GPS_ERROR: Kod błędu %d dla ID %s", response.code(), cleanInput);
                    log.warning("GPS Tool: " + error);
                    return error;
                }
            } else {
                String error = "GPS_ERROR: Brak odpowiedzi z serwera dla ID " + cleanInput;
                log.warning("GPS Tool: " + error);
                return error;
            }

        } catch (NumberFormatException e) {
            String error = "GPS_ERROR: Nieprawidłowy format ID: " + input + " (wymagana liczba)";
            log.log(Level.WARNING, "GPS Tool: " + error, e);
            return error;
        } catch (Exception e) {
            String error = "GPS_ERROR: Błąd wykonania dla ID " + input + ": " + e.getMessage();
            log.log(Level.WARNING, "GPS Tool: " + error, e);
            return error;
        }
    }

    private record GpsRequest(
            Integer userID
    ) {
    }

    private record GpsResponse(
            int code,
            GpsMessage message
    ) {
    }

    private record GpsMessage(
            String lat,
            String lon
    ) {
    }
}