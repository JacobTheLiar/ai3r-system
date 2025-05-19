package pl.jit.robotsystem.share;

import java.util.Optional;
import java.util.regex.Pattern;

public class ThinkingRemover {

    private static final Pattern THINKING_PATTERN = Pattern.compile("<think>.*?</think>", Pattern.DOTALL);

    private ThinkingRemover() {
        // private constructor to prevent instantiation
    }

    public static String removeThinkingProcess(String message) {
        return Optional.ofNullable(message)
                .map(msg -> THINKING_PATTERN.matcher(msg).replaceAll(""))
                .orElse("");
    }
}
