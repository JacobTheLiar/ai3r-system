package pl.jit.robotsystem.service;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.share.FlagFinder;
import pl.jit.robotsystem.share.No5Action;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * S04E02 - use the fine-tuned model to solve a problem
 */
@Service
@Log
public class ResearchUseService implements No5Action {

    private final C3ntralaService c3ntralaService;
    private final OpenAiService openAiService;
    private final File workingDirectory;

    public ResearchUseService(C3ntralaService c3ntralaService, OpenAiService openAiService, File workingDirectory) {
        this.c3ntralaService = c3ntralaService;
        this.openAiService = openAiService;
        this.workingDirectory = workingDirectory;
    }

    public void action() {
        log.info("Starting research...");
        File validateFile = new File(workingDirectory, "lab_data/verify.txt");
        List<String> resultToCheck = getFileContents(validateFile)
                .stream()
                .map(this::splitContent)
                .filter(this::chekData)
                .map(DataToCheck::no)
                .toList();

        if (resultToCheck.isEmpty()) {
            log.warning("No data found");
            return;
        }
        try {
            c3ntralaService.report("research", resultToCheck, String.class)
                    .ifPresent(FlagFinder::containsFlag);
            log.info("Done!");
        } catch (Exception e){
            log.severe(e.getMessage());
        }
    }

    private List<String> getFileContents(File file) {
        log.info("Reading file: " + file.getName());
        try {
            return Files.readAllLines(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DataToCheck splitContent(String content) {
        log.info("Splitting: " + content);
        String[] split = content.split("=");
        if (split.length != 2) {
            throw new RuntimeException("Invalid file");
        }
        return new DataToCheck(split[0], split[1]);
    }

    private boolean chekData(DataToCheck data) {
        log.info("Checking data: " + data.toString());
        String result = openAiService.getCompletion("You are a data validator. Return 1 for valid data, 0 for invalid data.", data.data(), "ft:gpt-4o-mini-2024-07-18:jacob-it:research:BeHzzfSx");
        log.info(" - model response: " + result);
        return "1".equals(result);
    }

    record DataToCheck(
            String no,
            String data
    ){}
}
