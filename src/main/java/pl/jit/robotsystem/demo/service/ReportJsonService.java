package pl.jit.robotsystem.demo.service;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.demo.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.demo.service.c3ntrala.EmptyResponseFromC3ntralaException;
import pl.jit.robotsystem.demo.service.openai.OpenAiService;
import pl.jit.robotsystem.demo.service.prompt.PromptRepository;
import pl.jit.robotsystem.demo.share.FlagFinder;
import pl.jit.robotsystem.demo.share.No5Action;

import java.util.Arrays;
import java.util.List;

/**
 * S01E03 - verify and fix a JSON values
 */
@SuppressWarnings("FieldCanBeLocal")
@Service
@Log
public class ReportJsonService implements No5Action {

    private final C3ntralaService c3ntralaService;
    private final OpenAiService aiService;
    private final ObjectMapper objectMapper;
    private final PromptRepository promptRepository;

    public ReportJsonService(C3ntralaService c3ntralaService, OpenAiService aiService, ObjectMapper objectMapper, PromptRepository promptRepository) {
        this.c3ntralaService = c3ntralaService;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
        this.promptRepository = promptRepository;
    }

    public void action() {
        TestData originalData = c3ntralaService.getData("json.txt", TestData.class)
                .orElseThrow(EmptyResponseFromC3ntralaException::new);

        log.info("Fixing answers...");
        if (originalData == null || originalData.testData == null) {
            log.warning("No data to fix!");
            return;
        }

        originalData.testData = originalData.testData.stream()
                .peek(data -> {
                    String calculated = calculateSum(data.question);
                    if (!data.answer.equals(calculated)) {
                        log.info(" - Fixing answer for question: " + data.question);
                        log.info("   * Original answer: " + data.answer);
                        log.info("   * Calculated answer: " + calculated);
                        data.answer = calculated;
                    }
                }).toList();

        log.info("Answering test questions...");
        log.info("  - collecting data...");

        List<ItemTest> questionList = originalData.testData.stream()
                .filter(data -> data.test != null)
                .map(TestDataItem::getTest)
                .peek(data -> log.info("    * found question: " + data.q)).toList();

        log.info("  - answering data...");

        try {
            String questionListJson = objectMapper.writeValueAsString(questionList);
            String prompt = promptRepository.getPromptData("s01e03-fix-json");
            aiService.getCompletionList(prompt, questionListJson, ItemTest.class)
                    .forEach(answer -> originalData.testData.stream()
                            .filter(item -> item.getTest() != null && item.getTest().getQ().equals(answer.getQ()))
                            .findFirst()
                            .ifPresent(item -> {
                                log.info("    * fixing answer for question: " + item.getTest().getQ());
                                log.info("      - Original answer: " + item.getTest().getA());
                                log.info("      - Calculated answer: " + answer.getA());
                                item.getTest().setA(answer.getA());
                            }));
        } catch (Exception e) {
            log.warning("Error while parsing JSON data: " + e.getMessage());
            return;
        }

        log.info("Sending data... ");
        String response = c3ntralaService.report("JSON", originalData, String.class)
                .orElseThrow(EmptyResponseFromC3ntralaException::new);
        log.info("Response: \n" + response);
        FlagFinder.containsFlag(response);
        log.info("Done!");
    }

    private String calculateSum(String expression) {
        return String.valueOf(
                Arrays.stream(expression.split("\\+"))
                        .map(String::trim)
                        .mapToInt(Integer::parseInt)
                        .sum()
        );
    }

    @Data
    static class ItemTest {
        private String q;
        private String a;
    }

    @Data
    static class TestDataItem {
        private String question;
        private String answer;
        private ItemTest test;
    }

    @Data
    static class TestData {
        private String apikey;
        private String description;
        private String copyright;

        @JsonProperty("test-data")
        private List<TestDataItem> testData;
    }
}
