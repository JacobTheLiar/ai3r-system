package pl.jit.robotsystem.service.qdrant;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log
public class EmbeddingService {

    private static final int CHUNK_SIZE = 800;
    private static final int OVERLAP_SIZE = 100;
    private static final String OPENAI_EMBEDDINGS_URL = "/embeddings";

    private final WebClient webClient;

    public EmbeddingService(@Qualifier("openAiClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<List<Double>> generateEmbedding(String text) {
        var request = Map.of(
                "model", "text-embedding-3-small", // Tańszy niż ada-002
                "input", text
        );

        log.info("generate embeddings for: "+request);

        return webClient.post()
                .uri(OPENAI_EMBEDDINGS_URL)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .map(response -> {
                    List<Double> embedding = response.data().getFirst().embedding();
                    log.info("Generated embedding size: " + embedding.size()); // DEBUG
                    return embedding;
                })
                .doOnError(error -> log.severe("Failed to generate embedding: " + error.getMessage()));
    }

    public Flux<DocumentChunk> processTextFile(String content) {
        return Flux.fromIterable(splitIntoChunks(content))
                .index()
                .flatMap(tuple -> {
                    UUID chunkId = UUID.randomUUID(); // Generuj UUID
                    String chunkText = tuple.getT2();

                    return generateEmbedding(chunkText)
                            .map(embedding -> new DocumentChunk(chunkId, chunkText, embedding))
                            .doOnSuccess(chunk -> log.info("Processed chunk: " + chunkId));
                });
    }

    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();

        if (text.length() <= CHUNK_SIZE) {
            chunks.add(text.trim());
            return chunks;
        }

        String[] sentences = text.split("\\. ");
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            String potentialChunk = currentChunk + sentence + ". ";

            if (potentialChunk.length() > CHUNK_SIZE && !currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString().trim());

                // Overlap - weź ostatnie zdania z poprzedniego chunk
                String overlap = getLastWords(currentChunk.toString());
                currentChunk = new StringBuilder(overlap + sentence + ". ");
            } else {
                currentChunk.append(sentence).append(". ");
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks.stream()
                .filter(chunk -> !chunk.trim().isEmpty())
                .collect(Collectors.toList());
    }

    private String getLastWords(String text) {
        if (text.length() <= EmbeddingService.OVERLAP_SIZE) {
            return text;
        }
        String substring = text.substring(Math.max(0, text.length() - EmbeddingService.OVERLAP_SIZE));
        int spaceIndex = substring.indexOf(' ');
        return spaceIndex > 0 ? substring.substring(spaceIndex + 1) : substring;
    }

    public record DocumentChunk(
            UUID id,
            String text,
            List<Double> embedding
    ) {}

    public record EmbeddingResponse(
            String object,
            List<EmbeddingData> data,
            String model,
            Usage usage
    ) {}

    public record EmbeddingData(
            String object,
            int index,
            List<Double> embedding
    ) {}

    public record Usage(
            int prompt_tokens,
            int total_tokens
    ) {}
}
