package pl.jit.robotsystem.service;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import pl.jit.robotsystem.neo.PersonNode;
import pl.jit.robotsystem.neo.PersonService;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaDbService;
import pl.jit.robotsystem.service.c3ntrala.C3ntralaService;
import pl.jit.robotsystem.share.FlagFinder;
import pl.jit.robotsystem.share.No5Action;

import java.util.List;
import java.util.Map;

/**
 * S03E03 - use neo4j
 */
@Service
@Log
public class ConnectionsService implements No5Action {

    private final C3ntralaDbService c3ntralaDbService;
    private final C3ntralaService c3ntralaService;
    private final PersonService personService;

    private static final String USER_RAFAL = "Rafał";
    private static final String USER_BARBARA = "Barbara";

    public ConnectionsService(C3ntralaDbService c3ntralaDbService, C3ntralaService c3ntralaService, PersonService personService) {
        this.c3ntralaDbService = c3ntralaDbService;
        this.c3ntralaService = c3ntralaService;
        this.personService = personService;
    }

    @Override
    public void action() {
        initializePersonsIfNeeded();
        initializeConnectionsIfNeeded();
        findAndReportConnectionPath();

        log.info("Done!");
    }

    private void initializePersonsIfNeeded() {
        if (!personService.needInitPersons()) {
            return;
        }

        log.info("Persons need to be initialized");
        List<Map<String, String>> persons = c3ntralaDbService.getQuery("select id, username from users;");
        persons.forEach(this::createPerson);
    }

    private void createPerson(Map<String, String> person) {
        log.info(" - adding person: " + person);
        personService.createPerson(person.get("id"), person.get("username"));
    }

    private void initializeConnectionsIfNeeded() {
        if (!personService.needInitConnections()) {
            return;
        }

        log.info("Connections need to be initialized");
        List<Map<String, String>> connections = c3ntralaDbService.getQuery("select * from connections;");
        connections.forEach(this::createConnection);
    }

    private void createConnection(Map<String, String> connection) {
        log.info(" - adding connection: " + connection);
        personService.createConnection(connection.get("user1_id"), connection.get("user2_id"));
    }

    private void findAndReportConnectionPath() {
        log.info("Finding connected persons between Barbara and Rafal");

        PersonNode rafal = findPersonByName(USER_RAFAL);
        PersonNode barbara = findPersonByName(USER_BARBARA);

        personService.getShortestConnectionChain(rafal.getUserId(), barbara.getUserId())
                .ifPresent(this::reportConnectionPath);
    }

    private PersonNode findPersonByName(String name) {
        PersonNode person = personService.findPersonByName(name)
                .orElseThrow(() -> new IllegalStateException("Person not found: " + name));
        log.info(" - " + name + ": " + person);
        return person;
    }

    private void reportConnectionPath(List<String> connectionPath) {
        log.info(" - found connection path: " + connectionPath);
        String pathString = String.join(",", connectionPath);
        c3ntralaService.report("connections", pathString, String.class)
                .ifPresent(FlagFinder::containsFlag);
    }
}
