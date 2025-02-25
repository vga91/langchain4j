package dev.langchain4j;

import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

public class Neo4jTestUtils {

    public static Neo4jContainer<?> getNeo4jContainer() {
        return new Neo4jContainer<>(DockerImageName.parse("neo4j:5.26"));
    }
}
