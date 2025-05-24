package pl.jit.robotsystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import lombok.extern.java.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.common.PlaceholderProcessorService;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.prompt.PromptRepository;
import pl.jit.robotsystem.share.FlagFinder;
import pl.jit.robotsystem.share.No5Action;
import pl.jit.robotsystem.share.ThinkingRemover;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;


/**
 * S02E05 - analyze html with media
 */
@Service
@Log
public class ArxivService implements No5Action {

    private final C3ntralaService c3ntralaService;
    private final OpenAiService openAiService;
    private final PromptRepository promptRepository;
    private final FlexmarkHtmlConverter converter;
    private final PlaceholderProcessorService placeholderProcessorService;
    private final ObjectMapper objectMapper;

    public ArxivService(C3ntralaService c3ntralaService, OpenAiService openAiService, PromptRepository promptRepository, FlexmarkHtmlConverter converter, PlaceholderProcessorService placeholderProcessorService, ObjectMapper objectMapper) {
        this.c3ntralaService = c3ntralaService;
        this.openAiService = openAiService;
        this.promptRepository = promptRepository;
        this.converter = converter;
        this.placeholderProcessorService = placeholderProcessorService;
        this.objectMapper = objectMapper;
    }


    public void action() {
        File draft = c3ntralaService.downloadFile("dane/arxiv-draft.html", "arxiv/arxiv-draft.html");

        String draftHtml;

        try {
            draftHtml = Files.readString(draft.toPath());
        } catch (IOException e) {
            log.log(Level.SEVERE, "error reading arxiv draft", e);
            throw new RuntimeException("Cannot read file", e);
        }

        log.info("preparing html...");
        String draftParsed = prepareHtml(draftHtml);

        log.info("converting to MarkDown...");
        String draftMd = converter.convert(draftParsed);

        log.info("generating picture descriptions...");
        draftMd = placeholderProcessorService.processPictures(draftMd, (src, caption) -> {
            log.info("Found placeholder voice");
            log.info(" - downloading file: " + src);
            File picture = c3ntralaService.downloadFile("dane/" + src, "arxiv/" + src);
            log.info(" - transcribe file");
            String description = getPictureDescription(picture);
            log.info(" - done: " + description);
            return promptRepository.getPromptData("s02e05-picture")
                    .replace("{{source}}", src)
                    .replace("{{caption}}", caption)
                    .replace("{{description}}", description);
        });

        log.info("generating transcriptions ...");
        draftMd = placeholderProcessorService.processVoices(draftMd, src -> {
            log.info("Found placeholder voice");
            log.info(" - downloading file: " + src);
            File voice = c3ntralaService.downloadFile("dane/" + src, "arxiv/" + src);
            log.info(" - transcribe file");
            String transcription = getTranscription(voice);
            log.info(" - done: " + transcription);
            return promptRepository.getPromptData("s02e05-voice")
                    .replace("{{source}}", src)
                    .replace("{{transcription}}", transcription);
        });
        log.info("prepared\n\n"+draftMd);
        log.info("getting questions from c3ntrala...\n");

        final String markdown = draftMd;

        c3ntralaService.getData("arxiv.txt", String.class)
                .ifPresent(questions -> {
                    log.info("got questions questions:\n"+questions);
                    String systemPrompt = promptRepository.getPromptData("s02e05-answer-system");
                    String askPrompt = promptRepository.getPromptData("s02e05-answer-ask")
                            .replace("{{article}}", markdown)
                            .replace("{{questions}}", questions);

                    String aiResponse = openAiService.getCompletion(systemPrompt, askPrompt);
                    aiResponse = ThinkingRemover.removeThinkingProcess(aiResponse).trim();
                    log.info("got ai response:\n"+aiResponse);

                    log.info("sending response to c3ntrala...");
                    try {
                        Map<String, String> answer = objectMapper.readValue(aiResponse, new TypeReference<>(){});
                        c3ntralaService.report("arxiv", answer, String.class)
                                .ifPresent(response -> {
                                    log.info("got response from c3ntrala:\n"+response);
                                    FlagFinder.containsFlag(response);
                                });
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
        log.info("Done!");
    }

    private String prepareHtml(String html) {
        Document doc = Jsoup.parse(html);
        doc.select("figure").forEach(figure -> Optional.ofNullable(figure.selectFirst("img"))
                .ifPresent(img -> {
                    String src = img.attr("src");
                    String description = Optional.ofNullable(figure.selectFirst("figcaption"))
                            .map(Element::text)
                            .orElse("");

                    figure.replaceWith(new Element("placeholder")
                            .text("{{ picture='" + src + "', description='" + description.replace("'", "\"") + "' }}"));
                }));
        doc.select("a[href$=.mp3]").forEach(link ->
                link.replaceWith(new Element("placeholder")
                        .text("{{ voice='" + link.attr("href") + "' }}"))
        );
        doc.select("audio").remove(); // doc.select("audio[controls]").remove();
        doc.select("head").remove();

        return doc.html();
    }


    private String getTranscription(File file) {
        Path transcriptionPath = file.toPath().getParent().resolve(file.getName() + ".md");
        return Optional.of(transcriptionPath)
                .filter(Files::exists)
                .map(this::readTranscriptionFile)
                .orElseGet(() -> transcribeAndSave(file, transcriptionPath));
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
            String transcription = openAiService.transcribeAudio(audioFile.toPath());
            Files.writeString(transcriptionPath, transcription);
            log.info("Transcription saved to: " + transcriptionPath);
            return transcription;
        } catch (Exception e) {
            log.severe("Transcription error: " + e.getMessage());
            throw new RuntimeException("Failed to transcribe", e);
        }
    }

    private String getPictureDescription(File file) {
        Path transcriptionPath = file.toPath().getParent().resolve(file.getName() + ".md");
        return Optional.of(transcriptionPath)
                .filter(Files::exists)
                .map(this::readPictureDescriptionFile)
                .orElseGet(() -> describePictureAndSave(file, transcriptionPath));
    }

    private String readPictureDescriptionFile(Path transcriptionPath) {
        try {
            log.info("Reading existing description: " + transcriptionPath);
            return Files.readString(transcriptionPath);
        } catch (IOException e) {
            log.warning("Cannot read description file: " + e.getMessage());
            return null;
        }
    }

    private String describePictureAndSave(File pictureFile, Path transcriptionPath) {
        try {
            String prompt = promptRepository.getPromptData("s02e05-describe-picture");
            String transcription = openAiService.workWithImage(prompt, pictureFile);
            Files.writeString(transcriptionPath, transcription);
            log.info("Description saved to: " + transcriptionPath);
            return transcription;
        } catch (Exception e) {
            log.severe("Description error: " + e.getMessage());
            throw new RuntimeException("Failed to describe", e);
        }
    }
}
