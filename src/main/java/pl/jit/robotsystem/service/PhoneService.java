package pl.jit.robotsystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.openai.tools.OpenAiToolsService;
import pl.jit.robotsystem.service.openai.tools.Tool;
import pl.jit.robotsystem.service.openai.tools.centrala.CentralaTool;
import pl.jit.robotsystem.service.openai.tools.api.ApiTestTool;
import pl.jit.robotsystem.service.openai.tools.conversations.ConversationsTool;
import pl.jit.robotsystem.service.openai.tools.facts.FactsTool;
import pl.jit.robotsystem.service.prompt.PromptRepository;
import pl.jit.robotsystem.service.zip.ZipService;
import pl.jit.robotsystem.share.FlagFinder;
import pl.jit.robotsystem.share.No5Action;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S05E01 - sort transcription and answer question
 */
@Service
@Log
public class PhoneService implements No5Action {

    private final C3ntralaService c3ntralaService;
    private final PromptRepository promptRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final ZipService zipService;
    private final OpenAiToolsService openAiToolsService;

    public PhoneService(C3ntralaService c3ntralaService, PromptRepository promptRepository, OpenAiService openAiService, ObjectMapper objectMapper, ZipService zipService, OpenAiToolsService openAiToolsService) {
        this.c3ntralaService = c3ntralaService;
        this.promptRepository = promptRepository;
        this.openAiService = openAiService;
        this.objectMapper = objectMapper;
        this.zipService = zipService;
        this.openAiToolsService = openAiToolsService;
    }

    public void action() {
        log.info("Getting system prompt...");
        String promptData = promptRepository.getPromptData("s05e01-detective");

        log.info("Preparing tools...");
        List<Tool> tools = List.of(
                new ConversationsTool(getConversations()),
                new FactsTool(openAiService, getFacts()),
                new ApiTestTool(),
                new CentralaTool(c3ntralaService, objectMapper)
        );

        log.info("Getting questions...");
        String questionsJson = c3ntralaService.getData("phone_questions.json", String.class)
                .orElseThrow();

        Map<String, String> questions = mapToObject(questionsJson);

        // TYLKO pytania jako user message
        String userMessage = formatQuestionsOnly(questions);

        log.info("Uruchamianie detektywa AI...");
        String result = openAiToolsService.getCompletionWithTools(
                promptData,
                userMessage,
                tools
        );

        log.info("=== WYNIK ===");
        log.info(result);

        if (result.contains("FLG:")) {
            FlagFinder.containsFlag(result);
        }
    }

    private String formatQuestionsOnly(Map<String, String> questions) {
        return questions.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    // Pozostałe metody pomocnicze bez zmian
    private Map<String, String> mapToObject(@NotNull String string) {
        try {
            return objectMapper.readValue(string, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Map<String, List<String>> getConversations() {
        String talksString = c3ntralaService.getData("phone_sorted.json", String.class)
                .map(s -> s.replaceAll(" ", " "))
                .orElseThrow();
        try {
            return objectMapper.readValue(talksString, new TypeReference<>() {});
        } catch (Exception e) {
            log.severe("Error parsing JSON: " + e.getMessage());
            throw new RuntimeException("Error parsing JSON: " + e.getMessage());
        }
    }

    private String getFacts() {
        File sourceFile = c3ntralaService.downloadFile("dane/pliki_z_fabryki.zip", "pliki_z_fabryki.zip");
        return zipService.unzipFile(sourceFile, "category").stream()
                .filter(file -> file.getAbsolutePath().contains("/category/facts/"))
                .filter(file -> file.getName().contains(".txt"))
                .map(File::toPath)
                .map(this::pathToString)
                .map(String::trim)
                .filter(content -> !content.isBlank())
                .map(fakt -> "FAKT:\n" + fakt + "\n")
                .collect(Collectors.joining("\n")).trim();
    }

    private String pathToString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            log.warning("Error reading file: " + e.getMessage());
            return "";
        }
    }
}
