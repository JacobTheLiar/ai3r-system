package pl.jit.robotsystem.service.openai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResponseImageData(
        String url,
        @JsonProperty("revised_prompt")
        String revisedPrompt
) {
}
