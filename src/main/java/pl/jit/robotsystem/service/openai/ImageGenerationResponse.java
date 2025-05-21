package pl.jit.robotsystem.service.openai;

import java.util.List;

public record ImageGenerationResponse(
        long created,
        List<ResponseImageData> data
) {
}
