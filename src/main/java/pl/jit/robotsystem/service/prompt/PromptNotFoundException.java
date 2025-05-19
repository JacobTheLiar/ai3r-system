package pl.jit.robotsystem.service.prompt;

public class PromptNotFoundException extends RuntimeException {
    public PromptNotFoundException(String promptName) {
        super("Prompt [" + promptName + "] not found");
    }
}
