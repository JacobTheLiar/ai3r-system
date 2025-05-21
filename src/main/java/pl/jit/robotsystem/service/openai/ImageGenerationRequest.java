package pl.jit.robotsystem.service.openai;

import lombok.Builder;

@Builder
public record ImageGenerationRequest(
        String model,
        String prompt,
        int n,
        String quality,
        String size,
        String style) {
}
