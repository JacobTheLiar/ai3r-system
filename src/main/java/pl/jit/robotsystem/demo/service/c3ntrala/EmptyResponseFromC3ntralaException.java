package pl.jit.robotsystem.demo.service.c3ntrala;

public class EmptyResponseFromC3ntralaException extends RuntimeException {
    public EmptyResponseFromC3ntralaException() {
        super("response from c3nrtala is empty");
    }
}
