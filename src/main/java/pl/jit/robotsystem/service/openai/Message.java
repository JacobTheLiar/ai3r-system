package pl.jit.robotsystem.service.openai;

import lombok.Builder;

@Builder
public record Message(
        String role,
        String content
) {
}
