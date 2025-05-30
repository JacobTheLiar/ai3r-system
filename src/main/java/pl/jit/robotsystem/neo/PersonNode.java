package pl.jit.robotsystem.neo;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Node("Person")
@Data
public class PersonNode {
    @Id
    private String userId;
    private String name;

    @Relationship(type = "CONNECTED_TO", direction = Relationship.Direction.OUTGOING)
    private Set<PersonNode> connections = new HashSet<>();
}
