package pl.jit.robotsystem.service.openai.tools.username;

import lombok.extern.java.Log;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaDbService;
import pl.jit.robotsystem.service.openai.tools.Tool;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log
public class UsernameTool implements Tool {

    private static final String QUERY = "SELECT * FROM users WHERE username='%s';";

    private final C3ntralaDbService c3ntralaDbService;

    public UsernameTool(C3ntralaDbService c3ntralaDbService) {
        this.c3ntralaDbService = c3ntralaDbService;
    }

    @Override
    public String getName() {
        return "username";
    }

    @Override
    public String getDescription() {
        return "Informacje o ludziach zapisanych w bazie danych, użyj ADAM zwróci informacje o Adamie, RAFAŁ zwróci informacje o Rafale";
    }

    @Override
    public String execute(String name) {
        List<Map<String, String>> result = c3ntralaDbService.getQuery(String.format(QUERY, name));

        if (result.isEmpty()) {
            return "Nie znaleziono użytkownika " + name + " w bazie danych.";
        }

        if (result.size() == 1) {
            Map<String, String> data = result.getFirst();
            return "Znaleziono użytkownika: " + name + "\n" + formatData(data);
        }

        String all = "Znaleziono " + result.size() + " wystąpienia użytkownika " + name + "\n";
        return all + result.stream().map(this::formatData)
                .collect(Collectors.joining("\n---\n"));
    }

    private String formatData(Map<String, String> data) {
        return data.entrySet().stream()
                .map(entry -> entry.getKey().toUpperCase() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }
}
