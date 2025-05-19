package pl.jit.robotsystem.demo.service.ollama;

import lombok.Builder;

@Builder
public record OllamaRequest(
        String model,
        Boolean stream,
        String system,
        String prompt
) {
}
