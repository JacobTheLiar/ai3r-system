package pl.jit.robotsystem.demo.service.prompt;

public class ReadingPromptException extends RuntimeException {
    public ReadingPromptException(Exception exception) {
        super(exception);
    }
}
