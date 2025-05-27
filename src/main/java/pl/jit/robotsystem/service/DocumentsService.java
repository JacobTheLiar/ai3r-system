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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * S03E01 - tag file contents and join
 */
@Service
@Log
public class DocumentsService implements No5Action {

    private final C3ntralaService c3ntralaService;
    private final ZipService zipService;
    private final OpenAiService openAiService;
    private final PromptRepository promptRepository;

    public DocumentsService(C3ntralaService c3ntralaService, ZipService zipService, OpenAiService openAiService, PromptRepository promptRepository) {
        this.c3ntralaService = c3ntralaService;
        this.zipService = zipService;
        this.openAiService = openAiService;
        this.promptRepository = promptRepository;
    }


    public void action() {
        File sourceFile = c3ntralaService.downloadFile("dane/pliki_z_fabryki.zip", "pliki_z_fabryki.zip");

        Set<Set<String>> factTags = zipService.unzipFile(sourceFile, "documents").stream()
                .filter(file -> file.getAbsolutePath().contains("/documents/facts/"))
                .map(this::tagFacts)
                .collect(Collectors.toSet());
        log.info("Collected tags from facts: ");
        factTags.forEach(facts -> log.info(" - " + facts.size()));

        Map<String, Set<String>> reportTags = zipService.unzipFile(sourceFile, "documents").stream()
                .filter(file -> file.getAbsolutePath().matches(".*/documents/\\d{4}-\\d{2}-\\d{2}_report-.*\\.txt"))
                .map(this::tagReport)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));
        log.info("Collected tags from reports: ");
        reportTags.forEach((key, value) -> log.info(" - " + key + ": " + value.size()));

        log.info("Extending report tags with matching fact tags: ");
        Map<String, String> finalReportTags = reportTags.entrySet().stream()
                .peek(entry -> {
                    Set<String> value = entry.getValue();
                    factTags.stream()
                            .filter(factTagSet -> factTagSet.stream().anyMatch(value::contains))
                            .forEach(value::addAll);
                })
                .peek(entry -> log.info(" - " + entry.getKey() + ": " + entry.getValue().size()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.join(",", entry.getValue())
                ));

        c3ntralaService.report("dokumenty", finalReportTags, String.class)
                .ifPresent(response -> {
                    log.info("Response from C3ntrala: " + response);
                    FlagFinder.containsFlag(response);
                });
        log.info("Done!");
    }

    private Set<String> tagFacts(File file) {
        try {
            String tags;
            File mdFile = new File(file.getParentFile(), file.getName() + ".md");
            if (mdFile.exists()) {
                tags = Files.readString(mdFile.toPath());
            } else {
                String fileContent = Files.readString(file.toPath());
                String prompt = promptRepository.getPromptData("s03e01-tag-facts");
                tags = openAiService.getCompletion(prompt, fileContent);
                Files.writeString(mdFile.toPath(), tags);
            }
            return Arrays.stream(tags.split(",")).map(String::trim).collect(Collectors.toSet());
        } catch (IOException e) {
            log.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Map<String, Set<String>> tagReport(File file) {
        try {
            String tags;
            File mdFile = new File(file.getParentFile(), file.getName() + ".md");
            if (mdFile.exists()) {
                tags = Files.readString(mdFile.toPath());
            } else {
                String fileContent = "# " + file.getName() + "\n" + Files.readString(file.toPath());
                String prompt = promptRepository.getPromptData("s03e01-tag-report");
                tags = openAiService.getCompletion(prompt, fileContent);
                Files.writeString(mdFile.toPath(), tags);
            }
            Set<String> tagSet = Arrays.stream(tags.split(",")).map(String::trim).collect(Collectors.toSet());
            return Collections.singletonMap(file.getName(), tagSet);
        } catch (IOException e) {
            log.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
