package pl.jit.robotsystem.demo.service.prompt;

public class PromptNotFoundException extends RuntimeException {
    public PromptNotFoundException(String promptName) {
        super("Prompt [" + promptName + "] not found");
    }
}
