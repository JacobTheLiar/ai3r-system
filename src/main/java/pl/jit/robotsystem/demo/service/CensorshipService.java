package pl.jit.robotsystem.demo.service;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.demo.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.demo.service.c3ntrala.EmptyResponseFromC3ntralaException;
import pl.jit.robotsystem.demo.service.ollama.OllamaService;
import pl.jit.robotsystem.demo.service.prompt.PromptRepository;
import pl.jit.robotsystem.demo.share.FlagFinder;
import pl.jit.robotsystem.demo.share.No5Action;


/**
 * S01E05 - get and anonymize personal data using ollama and send back
 */
@SuppressWarnings("FieldCanBeLocal")
@Service
@Log
public class CensorshipService implements No5Action {

    private final OllamaService aiService;
    private final C3ntralaService c3ntrala;
    private final PromptRepository promptRepository;

    public CensorshipService(OllamaService aiService, C3ntralaService c3ntrala, PromptRepository promptRepository) {
        this.aiService = aiService;
        this.c3ntrala = c3ntrala;
        this.promptRepository = promptRepository;
    }

    public void action() {
        String originalData = c3ntrala.getData("cenzura.txt", String.class)
                .orElseThrow(EmptyResponseFromC3ntralaException::new);

        log.info("Censoring text...");
        String prompt = promptRepository.getPromptData("s01e05-censor");
        String censored = aiService.getCompletion(prompt, originalData);
        log.info("Censored data: " + censored);

        String response = c3ntrala.report("CENZURA", censored, String.class)
                .orElseThrow(EmptyResponseFromC3ntralaException::new);
        FlagFinder.containsFlag(response);

        log.info("Done!");
    }
}
