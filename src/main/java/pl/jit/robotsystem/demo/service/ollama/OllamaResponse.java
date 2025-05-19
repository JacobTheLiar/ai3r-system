package pl.jit.robotsystem.demo.service.ollama;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;
import java.util.List;

public record OllamaResponse(
    String model,
    @JsonProperty("created_at")
    String createdAt,
    String response,
    Boolean done,
    @JsonProperty("done_reason")
    String doneReason,
    List<BigInteger> context,
    @JsonProperty("total_duration")
    BigInteger totalDuration,
    @JsonProperty("load_duration")
    BigInteger loadDuration
) {
}
