package pl.jit.robotsystem.configuration;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotenvConfiguration {
    public static final String OPENAI_API_KEY = "OPENAI_API_KEY";
    public static final String AI3R_API_KEY = "AI3R_API_KEY";
    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .directory("./")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();
    }
}
