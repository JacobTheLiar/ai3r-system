package pl.jit.robotsystem.service;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.prompt.PromptRepository;
import pl.jit.robotsystem.share.No5Action;

import static pl.jit.robotsystem.share.FlagFinder.containsFlag;


/**
 * S01E02 - cheat the robot
 */
@SuppressWarnings("FieldCanBeLocal")
@Service
@Log
public class VerifyService implements No5Action {

    private final String VERIFY_ENDPOINT = "/verify";

    private final WebClient webClient;
    private final OpenAiService aiService;
    private final PromptRepository promptRepository;

    public VerifyService(@Qualifier("xyzApiClient") WebClient webClient, OpenAiService aiService, PromptRepository promptRepository) {
        this.webClient = webClient;
        this.aiService = aiService;
        this.promptRepository = promptRepository;
    }

    public void action() {
        VerifyModel response = talkToRobot(new VerifyModel(0, "READY"));

        log.info("Received: " + response.text + " with ID: " + response.msgID);

        while (!"OK".equals(response.text) || containsFlag(response.text)) {
            log.info("Getting a answer for a question [%s]...".formatted(response.text));
            String prompt = promptRepository.getPromptData("s01e02-cheat-the-robot");
            String answer = aiService.getCompletion(prompt, response.text);

            response = talkToRobot(new VerifyModel(response.msgID, answer));
        }
        log.info("Done!");
    }

    private VerifyModel talkToRobot(VerifyModel verifyModel) {
        log.info("Sending [%s]...".formatted(verifyModel.text()));
        VerifyModel response = webClient.post()
                .uri(VERIFY_ENDPOINT)
                .bodyValue(verifyModel)
                .retrieve()
                .bodyToMono(VerifyModel.class)
                .block();
        if (response != null) {
            log.info("Received: " + response.text() + " with ID: " + response.msgID);
            return response;
        }
        throw new RuntimeException("Response is null");
    }

    record VerifyModel(
            int msgID,
            String text) {
    }
}
