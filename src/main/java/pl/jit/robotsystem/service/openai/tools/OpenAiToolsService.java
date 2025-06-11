package pl.jit.robotsystem.service.openai.tools;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.service.openai.Message;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.openai.StringMessage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static pl.jit.robotsystem.service.openai.OpenAiService.GPT_4_1_MINI;

@SuppressWarnings("rawtypes")
@Service
@Log
public class OpenAiToolsService {

    private final OpenAiService openAiService;
    private final File workingDirectory;

    public OpenAiToolsService(OpenAiService openAiService, File workingDirectory) {
        this.openAiService = openAiService;
        this.workingDirectory = workingDirectory;
    }

    public String getCompletionWithTools(String systemPrompt, String userMessage, List<Tool> availableTools) {
        return getCompletionWithTools(systemPrompt, userMessage, availableTools, GPT_4_1_MINI);
    }

    public String getCompletionWithTools(String systemPrompt, String userMessage,
                                         List<Tool> availableTools, String modelName) {
        log.info("=== ROZPOCZYNANIE SESJI Z NARZĘDZIAMI ===");
        log.info("Model: " + modelName);
        log.info("Liczba dostępnych narzędzi: " + availableTools.size());
        availableTools.forEach(tool -> log.info("- Narzędzie: " + tool.getName() + " (" + tool.getDescription() + ")"));
        log.info("Pytanie użytkownika: " + truncateForLog(userMessage, 200));

        List<Message> conversationHistory = new ArrayList<>();
        String fullSystemPrompt = buildSystemPromptWithTools(systemPrompt, availableTools);

        conversationHistory.add(StringMessage.builder()
                .role("system")
                .content(fullSystemPrompt)
                .build());
        conversationHistory.add(StringMessage.builder()
                .role("user")
                .content(userMessage)
                .build());

        log.info("Prompt systemowy przygotowany (długość: " + fullSystemPrompt.length() + " znaków)");

        int maxIterations = 100;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            log.info("\n--- ITERACJA " + (iteration + 1) + "/" + maxIterations + " ---");

            log.info("Wysyłanie zapytania do AI...");
            String response = getCompletion(conversationHistory, modelName);
            log.info("Otrzymano odpowiedź AI (długość: " + response.length() + " znaków)");
            log.info("Odpowiedź AI: " + truncateForLog(response, 500));

            List<ToolCall> toolCalls = extractToolCalls(response);
            log.info("Znaleziono wywołań narzędzi: " + toolCalls.size());

            if (toolCalls.isEmpty()) {
                log.info("Brak wywołań narzędzi - kończymy z finalną odpowiedzią");
                log.info("=== SESJA ZAKOŃCZONA POMYŚLNIE ===");
                conversationHistory.add(StringMessage.builder()
                        .role("assistant")
                        .content(response)
                        .build());
                storeConversation(conversationHistory);
                return response;
            }

            toolCalls.forEach(call ->
                    log.info("Wykryto wywołanie: " + call.toolName() + " z parametrem: " + truncateForLog(call.query(), 100)));

            conversationHistory.add(StringMessage.builder()
                    .role("assistant")
                    .content(response)
                    .build());

            for (int toolIndex = 0; toolIndex < toolCalls.size(); toolIndex++) {
                ToolCall toolCall = toolCalls.get(toolIndex);
                log.info("\n>> Wykonywanie narzędzia " + (toolIndex + 1) + "/" + toolCalls.size() +
                         ": " + toolCall.toolName() + "(" + truncateForLog(toolCall.query(), 50) + ")");

                long startTime = System.currentTimeMillis();
                String toolResult = executeTool(toolCall.toolName(), toolCall.query(), availableTools);
                long executionTime = System.currentTimeMillis() - startTime;

                log.info("Narzędzie " + toolCall.toolName() + " wykonane w " + executionTime + "ms");
                log.info("Wynik narzędzia (długość: " + toolResult.length() + " znaków): " +
                         truncateForLog(toolResult, 300));

                conversationHistory.add(StringMessage.builder()
                        .role("user")
                        .content("WYNIK " + toolCall.toolName() + ": " + toolResult)
                        .build());
            }

            log.info("Historia konwersacji ma teraz " + conversationHistory.size() + " wiadomości");
        }

        log.warning("=== OSIĄGNIĘTO LIMIT ITERACJI (" + maxIterations + ") ===");
        storeConversation(conversationHistory);
        return "Przekroczono limit iteracji";
    }

    private String getCompletion(List<Message> messages, String modelName) {
        log.info("Wywołanie OpenAI API z " + messages.size() + " wiadomościami");
        try {
            String result = openAiService.getCompletion(messages, modelName);
            log.info("OpenAI API odpowiedziało pomyślnie");
            return result;
        } catch (Exception e) {
            log.severe("Błąd wywołania OpenAI API: " + e.getMessage());
            throw e;
        }
    }

    private List<ToolCall> extractToolCalls(String response) {
        Pattern toolPattern = Pattern.compile("\\[TOOL:(\\w+):([^]]+)]");
        Matcher matcher = toolPattern.matcher(response);

        List<ToolCall> toolCalls = new ArrayList<>();
        while (matcher.find()) {
            toolCalls.add(new ToolCall(matcher.group(1), matcher.group(2)));
        }

        if (!toolCalls.isEmpty()) {
            log.info("Wyodrębniono wywołania narzędzi: " +
                     toolCalls.stream()
                             .map(tc -> tc.toolName() + "(" + truncateForLog(tc.query(), 30) + ")")
                             .collect(Collectors.joining(", ")));
        }

        return toolCalls;
    }

    private String executeTool(String toolName, String query, List<Tool> availableTools) {
        log.info("Szukanie narzędzia: " + toolName);

        return availableTools.stream()
                .filter(tool -> {
                    boolean matches = tool.getName().equalsIgnoreCase(toolName);
                    if (matches) {
                        log.info("Znaleziono narzędzie: " + tool.getName() + " - wykonywanie...");
                    }
                    return matches;
                })
                .findFirst()
                .map(tool -> {
                    try {
                        String result = tool.execute(query);
                        log.info("Narzędzie " + toolName + " wykonane pomyślnie");
                        return result;
                    } catch (Exception e) {
                        log.severe("Błąd wykonania narzędzia " + toolName + ": " + e.getMessage());
                        return "Błąd wykonania narzędzia " + toolName + ": " + e.getMessage();
                    }
                })
                .orElseGet(() -> {
                    log.warning("Narzędzie " + toolName + " nie zostało znalezione. Dostępne: " +
                                availableTools.stream().map(Tool::getName).collect(Collectors.joining(", ")));
                    return "Narzędzie " + toolName + " nie jest dostępne w tym kontekście";
                });
    }

    private String buildSystemPromptWithTools(String originalPrompt, List<Tool> availableTools) {
        if (availableTools.isEmpty()) {
            log.info("Brak narzędzi - używamy oryginalnego prompta");
            return originalPrompt;
        }

        String toolsDescription = availableTools.stream()
                .map(t -> "- " + t.getName() + ": " + t.getDescription())
                .collect(Collectors.joining("\n"));

        String fullPrompt = originalPrompt + "\n\nDOSTĘPNE NARZĘDZIA:\n" + toolsDescription +
                            "\n\nAby użyć narzędzia, napisz: [TOOL:nazwa:zapytanie]";

        log.info("Prompt systemowy rozszerzony o " + availableTools.size() + " narzędzi");
        return fullPrompt;
    }

    private String truncateForLog(String text, int maxLength) {
        if (text == null) return "null";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "... [obcięto " + (text.length() - maxLength) + " znaków]";
    }


    public void storeConversation(List<Message> conversationHistory) {
        String content = conversationHistory.stream()
                .map(message ->
                        "# ROLE: " + message.role() + "\n"
                        + message.content() + "\n"
                ).collect(Collectors.joining("\n")).trim();

        // Uzyskanie aktualnej daty i czasu
        String dateFolder = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String timeStamp = new SimpleDateFormat("HH-mm-ss-SSS").format(new Date());

        // Tworzenie podkatalogu z datą
        File dateDir = new File(workingDirectory, dateFolder);
        if (!dateDir.exists()) {
            dateDir.mkdirs(); // Tworzy katalog, jeśli nie istnieje
        }

        // Tworzenie pliku z czasem
        File file = new File(dateDir, timeStamp + ".md");
        try {
            Files.writeString(file.toPath(), content);
        } catch (IOException e) {
            e.printStackTrace(); // Obsługa błędów
        }
    }
}