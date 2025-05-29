package pl.jit.robotsystem.service.qdrant.vectors;

import lombok.extern.java.Log;
import org.springframework.stereotype.Component;
import pl.jit.robotsystem.service.qdrant.EmbeddingService;
import pl.jit.robotsystem.service.qdrant.QdrantService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

@Component
@Log
public class DocumentIndexingService {
    private final QdrantService qdrantService;
    private final EmbeddingService embeddingService;

    private static final String COLLECTION_NAME = "wektory";

    public DocumentIndexingService(QdrantService qdrantService, EmbeddingService embeddingService) {
        this.qdrantService = qdrantService;
        this.embeddingService = embeddingService;
    }

    /**
     * Indeksuje pliki, tylko jeśli nie były wcześniej przetworzone
     */
    public Mono<Void> indexDocumentsOnce(List<File> files) {
        return isAlreadyIndexed()
                .flatMap(indexed -> {
                    if (indexed) {
                        log.info("Documents already indexed, skipping...");
                        return Mono.empty();
                    }
                    return ensureCollectionExists()
                            .then(indexAllDocuments(files))
                            .doOnSuccess(v -> log.info("Successfully indexed " + files.size() + " documents"));
                });
    }

    private Mono<Void> indexAllDocuments(List<File> files) {
        return Flux.fromIterable(files)
                .flatMap(this::indexSingleDocument, 3)
                .then();
    }

    private Mono<Void> indexSingleDocument(File file) {
        try {
            //noinspection BlockingMethodInNonBlockingContext
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String fileName = file.getName();
            String date = extractDateFromFilename(fileName);

            log.info("Processing file: " +fileName+ ", extracted date: " + date);

            Map<String, Object> metadata = Map.of(
                    "filename", fileName,
                    "date", date,
                    "file_path", file.getAbsolutePath(),
                    "indexed_at", Instant.now().toString(),
                    "document_type", "report"
            );

            return embeddingService.processTextFile(content)
                    .flatMap(chunk -> {
                        String pointId = UUID.randomUUID().toString();

                        String enrichedText = "Data raportu: " + date + "\n\n" + chunk.text();

                        Map<String, Object> chunkMetadata = new HashMap<>(metadata);
                        chunkMetadata.put("chunk_text", chunk.text());
                        chunkMetadata.put("enriched_text", enrichedText);
                        chunkMetadata.put("original_chunk_id", chunk.id());

                        log.info("Using UUID: " + pointId + " for chunk: " + chunk.id());

                        return qdrantService.upsertPoint(
                                COLLECTION_NAME,
                                pointId,
                                chunk.embedding(),
                                chunkMetadata
                        );
                    })
                    .then()
                    .doOnSuccess(v -> log.info("Indexed document: " + fileName));

        } catch (IOException e) {
            log.severe("Failed to read file: " + file.getName() + " - " + e.getMessage());
            return Mono.error(e);
        }
    }

    /**
     * Sprawdza, czy dokumenty już zostały zaindeksowane przez sprawdzenie, czy kolekcja istnieje i ma punkty
     */
    private Mono<Boolean> isAlreadyIndexed() {
        return qdrantService.getCollectionInfo(COLLECTION_NAME)
                .map(info -> {
                    // Sprawdź, czy ma punkty (nie tylko czy istnieje)
                    return info.contains("\"points_count\"") &&
                           !info.contains("\"points_count\":0") &&
                           !info.contains("\"points_count\": 0");
                })
                .onErrorReturn(false);
    }

    /**
     * Tworzy kolekcję, tylko jeśli nie istnieje
     */
    private Mono<Void> ensureCollectionExists() {
        return qdrantService.getCollectionInfo(COLLECTION_NAME)
                .doOnSuccess(info -> log.info("Collection already exists"))
                .then()
                .onErrorResume(throwable -> {
                    log.info("Collection doesn't exist, creating...");
                    return qdrantService.createCollection(COLLECTION_NAME).then();
                });
    }

    public Mono<QdrantService.QdrantSearchResponse> searchAnswer(String question) {
        return embeddingService.generateEmbedding(question)
                .flatMap(queryVector ->
                        qdrantService.searchSimilar(COLLECTION_NAME, queryVector, 1))
                .doOnSuccess(answer -> log.info("Found answer: " + answer));
    }

    private String extractDateFromFilename(String filename) {
        // DEBUG: dodaj więcej logowania
        log.info("Extracting date from filename: "+ filename);

        // Format: 2024_01_17.txt
        if (filename.matches("\\d{4}_\\d{2}_\\d{2}\\.txt")) {
            String dateStr = filename.substring(0, 10).replace("_", "-");
            log.info("Extracted date: "+ dateStr);
            return dateStr;
        }

        // Format: 2024-11-12_report-02-sektor_A3.txt
        String[] parts = filename.split("_");
        if (parts.length > 0 && parts[0].matches("\\d{4}-\\d{2}-\\d{2}")) {
            return parts[0];
        }

        log.warning("Could not extract date from: "+ filename);
        return "unknown";
    }

    /**
     * Wymuś ponowne indeksowanie (usuń całą kolekcję)
     */
    @SuppressWarnings("unused")
    public Mono<Void> resetIndex() {
        return qdrantService.deleteCollection(COLLECTION_NAME)
                .doOnSuccess(v -> log.info("Collection deleted - documents will be reindex on next run"))
                .then();
    }
}
