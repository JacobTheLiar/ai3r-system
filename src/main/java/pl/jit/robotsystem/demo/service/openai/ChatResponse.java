package pl.jit.robotsystem.demo.service.openai;

import java.util.List;

public record ChatResponse(
        List<Choice> choices
) {
}