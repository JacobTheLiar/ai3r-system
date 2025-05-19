package pl.jit.robotsystem.demo.service.openai;

import lombok.Builder;

@Builder
public record Message(
        String role,
        String content
) {
}
