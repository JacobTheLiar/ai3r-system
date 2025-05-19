package pl.jit.robotsystem.service;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.prompt.PromptRepository;
import pl.jit.robotsystem.service.zip.ZipService;
import pl.jit.robotsystem.share.FlagFinder;
import pl.jit.robotsystem.share.No5Action;
import pl.jit.robotsystem.share.ThinkingRemover;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * S02E01 - analyze voice - find a place from mp3
 */
@Service
@Log
public class VoiceService implements No5Action {

    private final C3ntralaService c3ntralaService;
    private final ZipService zipService;
    private final OpenAiService openAiService;
    private final PromptRepository promptRepository;

    public VoiceService(C3ntralaService c3ntralaService, ZipService zipService, OpenAiService openAiService, PromptRepository promptRepository) {
        this.c3ntralaService = c3ntralaService;
        this.zipService = zipService;
        this.openAiService = openAiService;
        this.promptRepository = promptRepository;
    }


    public void action() {
        log.info("Downloading voice from %s...".formatted("c3ntrala"));

        File data = c3ntralaService.downloadFile("dane/przesluchania.zip", "przesluchania.zip");
        List<File> voiceList = zipService.unzipFile(data);

        String allTranscriptions = voiceList.stream()
                .map(this::getTranscription)
                .collect(Collectors.joining("\n\n"));
        log.info("Collected all transcriptions:\n\n"+allTranscriptions);

        log.info("Analyze transcriptions to get exact location");
        String prompt = promptRepository.getPromptData("s02e01-detective");
        String response = openAiService.getCompletion(prompt, allTranscriptions);
        log.info("Response from AI: " + response);

        log.info("cleaning data...");
        response = ThinkingRemover.removeThinkingProcess(response);
        log.info(" - cleaned: "+response);

        log.info("sending response to c3ntrala...");
        c3ntralaService.report("mp3", response, String.class)
                .ifPresent(FlagFinder::containsFlag);

        log.info("done!");
    }

    private String getTranscription(File file) {
        Path transcriptionPath = getTranscriptionPath(file);
        return Optional.of(transcriptionPath)
                .filter(Files::exists)
                .map(this::readTranscriptionFile)
                .orElseGet(() -> transcribeAndSave(file, transcriptionPath));
    }

    private Path getTranscriptionPath(File audioFile) {
        String fileName = audioFile.getName();
        String baseName = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;
        return audioFile.toPath().getParent().resolve(baseName + ".md");
    }

    private String readTranscriptionFile(Path transcriptionPath) {
        try {
            log.info("Reading existing transcription: " + transcriptionPath);
            return Files.readString(transcriptionPath);
        } catch (IOException e) {
            log.warning("Cannot read transcription file: " + e.getMessage());
            return null;
        }
    }

    private String transcribeAndSave(File audioFile, Path transcriptionPath) {
        try {
            log.info("Transcribing file: " + audioFile.getName());

            String fileName = audioFile.getName();
            String baseName = fileName.contains(".")
                    ? fileName.substring(0, fileName.lastIndexOf('.'))
                    : fileName;

            String transcription = openAiService.transcribeAudio(audioFile.toPath());

            String fullContent = String.format("# Transkrypcja przesłuchania %s\n%s",
                    baseName, transcription);
            Files.writeString(transcriptionPath, fullContent);
            log.info("Transcription saved to: " + transcriptionPath);
            return fullContent;
        } catch (Exception e) {
            log.severe("Transcription error: " + e.getMessage());
            throw new RuntimeException("Failed to transcribe", e);
        }
    }
}