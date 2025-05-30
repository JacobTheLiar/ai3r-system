package pl.jit.robotsystem.neo;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Service
@Log
public class PersonService {
    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public void createConnection(String user1Id, String user2Id) {
        personRepository.createConnection(user1Id, user2Id);
    }

    public List<PersonNode> getConnections(String userId) {
        return personRepository.findConnectedPersons(userId);
    }

    public boolean needInitConnections() {
        return !personRepository.hasAnyConnections();
    }

    public boolean needInitPersons() {
        return !personRepository.hasAnyPersons();
    }

    public void createPerson(String userId, String name) {
        PersonNode person = new PersonNode();
        person.setUserId(userId);
        person.setName(name);
        personRepository.save(person);
    }

    public Optional<PersonNode> findPerson(String userId) {
        return personRepository.findByUserId(userId);
    }

    public Optional<PersonNode> findPersonByName(String name) {
        return personRepository.findByName(name);
    }

    public PersonNode savePerson(PersonNode person) {
        return personRepository.save(person);
    }

    public void deletePerson(String userId) {
        personRepository.deleteById(userId);
    }

    public List<PersonNode> getAllPersons() {
        return personRepository.findAll();
    }

    public Optional<List<String>> getShortestConnectionChain(String fromUserId, String toUserId) {
        return personRepository.findShortestPath(fromUserId, toUserId);
    }

    public List<List<String>> getAllConnectionChains(String fromUserId, String toUserId, int maxDepth) {
        return personRepository.findAllPaths(fromUserId, toUserId, maxDepth, 10);
    }
}
