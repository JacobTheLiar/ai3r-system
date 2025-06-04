package pl.jit.robotsystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.furstenheim.CopyDown;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.prompt.PromptRepository;
import pl.jit.robotsystem.share.No5Action;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * S04E03 - clarify photo, and describe
 */
@Service
@Log
public class SoftoService implements No5Action {

    private final static String MAIN_URL = "https://softo.ag3nts.org/";
    private final static int MAX_HOOPS = 10;

    private final C3ntralaService c3ntralaService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final Map<String, String> pageContent = new HashMap<>();
    private final PromptRepository promptRepository;
    private final CopyDown htmlConverter;
    private final OpenAiService openAiService;


    public SoftoService(C3ntralaService c3ntralaService, ObjectMapper objectMapper, PromptRepository promptRepository, CopyDown htmlConverter, OpenAiService openAiService) {
        this.c3ntralaService = c3ntralaService;
        this.objectMapper = objectMapper;
        this.promptRepository = promptRepository;
        this.htmlConverter = htmlConverter;
        this.openAiService = openAiService;
        this.webClient = WebClient.create();
    }


    public void action() {
        c3ntralaService.getData("softo.json", String.class)
                .map(this::mapSoftoToObject)
                .map(this::answerQuestions)
                .map(this::sendQuestionsToC3ntrala)
                .ifPresent(r -> System.out.println("present " + r));

        log.info("Done!");
    }

    private Map<String, String> mapSoftoToObject(@NotNull String softoString) {
        try {
            return objectMapper.readValue(softoString, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> answerQuestions(@NotNull Map<String, String> questions) {
        questions.replaceAll((key, value) -> answerQuestion(value));
        return questions;
    }

    private String answerQuestion(String question) {
        String currentUrl = MAIN_URL;
        String promptData = promptRepository.getPromptData("s04e03-find-answer-system");
        boolean done = false;
        int hoops = 0;
        String answer = "";

        log.info("====================> answering question: " + question);
        log.info(" - main url: " + MAIN_URL);
        List<List<String>> visitedPaths = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        while (!done) {
            hoops++;
            if (MAIN_URL.equals(currentUrl)) {
                log.info("   -  start new search path...");
                paths = new ArrayList<>();
                visitedPaths.add(paths);
            }

            log.info("     -  current url: " + currentUrl);
            log.info("     -  visited-paths: ");

            visitedPaths.stream()
                    .peek(x -> log.info("        ---"))
                    .forEach(path -> path.forEach(url -> log.info("          - " + url)));


            String page = pageContent.getOrDefault(currentUrl, downloadContent(currentUrl));
            String systemPrompt = promptData
                    .replace("{{question}}", question)
                    .replace("{{main-page}}", MAIN_URL)
                    .replace("{{actual-page}}", currentUrl)
                    .replace("{{visited-paths}}", visitedPaths.toString());


            LlmAnswer completion = openAiService.getCompletion(systemPrompt, page, LlmAnswer.class);

            log.info("     -  LLM's answer: ");
            log.info("        -  thinking: " + completion.thinking);
            log.info("        -  goToLink: " + completion.goToLink);
            log.info("        -  answer  : " + completion.answer);


            if (completion.goToLink == null || completion.goToLink.isBlank()) {
                done = !completion.answer().isBlank();
                if (done) {
                    log.info("====================> found answer for question");
                    log.info(" - question: "+question);
                    log.info(" - answer  : " + completion.answer);
                    log.info("====================<");
                    answer = completion.answer();
                }
            } else {
                paths.add(currentUrl);
                currentUrl = completion.goToLink;
            }

            if (MAX_HOOPS == hoops) {
                log.warning("Maximum number of hoops: " + MAX_HOOPS);
                return "";
            }
        }
        return answer;
    }


    private String downloadContent(String url) {
        log.info("Processing " + url + "....");
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(htmlConverter::convert)
                .filter(Predicate.not(String::isBlank))
                .doOnNext(md -> {
                    log.info(" - done, size: " + md.length());
                    pageContent.put(url, md);
//                    System.out.println("<page-content url=\"" + url + "\">\n"+md+"\n</page-content>");
                })
                .defaultIfEmpty("")
                .block();
    }

    private String sendQuestionsToC3ntrala(@NotNull Map<String, String> answers) {
        try {
            return c3ntralaService.report("softo", answers, String.class)
                    .orElse("");
        } catch (Exception e) {
            log.severe(e.getMessage());
            return "";
        }
    }


    record LlmAnswer(
            String thinking,
            String goToLink,
            String answer
    ) {
    }
}
