package dev.langchain4j.transformer;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

// TODO - mettere i test con Neo4j, mentre nell'altro quelli solo di graphTransformer
// todo - chiamarlo converter or roba del genere
public class Neo4jGraphTransformer {
    // TODO... 
    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.26"))
            .withoutAuthentication()
            .withPlugins("apoc");
    
    @BeforeEach
    void beforeEach() {
        Document docCat = new DefaultDocument("Sylvester the cat is on the table", Metadata.from("key2", "value2"));
        Document docKeanu = new DefaultDocument("Keanu Reeves acted in Matrix", Metadata.from("key33", "value3"));
        final List<Document> documents = List.of(docCat, docKeanu);
        // TODO -
    }
    
    // TODO - tests
}
