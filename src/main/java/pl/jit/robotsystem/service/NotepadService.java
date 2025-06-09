package pl.jit.robotsystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.pdf.PDFService;
import pl.jit.robotsystem.service.prompt.PromptRepository;
import pl.jit.robotsystem.share.FlagFinder;
import pl.jit.robotsystem.share.No5Action;
import pl.jit.robotsystem.share.ThinkingRemover;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * S04E05 - read PDF with images and
 */
@Service
@Log
public class NotepadService implements No5Action {

    private final C3ntralaService c3ntralaService;
    private final ObjectMapper objectMapper;
    private final PromptRepository promptRepository;
    private final OpenAiService openAiService;
    private final PDFService pdfService;


    public NotepadService(C3ntralaService c3ntralaService, ObjectMapper objectMapper, PromptRepository promptRepository, OpenAiService openAiService, PDFService pdfService) {
        this.c3ntralaService = c3ntralaService;
        this.objectMapper = objectMapper;
        this.promptRepository = promptRepository;
        this.openAiService = openAiService;
        this.pdfService = pdfService;
    }


    public void action() {
        String notatka = Optional.ofNullable(c3ntralaService.downloadFile("dane/notatnik-rafala.pdf", "notepad/notatnik-rafala.pdf"))
                .map(this::processPDF)
                .orElseThrow();

        c3ntralaService.getData("notes.json", String.class)
                .map(this::mapSoftoToObject)
                .map((Map<String, String> questions) -> answerQuestions(questions, notatka))
                .map(this::sendQuestionsToC3ntrala)
                .ifPresent(FlagFinder::containsFlag);

        log.info("Done!");
    }

    private String processPDF(File pdfFile) {
        return getCachedText(pdfFile)
                .orElseGet(() -> processAndCache(pdfFile));
    }

    private Optional<String> getCachedText(File pdfFile) {
        String textFilename = pdfFile.getName().replaceAll("\\.pdf$", ".txt");
        File textFile = new File(pdfFile.getParentFile(), textFilename);

        if (!textFile.exists()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Files.readString(textFile.toPath()));
        } catch (IOException e) {
            log.warning("Could not read text file " + textFile.getName());
            return Optional.empty();
        }
    }

    private String processAndCache(File pdfFile) {
        String notatnikTxt = IntStream.range(1, pdfService.pageCount(pdfFile))
                .mapToObj(pageNo -> {
                    String textFromPage = pdfService.getTextFromPage(pdfFile, pageNo);
                    return "# Strona nr " + pageNo + "\n" + textFromPage + "\n";
                })
                .collect(Collectors.joining("\n"));

        File imageFromPage = pdfService.getImageFromPage(pdfFile, 19, "notatnik-rafala-strona-19.png");
        String prompt = promptRepository.getPromptData("s04e05-decode-notes");
        String lastPageNotes = openAiService.workWithImage(prompt, imageFromPage);
        notatnikTxt = notatnikTxt + "\n\n# Strona nr 19\n" + lastPageNotes;

        String textFilename = pdfFile.getName().replaceAll("\\.pdf$", ".txt");
        File textFile = new File(pdfFile.getParentFile(), textFilename);

        try {
            Files.writeString(textFile.toPath(), notatnikTxt);
        } catch (IOException e) {
            log.warning("Could not write text file " + textFile.getName());
        }

        return notatnikTxt;
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

    private Map<String, String> answerQuestions(@NotNull Map<String, String> questions, String note) {
        Map<String, String> answers = questions.keySet().stream()
                .collect(Collectors.toMap(t -> t, t -> ""));

        String promptData = promptRepository.getPromptData("s04e05-find-answer");
        questions.forEach((questionNo, question) -> {
            Map<String, String> badAnswers = new HashMap<>();
            boolean done = false;
            int hoops = 0;

            log.info("====================> answering question: " + question);
            log.info("====================>    question number: " + questionNo);
            while (!done) {
                hoops++;
                log.info("hoop no: " + hoops);
                String bAnswers = badAnswers.entrySet()
                        .stream()
                        .map(entry -> "zła odpowiedź: " + entry.getKey() + ";")
                        .collect(Collectors.joining("\n"));
                String systemPrompt = promptData
                        .replace("{{tekst}}", note)
                        .replace("{{bledy}}", bAnswers);

                log.info("  - błędy:\n"+bAnswers);

                String answer = openAiService.getCompletion(systemPrompt, question, String.class);
                log.info("     -  LLM's answer: " + answer);
                log.info("  - removing thinking...");
                answer = ThinkingRemover.removeThinkingProcess(answer);
                log.info("        -  answer  : " + answer);

                answers.replace(questionNo,answer);

                CentralaAnswer report = c3ntralaService.report("notes", answers, CentralaAnswer.class)
                        .orElseThrow();

                log.info("centrala report:");
                log.info(" - code   : " + report.code);
                log.info(" - message: " + report.message);
                log.info(" - hint   : " + report.hint);
                log.info(" - debug  : " + report.debug);

                done = !report.message.equals("Answer for question " + questionNo + " is incorrect");

                if (!done) {
                    badAnswers.put(answer, report.hint);
                }
            }
        });
        return answers;
    }


    private String sendQuestionsToC3ntrala(@NotNull Map<String, String> answers) {
        try {
            return c3ntralaService.report("notes", answers, String.class)
                    .orElse("");
        } catch (Exception e) {
            log.severe(e.getMessage());
            return "";
        }
    }

    record CentralaAnswer(
            Integer code,
            String message,
            String hint,
            String debug
    ) {
    }
}
