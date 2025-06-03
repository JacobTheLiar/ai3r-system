package pl.jit.robotsystem.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.c3ntrala.ReportResponse;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.prompt.PromptRepository;
import pl.jit.robotsystem.share.FlagFinder;
import pl.jit.robotsystem.share.No5Action;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * S04E01 - clarify photo, and describe
 */
@Service
@Log
public class PhotosService implements No5Action {

    private static final String PHOTOS_TASK = "photos";

    private final C3ntralaService c3ntralaService;
    private final OpenAiService openAiService;
    private final PromptRepository promptRepository;
    private final ObjectMapper objectMapper;


    public PhotosService(C3ntralaService c3ntralaService, OpenAiService openAiService, PromptRepository promptRepository, ObjectMapper objectMapper) {
        this.c3ntralaService = c3ntralaService;
        this.openAiService = openAiService;
        this.promptRepository = promptRepository;
        this.objectMapper = objectMapper;
    }

    public void action() {
        letsStart()
                .map(this::extractFilesAndOperations)
                .map(this::processAllFiles)
                .map(this::prepareOverallDescription)
                .map(this::sendDescriptionToC3ntrala)
                .ifPresent(FlagFinder::containsFlag);
        log.info("Done!");
    }

    private Optional<String> letsStart() {
        log.info("Getting message about photos...");
        return c3ntralaService.report(PHOTOS_TASK, "START", ReportResponse.class)
                .stream()
                .filter(response -> response.code().equals(0))
                .map(ReportResponse::message)
                .findFirst();
    }

    private FilesOperations extractFilesAndOperations(String message) {
        log.info("extracting possibilities...");
        String prompt = promptRepository.getPromptData("s04e01-extract-files");
        FilesOperations filesOperations = openAiService.getCompletion(prompt, message, FilesOperations.class);
        log.info(" - operations: " + filesOperations.operations());
        log.info(" - photos: " + filesOperations.photos());
        return filesOperations;
    }

    private Map<String, String> processAllFiles(FilesOperations filesOperations) {
        return filesOperations.photos().stream()
                .collect(Collectors.toMap(
                        photo -> photo,
                        photo -> processSinglePhoto(photo, filesOperations.operations())
                ));
    }

    private String processSinglePhoto(String photoName, List<String> availableOperations) {
        log.info("Work with photo: " + photoName);
        File file = getFile(photoName);
        List<String> performedOperations = new ArrayList<>();
        while (true) {
            VisionResponse response = analyzePhoto(file, performedOperations, availableOperations);
            if (isDone(response)) {
                return response == null || response.description == null ? "no description" : response.description;
            }

            performedOperations.add(response.operation);
            String finePhotoName = finePhoto(response.operation, photoName);
            if (finePhotoName.contains("NOT FOUND")) {
                return "no description";
            }
            file = getFile(finePhotoName);
        }
    }

    private boolean isDone(VisionResponse response) {
        return response == null || "SKIP".equalsIgnoreCase(response.operation) ||
               ("OK".equalsIgnoreCase(response.operation) && !response.description.isBlank());
    }

    private VisionResponse analyzePhoto(File file, List<String> performedOperations, List<String> availableOperations) {
        log.info(" - analyzing...");
        String analyzePrompt = promptRepository.getPromptData("s04e01-analyze-photo")
                .replace("{{operations}}", availableOperations.toString())
                .replace("{{performed_operations}}", performedOperations.toString());

        String visionResponse = openAiService.workWithImage(analyzePrompt, file);
        log.info("   - answer: " + visionResponse);
        try {
            return objectMapper.readValue(visionResponse, VisionResponse.class);
        } catch (JsonProcessingException e) {
            log.severe("problem with response processing" + e.getMessage());
        }
        return null;
    }


    private String prepareOverallDescription(Map<String, String> descriptions) {
        log.info("prepare overallDescription...");
        String allDescriptions = descriptions.entrySet().stream()
                .map(entry -> "# " + entry.getKey() + "\n" + entry.getValue())
                .collect(Collectors.joining("\n\n"));
        String descriptionPrompt = promptRepository.getPromptData("s04e01-description");
        String overallDescription = openAiService.getCompletion(descriptionPrompt, allDescriptions, String.class);
        log.info(" - prepared: " + overallDescription);
        return overallDescription;
    }

    private String sendDescriptionToC3ntrala(String overallDescription) {
        return c3ntralaService.report(PHOTOS_TASK, overallDescription, ReportResponse.class)
                .map(ReportResponse::message)
                .orElseThrow();
    }

    private File getFile(String fileName) {
        log.info(" - downloading file: %s...".formatted(fileName));
        return c3ntralaService.getPublicData("barbara/" + fileName, byte[].class)
                .map((byte[] resource) -> resourceToFile(resource, fileName))
                .orElseThrow();
    }

    private String finePhoto(String operation, String filename) {
        String fineMessage = c3ntralaService.report(PHOTOS_TASK, operation + " " + filename, ReportResponse.class)
                .filter(r -> r.code().equals(0))
                .map(ReportResponse::message)
                .orElseThrow();
        String finedPrompt = promptRepository.getPromptData("s04e01-extract-file-name");
        return openAiService.getCompletion(finedPrompt, fineMessage);
    }

    private File resourceToFile(byte[] data, String filename) {
        try {
            File tempFile = File.createTempFile("prefix", filename);
            Files.write(tempFile.toPath(), data);
            return tempFile;
        } catch (IOException e) {
            log.severe("Cannot create temp file");
            return null;
        }
    }

    record VisionResponse(
            String thinking,
            String operation,
            String description
    ) {
    }

    record FilesOperations(
            List<String> operations,
            List<String> photos
    ) {
    }
}
