package pl.jit.robotsystem.service;

import lombok.extern.java.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.prompt.PromptRepository;
import pl.jit.robotsystem.share.FlagFinder;
import pl.jit.robotsystem.share.No5Action;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

/**
 * S01E01 - hack login to XYZ
 */
@Service
@Log
public class HackLoginService implements No5Action {

    private static final String USERNAME = "tester";
    private static final String PASSWORD = "574e112a";

    private final WebClient xyzClient;
    private final OpenAiService aiService;
    private final PromptRepository promptRepository;
    private final File workingDirectory;


    public HackLoginService(@Qualifier("xyzApiClient") WebClient xyzClient, OpenAiService aiService,
                            PromptRepository promptRepository, File workingDirectory) {
        this.xyzClient = xyzClient;
        this.aiService = aiService;
        this.promptRepository = promptRepository;
        this.workingDirectory = workingDirectory;
    }


    public void action() {
        log.info("Open XYZ page");
        String page = xyzClient.get()
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();
        String question = getQuestion(page);
        log.info("Found Question: " + question);

        log.info("getting answer...");
        String prompt = promptRepository.getPromptData("s01e01-hack-login");
        String answer = aiService.getCompletion(prompt, question);
        log.info("got answer: " + answer);

        log.info("Trying login...");

        String loginResponse = xyzClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", USERNAME)
                        .with("password", PASSWORD)
                        .with("answer", answer))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();

        if (loginResponse != null) {
            FlagFinder.containsFlag(loginResponse);
            File outputFile = new File(workingDirectory, "login-xyz-response.html");
            try {
                Files.writeString(outputFile.toPath(), loginResponse, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                log.severe(e.getMessage());
            }
        }
        log.info("Done!");
    }


    private String getQuestion(String page) {
        Document document = Jsoup.parse(page);
        Element questionElement = document.getElementById("human-question");
        String fullText = questionElement!= null ? questionElement.text() : "";
        if (fullText.startsWith("Question:")) {
            return fullText.substring("Question:".length()).trim();
        }
        return "69";
    }
}