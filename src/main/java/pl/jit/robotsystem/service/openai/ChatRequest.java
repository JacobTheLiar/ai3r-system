package pl.jit.robotsystem.service.openai;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatRequest(
        Number temperature,
        String model,
        List<Message> messages) {
}
