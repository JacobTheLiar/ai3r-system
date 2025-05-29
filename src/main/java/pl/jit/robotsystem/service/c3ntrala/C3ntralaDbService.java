package pl.jit.robotsystem.service.c3ntrala;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.time.Duration.ofSeconds;
import static pl.jit.robotsystem.configuration.DotenvConfiguration.AI3R_API_KEY;

@Service
@Log
public class C3ntralaDbService {

    @SuppressWarnings("FieldCanBeLocal")
    private final String DB_URL = "/apidb";
    private final WebClient webClient;
    private final String ai3rApiKey;


    public C3ntralaDbService(@Qualifier("c3ntralaApiClient") WebClient webClient, Dotenv dotenv) {
        this.webClient = webClient;
        this.ai3rApiKey = dotenv.get(AI3R_API_KEY);
    }

    public List<Map<String, String>> getQuery(String query) {
        log.info("Querying c3entrala's DB ...");
        DbRequest request = new DbRequest("database", ai3rApiKey, query);
        log.info(" - request: " + request);

        ParameterizedTypeReference<DbResponse<Map<String, String>>> responseType = new ParameterizedTypeReference<>() {
        };
        DbResponse<Map<String, String>> result = webClient.post()
                .uri(DB_URL)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(responseType)
                .timeout(ofSeconds(10))
                .block();

        if (result == null || result.error() == null || result.reply() == null) {
            log.warning("No data found");
            return Collections.emptyList();
        }
        log.info(" - response: " + result.error() + "; record count: " + result.reply().size());
        return result.reply();
    }

    record DbRequest(
            String task,
            String apikey,
            String query
    ) {
    }

    record DbResponse<T>(
            List<T> reply,
            String error
    ) {
    }
}
