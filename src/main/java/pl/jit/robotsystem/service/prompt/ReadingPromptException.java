package pl.jit.robotsystem.service.prompt;

public class ReadingPromptException extends RuntimeException {
    public ReadingPromptException(Exception exception) {
        super(exception);
    }
}
