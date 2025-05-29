package pl.jit.robotsystem.service;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.prompt.PromptRepository;
import pl.jit.robotsystem.share.FlagFinder;
import pl.jit.robotsystem.share.No5Action;

import java.util.*;
import java.util.logging.Level;

import static pl.jit.robotsystem.configuration.DotenvConfiguration.AI3R_API_KEY;

@Service
@Log
public class LoopService implements No5Action {

    private static final String RESTRICTED_KEYWORD = "RESTRICTED";
    private static final String HTTP_KEYWORD = "http";
    private static final String TARGET_PERSON = "BARBARA";
    private static final String PLACES_ENDPOINT = "/places";
    private static final String PEOPLE_ENDPOINT = "/people";

    private final C3ntralaService c3ntralaService;
    private final PromptRepository promptRepository;
    private final OpenAiService openAiService;
    private final String ai3rApiKey;

    public LoopService(C3ntralaService c3ntralaService, PromptRepository promptRepository,
                       OpenAiService openAiService, Dotenv dotenv) {
        this.c3ntralaService = c3ntralaService;
        this.promptRepository = promptRepository;
        this.openAiService = openAiService;
        this.ai3rApiKey = dotenv.get(AI3R_API_KEY);
    }

    public void action() {
        Optional<String> rawData = c3ntralaService.getPublicData("barbara.txt", String.class);
        if (rawData.isEmpty()) {
            log.info("No data found");
            return;
        }

        Data initialData = extractInitialData(rawData.get());
        log.info("Extracted initial data: " + initialData.toString());

        buildCompleteGraph(initialData);
        log.info("Graph building completed!");
    }

    private Data extractInitialData(String rawData) {
        String prompt = promptRepository.getPromptData("s03e04-extract-people-and-cities");
        return openAiService.getCompletion(prompt, rawData, Data.class);
    }

    private void buildCompleteGraph(Data data) {
        Set<String> visited = new HashSet<>();
        Queue<String> toProcess = initializeQueue(data);

        while (!toProcess.isEmpty()) {
            String node = toProcess.poll();

            if (!isValidNode(node, visited)) {
                continue;
            }

            visited.add(node);
            processNode(node, data, visited, toProcess);
        }
    }

    private Queue<String> initializeQueue(Data data) {
        Queue<String> queue = new LinkedList<>();
        queue.addAll(data.cities());
        queue.addAll(data.peoples());
        return queue;
    }

    private boolean isValidNode(String node, Set<String> visited) {
        return !visited.contains(node) && !node.contains(RESTRICTED_KEYWORD);
    }

    private void processNode(String node, Data data, Set<String> visited, Queue<String> toProcess) {
        boolean isCity = data.cities().contains(node);

        Optional<Response> response = queryApi(node, isCity);
        if (response.isEmpty()) {
            return;
        }

        Response resp = response.get();
        logResponse(resp);

        if (!isValidResponse(resp)) {
            log.info("Skipping invalid response: " + resp.message());
            return;
        }

        List<String> newNodes = parseNewNodes(resp.message(), visited);
        addNodesToData(newNodes, data, isCity);

        if (resp.code().equals(0)) {
            toProcess.addAll(newNodes);
            log.info("Added to queue: " + newNodes);
        }

        checkForTarget(resp.message(), node);
    }

    private Optional<Response> queryApi(String node, boolean isCity) {
        Request request = new Request(ai3rApiKey, node);
        String endpoint = isCity ? PLACES_ENDPOINT : PEOPLE_ENDPOINT;

        try {
            return c3ntralaService.postSpecialData(endpoint, request, Response.class);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error querying API for node: " + node + ", " + e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isValidResponse(Response response) {
        String message = response.message();
        return !message.contains(RESTRICTED_KEYWORD) && !message.contains(HTTP_KEYWORD);
    }

    private List<String> parseNewNodes(String responseMessage, Set<String> visited) {
        return Arrays.stream(responseMessage.split(" "))
                .filter(node -> !visited.contains(node))
                .toList();
    }

    private void addNodesToData(List<String> newNodes, Data data, boolean sourceIsCity) {
        if (sourceIsCity) {
            data.peoples().addAll(newNodes);
        } else {
            data.cities().addAll(newNodes);
        }
    }

    private void checkForTarget(String responseMessage, String currentNode) {
        if (responseMessage.contains(TARGET_PERSON)) {
            reportTarget(currentNode);
        }
    }

    private void reportTarget(String location) {
        try {
            c3ntralaService.report("loop", location, String.class)
                    .ifPresent(FlagFinder::containsFlag);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error reporting target location: " + e.getMessage());
        }
    }

    private void logResponse(Response response) {
        log.info("API Response - Code: " + response.code() + ", Message: " + response.message());
    }

    record Data(Set<String> peoples, Set<String> cities) {
    }

    record Request(String apikey, String query) {
    }

    record Response(Integer code, String message) {
    }
}
