package pl.jit.robotsystem.service.openai;

public interface Message<T> {
    String role();
    T content();
}
