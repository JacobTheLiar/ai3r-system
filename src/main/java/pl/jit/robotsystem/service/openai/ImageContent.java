package pl.jit.robotsystem.service.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ImageContent(
    String type,
    @JsonProperty("image_url")
    ImageUrl imageUrl
){}
