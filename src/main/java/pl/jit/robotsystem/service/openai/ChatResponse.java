package pl.jit.robotsystem.service.openai;

import java.util.List;

public record ChatResponse(
        List<Choice> choices
) {
}