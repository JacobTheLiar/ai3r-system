package pl.jit.robotsystem.service.openai;

import lombok.Builder;

import java.util.List;

@Builder
public record ImageMessage(
        String role,
        List<ImageContent> content)
        implements Message<List<ImageContent>> {
}
