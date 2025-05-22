package pl.jit.robotsystem.service;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.prompt.PromptRepository;
import pl.jit.robotsystem.service.zip.ZipService;
import pl.jit.robotsystem.share.FlagFinder;
import pl.jit.robotsystem.share.No5Action;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log
public class CategoryService implements No5Action {

    private final C3ntralaService c3ntralaService;
    private final ZipService zipService;
    private final OpenAiService openAiService;
    private final PromptRepository promptRepository;

    public CategoryService(C3ntralaService c3ntralaService, ZipService zipService, OpenAiService openAiService, PromptRepository promptRepository) {
        this.c3ntralaService = c3ntralaService;
        this.zipService = zipService;
        this.openAiService = openAiService;
        this.promptRepository = promptRepository;
    }


    public void action() {
        File sourceFile = c3ntralaService.downloadFile("dane/pliki_z_fabryki.zip", "pliki_z_fabryki.zip");

        String askData = zipService.unzipFile(sourceFile, "category").stream()
                .filter(file -> !file.getAbsolutePath().contains("/category/facts/"))
                .filter(file -> !file.getName().equals("weapons_tests.zip"))
                .filter(file -> file.getName().contains("."))
                .map(this::prepareData)
                .filter(content -> !content.isBlank())
                .collect(Collectors.joining("\n"));

        String prompt = promptRepository.getPromptData("s02e04-find-answer");
        CategoryAnswer response = openAiService.getCompletion(prompt, askData, CategoryAnswer.class);
        log.info("Response from AI: " + response);

        Optional<String> reported = c3ntralaService.report("kategorie", response, String.class);
        reported.ifPresent(FlagFinder::containsFlag);

        log.info("Done!");
    }

    private String prepareData(File file) {
        String fileName = file.getName();
        File preparedFile = new File(file.getAbsolutePath() + ".md");
        Path outputPath = Paths.get(preparedFile.getAbsolutePath());
        String content = "";

        if (!preparedFile.exists()) {
            if (fileName.endsWith(".png")) {
                content = picTextToString(file);
            } else if (fileName.endsWith(".mp3")) {
                content = voiceToString(file);
            } else if (fileName.endsWith(".txt")) {
                try {
                    content = Files.readString(Paths.get(file.getAbsolutePath()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (!content.isBlank()) {
                try {
                    Files.write(outputPath, content.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            try {
                content = Files.readString(outputPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (content.isBlank()) {
            return "";
        }
        return promptRepository.getPromptData("s02e04-file-template")
                .replace("{{file-name}}", fileName)
                .replace("{{content}}", content.trim());
    }

    private String voiceToString(File file) {
        try {
            return openAiService.transcribeAudio(file.toPath());
        } catch (Exception e) {
            log.severe("Transcription error: " + e.getMessage());
            throw new RuntimeException("Failed to transcribe", e);
        }
    }

    private String picTextToString(File file) {
        try {
            log.info("Transcribing file: " + file.getName());
            String prompt = promptRepository.getPromptData("s02e04-image");
            return openAiService.workWithImage(prompt, file);
        } catch (Exception e) {
            log.severe("Transcription error: " + e.getMessage());
            throw new RuntimeException("Failed to transcribe", e);
        }
    }

    record CategoryAnswer(
            List<String> people,
            List<String> hardware
    ) {
    }
}
