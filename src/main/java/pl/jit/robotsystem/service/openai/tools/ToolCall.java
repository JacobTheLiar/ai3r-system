package pl.jit.robotsystem.service.openai.tools;

public record ToolCall(
        String toolName,
        String query
) {
}
