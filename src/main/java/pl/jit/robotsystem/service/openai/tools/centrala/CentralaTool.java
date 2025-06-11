package pl.jit.robotsystem.service.openai.tools.centrala;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.openai.tools.Tool;

import java.util.Map;

@Log
public class CentralaTool implements Tool {

    private final C3ntralaService c3ntralaService;
    private final ObjectMapper objectMapper;

    public CentralaTool(C3ntralaService c3ntralaService, ObjectMapper objectMapper) {
        this.c3ntralaService = c3ntralaService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "centrala";
    }

    @Override
    public String getDescription() {
        return "Wysyła odpowiedzi do centrali i zwraca wynik. Użyj: centrala(zadanie,odpowiedzi_json) np. centrala(phone,{\"01\":\"Samuel\",\"02\":\"endpoint\"})";
    }

    @Override
    public String execute(String input) {
        try {
            String[] params = parseParameters(input);
            if (params.length != 2) {
                return "BŁĄD: Podaj zadanie i odpowiedzi JSON. Format: centrala(phone,{\"01\":\"odpowiedź\"})";
            }

            String taskName = params[0].trim();
            String answersJson = params[1].trim();

            // Parse JSON answers
            Map<String, String> answers = parseAnswersJson(answersJson);
            if (answers.isEmpty()) {
                return "BŁĄD: Nieprawidłowy format JSON odpowiedzi";
            }

            log.info("============================================================================================ ");
            log.info("Wysyłam do centrali zadanie: " + taskName);
            log.info("Odpowiedzi: " + answers);

            // Wyślij do centrali
            Map<String, Object> response = c3ntralaService.report(taskName, answers, Map.class)
                    .orElseThrow(() -> new RuntimeException("Brak odpowiedzi z centrali"));

            return formatCentralaResponse(response, answers);

        } catch (Exception e) {
            log.severe("Błąd CentralaTool: " + e.getMessage());
            return "BŁĄD: " + e.getMessage();
        }
    }

    private String[] parseParameters(String input) {
        // Obsługa: "phone,{json}" lub "(phone,{json})"
        String cleaned = input.trim();
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        // Znajdź pierwsze wystąpienie { aby oddzielić task od JSON
        int jsonStart = cleaned.indexOf('{');
        if (jsonStart == -1) {
            return cleaned.split(",", 2);
        }

        String taskPart = cleaned.substring(0, jsonStart);
        String jsonPart = cleaned.substring(jsonStart);

        // Usuń przecinek z końca taskPart
        taskPart = taskPart.replaceAll(",$", "").trim();

        return new String[]{taskPart, jsonPart};
    }

    private Map<String, String> parseAnswersJson(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> answers = objectMapper.readValue(json, Map.class);
            return answers;
        } catch (Exception e) {
            log.warning("Błąd parsowania JSON: " + e.getMessage());
            return Map.of();
        }
    }

    private String formatCentralaResponse(Map<String, Object> response, Map<String, String> sentAnswers) {
        Integer code = (Integer) response.get("code");
        String message = (String) response.get("message");

        StringBuilder result = new StringBuilder();
        result.append("=== ODPOWIEDŹ CENTRALI ===\n");
        result.append("Code: ").append(code).append("\n");
        result.append("Message: ").append(message).append("\n\n");

        if (code != null && code == 0) {
            result.append("✅ SUKCES! ");
            if (message != null && message.contains("FLG:")) {
                result.append("Znaleziono flagę: ").append(message).append("\n");
            } else {
                result.append("Zadanie wykonane poprawnie\n");
            }
        } else {
            result.append("❌ BŁĄD: ");
            if (message != null) {
                // Spróbuj wyciągnąć numer błędnego pytania
                String questionNumber = extractQuestionNumber(message);
                if (questionNumber != null) {
                    String wrongAnswer = sentAnswers.get(questionNumber);
                    result.append("Pytanie ").append(questionNumber)
                            .append(" jest błędne (wysłano: '").append(wrongAnswer).append("')\n");
                } else {
                    result.append(message).append("\n");
                }
            }

            result.append("\nWYSŁANE ODPOWIEDZI:\n");
            sentAnswers.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry ->
                            result.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));
        }

        return result.toString();
    }

    private String extractQuestionNumber(String message) {
        if (message == null) return null;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("question (\\d{2})");
        java.util.regex.Matcher matcher = pattern.matcher(message);

        return matcher.find() ? matcher.group(1) : null;
    }
}

