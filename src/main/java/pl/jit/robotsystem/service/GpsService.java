package pl.jit.robotsystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaDbService;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.openai.tools.OpenAiToolsService;
import pl.jit.robotsystem.service.openai.tools.Tool;
import pl.jit.robotsystem.service.openai.tools.centrala.CentralaTool;
import pl.jit.robotsystem.service.openai.tools.gps.GpsTool;
import pl.jit.robotsystem.service.openai.tools.places.PlacesTool;
import pl.jit.robotsystem.service.openai.tools.username.UsernameTool;
import pl.jit.robotsystem.service.prompt.PromptRepository;
import pl.jit.robotsystem.share.No5Action;

import java.util.List;

import static pl.jit.robotsystem.configuration.DotenvConfiguration.AI3R_API_KEY;


/**
 * S05E02 - find gps coordinates
 */
@Service
@Log
public class GpsService implements No5Action {


    private final PromptRepository promptRepository;
    private final OpenAiToolsService openAiToolsService;
    private final WebClient centralaWebClient;
    private final C3ntralaDbService c3ntralaDbService;
    private final C3ntralaService c3ntralaService;
    private final ObjectMapper objectMapper;
    private final Dotenv dotenv;

    public GpsService(PromptRepository promptRepository, OpenAiToolsService openAiToolsService, @Qualifier("c3ntralaApiClient") WebClient centralaWebClient, C3ntralaDbService c3ntralaDbService, C3ntralaService c3ntralaService, ObjectMapper objectMapper, Dotenv dotenv) {
        this.promptRepository = promptRepository;
        this.openAiToolsService = openAiToolsService;
        this.centralaWebClient = centralaWebClient;
        this.c3ntralaDbService = c3ntralaDbService;
        this.c3ntralaService = c3ntralaService;
        this.objectMapper = objectMapper;
        this.dotenv = dotenv;
    }

    public void action() {
        log.info("Getting system prompt...");
        String promptData = promptRepository.getPromptData("s05e02-detective");

        log.info("Preparing tools...");
        List<Tool> tools = List.of(
                new PlacesTool(centralaWebClient, dotenv.get(AI3R_API_KEY)),
                new UsernameTool(c3ntralaDbService),
                new GpsTool(centralaWebClient),
                new CentralaTool(c3ntralaService, objectMapper)
        );

        log.info("Getting question...");
        String question = c3ntralaService.getData("gps_question.json", Question.class)
                .map(Question::question)
                .orElseThrow();

        String llmResponse = openAiToolsService.getCompletionWithTools(promptData, question, tools);

        log.info("LLM response: " + llmResponse);
        log.info("Done!");
    }

    private record Question(String question){}
}
