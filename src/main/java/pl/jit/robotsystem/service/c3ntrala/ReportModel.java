package pl.jit.robotsystem.service.c3ntrala;

public record ReportModel<T>(
        String task,
        String apikey,
        T answer
) {
}