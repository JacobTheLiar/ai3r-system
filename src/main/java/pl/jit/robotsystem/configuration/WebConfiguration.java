package pl.jit.robotsystem.configuration;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import static pl.jit.robotsystem.configuration.DotenvConfiguration.OPENAI_API_KEY;

@Configuration
public class WebConfiguration {

    public static final String AI_URL = "https://api.openai.com/v1";
    public static final String XYZ_URL = "https://xyz.ag3nts.org";
    public static final String C3NTRALA_URL = "https://c3ntrala.ag3nts.org";
    public static final String OLLAMA_URL = "http://localhost:11434/";
    public static final String LOCAL_QDRANT_URL = "http://localhost:6333/collections/";

    @Bean("xyzApiClient")
    public WebClient webClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .followRedirect(true)))
                .baseUrl(XYZ_URL)
                .build();
    }

    @Bean("c3ntralaApiClient")
    public WebClient c3ntralaClient() {
        return WebClient.builder().codecs(config -> config
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)
                ).baseUrl(C3NTRALA_URL)
                .build();
    }

    @Bean("ollamaClient")
    public WebClient ollamaClient() {
        return WebClient.create(OLLAMA_URL);
    }

    @Bean("openAiClient")
    public WebClient openAiClient(Dotenv dotenv) {
        return WebClient.builder()
                .baseUrl(AI_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + dotenv.get(OPENAI_API_KEY))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean("localQdrantClient")
    public WebClient qdrantClient() {
        return WebClient.create(LOCAL_QDRANT_URL);
    }
}
