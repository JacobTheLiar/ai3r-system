package pl.jit.robotsystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.openai.Message;
import pl.jit.robotsystem.service.zip.ZipService;
import pl.jit.robotsystem.share.No5Action;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * S04E02 - prepare data for a fine-tune model
 */
@Service
@Log
public class ResearchPrepareService implements No5Action {

    private final C3ntralaService c3ntralaService;
    private final ObjectMapper objectMapper;
    private final ZipService zipService;


    public ResearchPrepareService(C3ntralaService c3ntralaService, ObjectMapper objectMapper, ZipService zipService) {
        this.c3ntralaService = c3ntralaService;
        this.objectMapper = objectMapper;
        this.zipService = zipService;
    }

    public void action() {
        File testData = c3ntralaService.downloadFile("dane/lab_data.zip", "lab_data/lab_data.zip");
        List<Messages> list = zipService.unzipFile(testData).stream()
                .filter(Predicate.not(file -> "verify.txt".equals(file.getName())))
                .map(this::processFile)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        Collections.shuffle(list);
        String jsonlContent = mapToString(list);
        log.info("Prepared " + list.size() + " training examples");
        saveResults(jsonlContent, testData.getParent());
        log.info("Done!");
    }

    private List<Messages> processFile(File file) {
        boolean correct = "correct.txt".equalsIgnoreCase(file.getName());
        try {
            return Files.readAllLines(file.toPath()).stream()
                    .map(line -> prepareMessages(line, correct))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.severe("Error reading file: " + file.getName());
        }
        return Collections.emptyList();
    }

    private Messages prepareMessages(String data, boolean valid) {
        return new Messages(
                List.of(
                        Message.<String>builder().role("system").content("You are a data validator. Return 1 for valid data, 0 for invalid data.").build(),
                        Message.<String>builder().role("user").content(data).build(),
                        Message.<String>builder().role("assistant").content(valid ? "1" : "0").build()
                )
        );
    }

    private String mapToString(List<Messages> messages) {
        return messages.stream()
                .map(this::serializeMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    private String serializeMessage(Messages message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.warning("Cannot serialize message: " + e.getMessage());
            return null;
        }
    }

    private void saveResults(String jsonlContent, String path) {
        try {
            Path fineTuning = Path.of(path, "fine-tuning.jsonl");
            Files.writeString(fineTuning, jsonlContent);
        } catch (IOException e) {
            log.warning("cannot save results");
        }
    }

    record Messages(
            List<Message<String>> messages
    ) {
    }
}
