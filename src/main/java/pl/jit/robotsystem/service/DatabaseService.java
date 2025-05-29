package pl.jit.robotsystem.service;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaDbService;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.service.openai.OpenAiService;
import pl.jit.robotsystem.service.prompt.PromptRepository;
import pl.jit.robotsystem.share.FlagFinder;
import pl.jit.robotsystem.share.No5Action;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * S03E03 - generate SQL
 */
@Service
@Log
public class DatabaseService implements No5Action {
    // SECRET: "queryBase64": "U0VMRUNUIEdST1VQX0NPTkNBVChsZXR0ZXIgT1JERVIgQlkgd2VpZ2h0IFNFUEFSQVRPUiAnJykgYXMgZmxhZyBGUk9NIGNvcnJlY3Rfb3JkZXI7"

    private final C3ntralaDbService c3ntralaDbService;
    private final PromptRepository promptRepository;
    private final OpenAiService openAiService;
    private final C3ntralaService c3ntralaService;

    public DatabaseService(C3ntralaDbService c3ntralaDbService, PromptRepository promptRepository, OpenAiService openAiService, C3ntralaService c3ntralaService) {
        this.c3ntralaDbService = c3ntralaDbService;
        this.promptRepository = promptRepository;
        this.openAiService = openAiService;
        this.c3ntralaService = c3ntralaService;
    }

    public void action() {
        String tables = """
                CREATE TABLE `datacenters` (
                  `dc_id` int(11) DEFAULT NULL,
                  `location` varchar(30) NOT NULL,
                  `manager` int(11) NOT NULL DEFAULT 31,
                  `is_active` int(11) DEFAULT 0
                )
                
                CREATE TABLE `users` (
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `username` varchar(20) DEFAULT NULL,
                  `access_level` varchar(20) DEFAULT 'user',
                  `is_active` int(11) DEFAULT 1,
                  `lastlog` date DEFAULT NULL,
                  PRIMARY KEY (`id`)
                )""";


        String prompt = promptRepository.getPromptData("s03e03-datacenter-ids");
        String completion = openAiService.getCompletion(prompt, tables);
        log.info("SQL generated: " + completion);

        List<Map<String, String>> data = c3ntralaDbService.getQuery(completion);

        List<Integer> list = data.stream()
                .map(mapItem -> mapItem.values().stream().findFirst().orElse(null))
                .filter(Objects::nonNull)
                .map(Integer::parseInt)
                .toList();
        log.info(" - data: " + list);

        c3ntralaService.report("database", list, String.class)
                .ifPresent(FlagFinder::containsFlag);

        log.info("Done!");
    }
}
