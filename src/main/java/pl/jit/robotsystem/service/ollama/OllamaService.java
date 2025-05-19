package pl.jit.robotsystem.service.ollama;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pl.jit.robotsystem.share.ThinkingRemover;

import static java.time.Duration.ofSeconds;
import static java.util.Optional.ofNullable;


@Service
public class OllamaService {

    private static final String DEFAULT_MODEL = "qwen3:4b-q4_K_M";

    private final WebClient ollamaClient;

    public OllamaService(@Qualifier("ollamaClient") WebClient openaiWebClient) {
        this.ollamaClient = openaiWebClient;
    }

    public String getCompletion(String systemPrompt, String userMessage) {
        OllamaResponse response = ollamaClient.post()
                .uri("/api/generate")
                .bodyValue(getRequest(systemPrompt, userMessage))
                .retrieve()
                .bodyToMono(OllamaResponse.class)
                .timeout(ofSeconds(60))
                .block();

        return ofNullable(response)
                .map(OllamaResponse::response)
                .map(ThinkingRemover::removeThinkingProcess)
                .orElse("");
    }


    private static OllamaRequest getRequest(String systemPrompt, String userMessage) {
        return OllamaRequest.builder()
                .model(DEFAULT_MODEL)
                .stream(false)
                .prompt(userMessage)
                .system(systemPrompt)
                .build();
    }
}
