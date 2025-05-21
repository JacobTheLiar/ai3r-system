package pl.jit.robotsystem.service;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.openai.ImageGenerationResponse;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.prompt.PromptRepository;
import pl.jit.robotsystem.share.No5Action;


@Service
@Log
public class RobotIdService implements No5Action {

    private final C3ntralaService c3ntralaService;
    private final PromptRepository promptRepository;
    private final OpenAiService openAiService;

    public RobotIdService(C3ntralaService c3ntralaService, PromptRepository promptRepository, OpenAiService openAiService) {
        this.c3ntralaService = c3ntralaService;
        this.promptRepository = promptRepository;
        this.openAiService = openAiService;
    }

    public void action() {
        log.info("Downloading root description...");
        c3ntralaService.getData("robotid.json", RobotDescriptionResponse.class)
                .ifPresentOrElse(
                        this::processRobotDescription,
                        () -> log.warning("Failed to download robot description")
                );

    }

    private void processRobotDescription(RobotDescriptionResponse response) {
        log.info("Robot description: " + response.description);
        String prompt = promptRepository.getPromptData("s03e03-robot-id")
                .replace("{{zeznanie}}", response.description);
        log.info("Generating image with prompt:\n\n" + prompt+"\n\n");

        ImageGenerationResponse aiResponse = openAiService.generateImage(prompt);

        log.info("Generated image with response:\n\n" + aiResponse);

        log.info("Sending response to c3ntrala");

        c3ntralaService.report("robotid", aiResponse.data().getFirst().url(), String.class)
                .ifPresent(c3response -> log.info("Response from c3ntrala: " + c3response));

        log.info("Done!");
    }

    record RobotDescriptionResponse(String description){}
}
