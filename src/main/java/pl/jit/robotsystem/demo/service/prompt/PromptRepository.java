package pl.jit.robotsystem.demo.service.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;

@Service
@RequiredArgsConstructor
@Log
public class PromptRepository {

    private static final String CLASSPATH_PATTERN = "classpath:prompt/%s.md";

    private final ResourceLoader resourceLoader;

    public String getPromptData(String promptName) {
        Resource resource = resourceLoader.getResource(String.format(CLASSPATH_PATTERN, promptName));
        if (resource.exists()) {
            try {
                String prompt = Files.readString(resource.getFile().toPath());
                log.info("Found prompt:\n<prompt-data name=\"%s\">\n%s\n<\\prompt-data>\n".formatted(promptName, prompt.trim()));
                return new String(Files.readAllBytes(resource.getFile().toPath()));
            } catch (IOException e) {
                throw new ReadingPromptException(e);
            }
        }
        throw new PromptNotFoundException(promptName);
    }

}
