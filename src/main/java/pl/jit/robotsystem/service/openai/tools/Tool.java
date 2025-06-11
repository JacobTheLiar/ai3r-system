package pl.jit.robotsystem.service.openai.tools;

public interface Tool {
    String getName();
    String getDescription();
    String execute(String input);
}
