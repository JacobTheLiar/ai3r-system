package pl.jit.robotsystem.service.openai.tools.facts;

import lombok.extern.java.Log;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.openai.tools.Tool;

@Log
public class FactsTool implements Tool {

    private final OpenAiService openAiService;
    private final String factsDatabase;

    public FactsTool(OpenAiService openAiService, String factsData) {
        this.openAiService = openAiService;
        this.factsDatabase = factsData;
    }

    @Override
    public String getName() { return "facts"; }

    @Override
    public String getDescription() {
        return "Fakty o postaciach/lokacjach. Użyj: 'list' (lista tematów), 'Adam' (fakty o osobie), 'Sektor D' (o lokacji)";
    }

    @Override
    public String execute(String query) {
        if ("list".equals(query.trim())) {
            return getAvailableTopics();
        }

        return getSpecificFacts(query);
    }

    private String getAvailableTopics() {
        String prompt = """
            Przeanalizuj bazę faktów i wypisz wszystkie dostępne tematy/postacie/lokacje.
            Zwróć tylko listę nazw oddzielonych przecinkami.
            
            Przykład odpowiedzi: Adam Gospodarczyk, Barbara Zawadzka, Sektor A, Sektor B, Aleksander Ragowski
            
            FAKTY: %s
            """.formatted(factsDatabase);

        return openAiService.getCompletion("system", prompt, "gpt-3.5-turbo");
    }

    private String getSpecificFacts(String topic) {
        String prompt = """
            Znajdź w bazie faktów wszystkie informacje dotyczące: %s
            
            Zwróć tylko te fakty które bezpośrednio dotyczą tematu.
            Formatuj jako listę punktorów.
            
            FAKTY: %s
            """.formatted(topic, factsDatabase);

        return openAiService.getCompletion("system", prompt, "gpt-3.5-turbo");
    }
}
