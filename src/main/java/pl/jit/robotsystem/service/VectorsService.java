package pl.jit.robotsystem.service;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.qdrant.QdrantService;
import pl.jit.robotsystem.service.qdrant.vectors.DocumentIndexingService;
import pl.jit.robotsystem.service.zip.EncryptedZipService;
import pl.jit.robotsystem.service.zip.ZipService;
import pl.jit.robotsystem.share.FlagFinder;
import pl.jit.robotsystem.share.No5Action;

import java.io.File;
import java.util.*;

@Service
@Log
public class VectorsService implements No5Action {

    private static final String QUESTION = "W raporcie, z którego dnia znajduje się wzmianka o kradzieży prototypu broni?";

    private final C3ntralaService c3ntralaService;
    private final ZipService zipService;
    private final EncryptedZipService encryptedZipService;
    private final DocumentIndexingService documentIndexingService;

    public VectorsService(C3ntralaService c3ntralaService, ZipService zipService, EncryptedZipService encryptedZipService, DocumentIndexingService documentIndexingService) {
        this.c3ntralaService = c3ntralaService;
        this.zipService = zipService;
        this.encryptedZipService = encryptedZipService;
        this.documentIndexingService = documentIndexingService;
    }

    public void action() {
        File sourceFile = c3ntralaService.downloadFile("dane/pliki_z_fabryki.zip", "pliki_z_fabryki.zip");

        List<File> fileList = zipService.unzipFile(sourceFile, "vectors").stream()
                .filter(file -> file.getName().equals("weapons_tests.zip"))
                .findFirst()
                .map(this::extractEncrypted)
                .filter(content -> !content.isEmpty())
                .orElse(Collections.emptyList());


        documentIndexingService.indexDocumentsOnce(fileList).block();
        QdrantService.QdrantSearchResponse response = documentIndexingService.searchAnswer(QUESTION).block();
        if (response == null) {
            log.warning("No documents found");
            return;
        }
        String answer = extractAnswerFromResults(response);

        log.info("Sending answer to c3ntrala: " + answer);
        c3ntralaService.report("wektory", answer, String.class)
                .ifPresent(FlagFinder::containsFlag);
        log.info("Done!");
    }

    private List<File> extractEncrypted(File sourceFile) {
        String decodedPassword = new String(Base64.getDecoder().decode("MTY3MA=="));
        return encryptedZipService.unzipFile(sourceFile, "vectors/weapons", decodedPassword.toCharArray());
    }

    private String extractAnswerFromResults(QdrantService.QdrantSearchResponse response) {
        log.info("Search response: ");
        return response.result().stream()
                .map(result -> {
                    Map<String, Object> payload = result.payload();
                    String text = (String) payload.get("chunk_text");
                    String date = (String) payload.get("date");
                    log.info(" -  score: "+ result.score() + "; date: " + date+"; text: " + text);
                    String lowerText = text.toLowerCase();
                    if (lowerText.contains("kradzież") || lowerText.contains("kradzieży") ||
                        lowerText.contains("skradziono") || lowerText.contains("ukradziono") ||
                        lowerText.contains("prototyp") && (lowerText.contains("zginął") || lowerText.contains("brak"))) {
                        return date;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Nie znaleziono informacji o kradzieży prototypu");
    }
}
