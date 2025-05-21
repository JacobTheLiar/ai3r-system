package pl.jit.robotsystem.service.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import static java.time.Duration.ofSeconds;

@Service
@Log
public class OpenAiService {

    private final WebClient openaiWebClient;
    private final ObjectMapper objectMapper;

    public OpenAiService(@Qualifier("openAiClient") WebClient openaiWebClient, ObjectMapper objectMapper) {
        this.openaiWebClient = openaiWebClient;
        this.objectMapper = objectMapper;
    }

    public String getCompletion(String systemPrompt, String userMessage) {
        ChatResponse response = openaiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(getRequest(systemPrompt, userMessage))
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .timeout(ofSeconds(100))
                .block();

        return Optional.ofNullable(response)
                .map(ChatResponse::choices)
                .orElse(Collections.emptyList())
                .stream()
                .findFirst()
                .map(Choice::message)
                .map(Message::content)
                .orElse(null);
    }

    public String transcribeAudio(Path audioFile){
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new FileSystemResource(audioFile))
                .header("Content-Type", "audio/mpeg");
        bodyBuilder.part("model", "whisper-1");

        return openaiWebClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(TranscriptionResponse.class)
                .map(TranscriptionResponse::text)
                .block();
    }

    @SuppressWarnings("unused")
    public <T> T getCompletion(String systemPrompt, String userMessage, Class<T> responseClass) {
        String completion = getCompletion(systemPrompt, userMessage);
        if (responseClass.equals(String.class)) {
            //noinspection unchecked
            return (T) completion;
        }
        return Optional.of(completion)
                .map(stringObject -> mapToObject(stringObject, responseClass))
                .orElse( null);
    }

    public <T> List<T> getCompletionList(String systemPrompt, String userMessage, Class<T> responseItemClass) {
        String completion = getCompletion(systemPrompt, userMessage);
        return Optional.ofNullable(completion)
                .map(content -> mapToList(content, responseItemClass))
                .orElseGet(Collections::emptyList);
    }

    public ImageGenerationResponse generateImage(String prompt) {
        return openaiWebClient.post()
                .uri("/images/generations")
                .bodyValue(ImageGenerationRequest.builder()
                        .prompt(prompt)
                        .model("dall-e-3")
                        .n(1)
                        .size("1024x1024")
                        .quality("hd")
                        .style("natural") //vivid
                        .build())
                .retrieve()
                .bodyToMono(ImageGenerationResponse.class)
                .timeout(ofSeconds(30))
                .block();
    }

    private  <T> T mapToObject(String value, Class<T> responseClass){
        try {
            return objectMapper.readValue(value, responseClass);
        } catch (JsonProcessingException e) {
            log.log(Level.SEVERE, "Error parsing response", e);
            return null;
        }
    }

    private  <T> List<T> mapToList(String value, Class<T> responseItemClass){
        try {
            return objectMapper.readValue(value, objectMapper.getTypeFactory().constructCollectionType(List.class, responseItemClass));
        } catch (JsonProcessingException e) {
            log.log(Level.SEVERE, "Error parsing response", e);
            return null;
        }
    }

    private static ChatRequest getRequest(String systemPrompt, String userMessage) {
        return ChatRequest.builder()
                .model("gpt-4.1-mini")
                .messages(List.of(
                        Message.builder().role("system").content(systemPrompt).build(),
                        Message.builder().role("user").content(userMessage).build()
                ))
                .build();
    }
}
