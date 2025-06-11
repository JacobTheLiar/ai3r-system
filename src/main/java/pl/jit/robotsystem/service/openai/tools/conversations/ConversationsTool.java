package pl.jit.robotsystem.service.openai.tools.conversations;

import lombok.extern.java.Log;
import pl.jit.robotsystem.service.openai.tools.Tool;

import java.util.List;
import java.util.Map;

@Log
public class ConversationsTool implements Tool {

    private final Map<String, List<String>> conversations;

    public ConversationsTool(Map<String, List<String>> conversations) {
        this.conversations = conversations;
    }

    @Override
    public String getName() {
        return "conversations";
    }

    @Override
    public String getDescription() {
        return "Dostęp do rozmów. Użyj: 'list' (lista rozmów), 'rozmowa1' (konkretna rozmowa), 'search:słowo' (wyszukaj)";
    }

    @Override
    public String execute(String query) {
        if ("list".equals(query.trim())) {
            return listAllConversations();
        }

        if (query.startsWith("search:")) {
            String keyword = query.substring(7).trim();
            return searchInConversations(keyword);
        }

        // Konkretna rozmowa: rozmowa1, rozmowa2, etc.
        if (conversations.containsKey(query.trim())) {
            return getConversation(query.trim());
        }

        return "Dostępne opcje: 'list', 'rozmowa1-rozmowa5', 'search:słowo_kluczowe'";
    }

    private String listAllConversations() {
        return "Dostępne rozmowy: " + String.join(", ", conversations.keySet());
    }

    private String getConversation(String conversationId) {
        List<String> lines = conversations.get(conversationId);
        return "=== " + conversationId.toUpperCase() + " ===\n" +
               String.join("\n", lines);
    }

    private String searchInConversations(String keyword) {
        StringBuilder result = new StringBuilder("WYSZUKIWANIE: '" + keyword + "'\n\n");

        conversations.forEach((id, lines) -> {
            List<String> matches = lines.stream()
                    .filter(line -> line.toLowerCase().contains(keyword.toLowerCase()))
                    .toList();

            if (!matches.isEmpty()) {
                result.append("=== ").append(id).append(" ===\n");
                matches.forEach(match -> result.append("- ").append(match).append("\n"));
                result.append("\n");
            }
        });

        return result.toString();
    }
}