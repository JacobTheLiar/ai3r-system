package pl.jit.robotsystem.service.openai;

import lombok.Builder;



@Builder
public record StringMessage(
        String role,
        String content)
        implements Message<String> {
}
