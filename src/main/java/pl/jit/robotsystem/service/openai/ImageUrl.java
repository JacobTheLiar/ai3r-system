package pl.jit.robotsystem.service.openai;

import lombok.Builder;

@Builder
public record ImageUrl(
        String url,
        String detail
) {
}
