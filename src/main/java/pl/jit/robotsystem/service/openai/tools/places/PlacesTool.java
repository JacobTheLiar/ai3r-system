package pl.jit.robotsystem.service.openai.tools.places;

import org.springframework.web.reactive.function.client.WebClient;
import pl.jit.robotsystem.service.openai.tools.Tool;

public class PlacesTool implements Tool {

    private final WebClient webClient;
    private final String ai3rApiKey;

    public PlacesTool(WebClient webClient, String ai3rApiKey) {
        this.webClient = webClient;
        this.ai3rApiKey = ai3rApiKey;
    }

    @Override
    public String getName() {
        return "places";
    }

    @Override
    public String getDescription() {
        return "Pozwala uzyskać listę osób przebywających w danej miejscowości, użyj POZNAŃ by otrzymać JAKUB EWELINA KORNELIA AMELIA.";
    }

    @Override
    public String execute(String input) {
        return webClient.post()
                .uri("/places")
                .bodyValue(getRequest(input))
                .retrieve()
                .bodyToMono(Response.class)
                .map(Response::message)
                .block();
    }

    private Request getRequest(String place){
        return new Request(ai3rApiKey, place);
    }

    private record Request(String apikey, String query) {
    }

    private record Response(Integer code, String message) {
    }
}
