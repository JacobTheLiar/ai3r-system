package pl.jit.robotsystem.service.qdrant;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@Log
public class QdrantService {
    private final WebClient webClient;
    private static final int GPT4_MINI_VECTOR_SIZE = 1536; // Rozmiar embeddings dla gpt-4o-mini

    public QdrantService(@Qualifier("localQdrantClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Tworzy nową kolekcję w Qdrant
     * Użyj na początku pracy z nową kolekcją
     */
    public Mono<String> createCollection(String collectionName) {
        var config = Map.of(
                "vectors", Map.of(
                        "size", GPT4_MINI_VECTOR_SIZE,
                        "distance", "Cosine"
                )
        );

        return webClient.put()
                .uri("/{collection}", collectionName)
                .bodyValue(config)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Created collection: " + collectionName))
                .doOnError(error -> log.entering("Failed to create collection: {}", error.getMessage()));
    }

    /**
     * Dodaje punkt (dokument) do kolekcji
     * Użyj do zapisywania dokumentów z ich wektorami
     */
    public Mono<String> upsertPoint(String collectionName, String pointId,
                                    List<Double> vector, Map<String, Object> payload) {
        var point = Map.of(
                "points", List.of(Map.of(
                        "id", pointId,
                        "vector", vector,
                        "payload", payload
                ))
        );

        log.info("Upsert point: " + point);

        return webClient.put()
                .uri("/{collection}/points", collectionName)
                .bodyValue(point)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Upserted point " + pointId + " to " + collectionName));
    }

    /**
     * Wyszukuje najbliższe punkty do podanego wektora
     * Główna metoda do semantic search - znajdź podobne dokumenty
     */
    public Mono<QdrantSearchResponse> searchSimilar(String collectionName,
                                                    List<Double> queryVector,
                                                    int limit) {
        var searchQuery = Map.of(
                "vector", queryVector,
                "limit", limit,
                "with_payload", true,
                "with_vector", false
        );

        return webClient.post()
                .uri("/{collection}/points/search", collectionName)
                .bodyValue(searchQuery)
                .retrieve()
                .bodyToMono(QdrantSearchResponse.class)
                .doOnSuccess(response -> log.info("Found " + response.result().size() + " results in +" + collectionName));
    }

    /**
     * Pobiera punkt po ID,
     * Użyj, gdy chcesz sprawdzić konkretny dokument
     */
    @SuppressWarnings("unused")
    public Mono<QdrantPoint> getPoint(String collectionName, String pointId) {
        return webClient.get()
                .uri("/{collection}/points/{id}", collectionName, pointId)
                .retrieve()
                .bodyToMono(QdrantPointResponse.class)
                .map(QdrantPointResponse::result);
    }

    /**
     * Usuwa punkt z kolekcji
     */
    @SuppressWarnings("unused")
    public Mono<String> deletePoint(String collectionName, String pointId) {
        var deleteRequest = Map.of("points", List.of(pointId));

        return webClient.post()
                .uri("/{collection}/points/delete", collectionName)
                .bodyValue(deleteRequest)
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Sprawdza info o kolekcji
     */
    public Mono<String> getCollectionInfo(String collectionName) {
        return webClient.get()
                .uri("/{collection}", collectionName)
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Usuwa kolekcję
     */
    public Mono<String> deleteCollection(String collectionName) {
        return webClient.delete()
                .uri("/{collection}", collectionName)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Deleted collection: " + collectionName));
    }

    public record QdrantSearchResponse(
            List<QdrantSearchResult> result,
            String status,
            double time
    ) {}

    public record QdrantSearchResult(
            String id,
            double score,
            Map<String, Object> payload,
            List<Double> vector
    ) {}

    public record QdrantPoint(
            String id,
            List<Double> vector,
            Map<String, Object> payload
    ) {}

    public record QdrantPointResponse(
            QdrantPoint result,
            String status,
            double time
    ) {}
}
