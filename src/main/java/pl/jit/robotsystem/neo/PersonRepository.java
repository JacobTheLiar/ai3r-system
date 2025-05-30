package pl.jit.robotsystem.neo;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public interface PersonRepository extends Neo4jRepository<PersonNode, String> {

    @Query("MATCH (p1:Person {userId: $user1Id}) " +
           "MATCH (p2:Person {userId: $user2Id}) " +
           "WHERE p1 <> p2 " +
           "CREATE (p1)-[:CONNECTED_TO]->(p2)")
    void createConnection(String user1Id, String user2Id);

    @Query("MATCH (p:Person {userId: $userId})-[:CONNECTED_TO]->(connected) RETURN connected")
    List<PersonNode> findConnectedPersons(String userId);

    Optional<PersonNode> findByUserId(String userId);

    // Najkrótsza ścieżka
    @Query("MATCH path = shortestPath((start:Person {userId: $startUserId})-[:CONNECTED_TO*1..10]-(end:Person {userId: $endUserId})) " +
           "RETURN [node in nodes(path) | node.name] as names")
    Optional<List<String>> findShortestPath(String startUserId, String endUserId);

    // Wszystkie ścieżki do określonej głębokości
    @Query("MATCH path = (start:Person {userId: $startUserId})-[:CONNECTED_TO*1..$maxDepth]-(end:Person {userId: $endUserId}) " +
           "RETURN [node in nodes(path) | node.name] as names " +
           "ORDER BY length(path) LIMIT $limit")
    List<List<String>> findAllPaths(String startUserId, String endUserId, int maxDepth, int limit);

    // Ścieżka z określoną długością
    @Query("MATCH path = (start:Person {userId: $startUserId})-[:CONNECTED_TO*$pathLength]-(end:Person {userId: $endUserId}) " +
           "RETURN [node in nodes(path) | node.name] as names")
    List<List<String>> findPathsWithLength(String startUserId, String endUserId, int pathLength);


    // Sprawdź, czy istnieją jakiekolwiek węzły Person
    @Query("MATCH (p:Person) RETURN count(p) > 0")
    boolean hasAnyPersons();

    // Sprawdź, czy istnieją relacje
    @Query("MATCH ()-[:CONNECTED_TO]-() RETURN count(*) > 0")
    boolean hasAnyConnections();

    @Query("MATCH (p:Person {name: $name}) RETURN p")
    Optional<PersonNode> findByName(String name);

}
