package pl.jit.robotsystem.service.common;

import org.springframework.stereotype.Service;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

@Service
public class PlaceholderProcessorService {

    private static final Pattern PICTURE_PATTERN =
            Pattern.compile("\\{\\{ picture='([^']+)', description='([^']*)' }}");

    private static final Pattern VOICE_PATTERN =
            Pattern.compile("\\{\\{ voice='([^']+)' }}");

    public String processPictures(String markdown, BiFunction<String, String, String> transformer) {
        return PICTURE_PATTERN.matcher(markdown)
                .replaceAll(match -> transformer.apply(
                        match.group(1), // src
                        match.group(2)  // description
                ));
    }

    public String processVoices(String markdown, Function<String, String> transformer) {
        return VOICE_PATTERN.matcher(markdown)
                .replaceAll(match -> transformer.apply(match.group(1))); // src
    }
}
