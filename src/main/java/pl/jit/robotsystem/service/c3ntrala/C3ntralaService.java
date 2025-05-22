package pl.jit.robotsystem.service.c3ntrala;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.File;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Optional;

import static java.time.Duration.ofSeconds;
import static pl.jit.robotsystem.configuration.DotenvConfiguration.AI3R_API_KEY;

@SuppressWarnings("FieldCanBeLocal")
@Service
@Log
public class C3ntralaService {

    private final String DATA_URL = "/data/%s/%s";
    private final String REPORT_URL = "/report";
    private final WebClient webClient;
    private final String ai3rApiKey;
    private final File workingDirectory;
    private final ObjectMapper objectMapper;


    public C3ntralaService(@Qualifier("c3ntralaApiClient") WebClient webClient, Dotenv dotenv, File workingDirectory, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.ai3rApiKey = dotenv.get(AI3R_API_KEY);
        this.workingDirectory = workingDirectory;
        this.objectMapper = objectMapper;
    }

    public <RESPONSE> Optional<RESPONSE> getData(String filename, Class<RESPONSE> responseType) {
        log.info("Retrieving data [%s] from c3entrala's API ...".formatted(filename));
        RESPONSE result = webClient.get()
                .uri(DATA_URL.formatted(ai3rApiKey, filename))
                .retrieve()
                .bodyToMono(responseType)
                .timeout(ofSeconds(10))
                .block();
        log.info("Retrieved data: %s".formatted(result));
        return Optional.ofNullable(result);
    }

    public <RESPONSE, REPORT> Optional<RESPONSE> report(String task, REPORT reportData, Class<RESPONSE> responseType) {
        log.info("Sending report [%s] to c3ntrala...".formatted(task));
        ReportModel<REPORT> censoredData = new ReportModel<>(task, ai3rApiKey, reportData);
        try {
            log.info(" - data: \n%s".formatted(objectMapper.writeValueAsString(censoredData)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        RESPONSE response = webClient.post()
                .uri(REPORT_URL)
                .bodyValue(censoredData)
                .retrieve()
                .bodyToMono(responseType)
                .timeout(ofSeconds(10))
                .block();
        log.info("C3ntrala's response: " + response);
        return Optional.ofNullable(response);
    }

    public File downloadFile(String apiPath, String filename) {

        File downloadedFile = new File(workingDirectory, filename);

        if (downloadedFile.exists()) {
            log.info("Getting file [%s] from disk ...".formatted(filename));
            return downloadedFile;
        }

        try {
            log.info("Downloading file [%s] from c3entrala's API ...".formatted(filename));
            Flux<DataBuffer> dataBufferFlux = webClient.get()
                    .uri(apiPath)
                    .retrieve()
                    .bodyToFlux(DataBuffer.class);

            DataBufferUtils.write(dataBufferFlux, downloadedFile.toPath(),
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                    .block(Duration.ofMinutes(2));

            log.info("Downloaded file saved to: %s".formatted(downloadedFile));
            return downloadedFile;

        } catch (Exception e) {
            log.severe("Error downloading file: " + e.getMessage());
            throw new RuntimeException("Failed to download file", e);
        }
    }
}
