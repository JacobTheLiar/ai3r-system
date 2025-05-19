package pl.jit.robotsystem.demo.service.c3ntrala;

public record ReportModel<T>(
        String task,
        String apikey,
        T answer
) {
}