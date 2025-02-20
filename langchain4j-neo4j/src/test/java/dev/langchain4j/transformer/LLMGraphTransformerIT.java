package dev.langchain4j.transformer;

//import dev.langchain4j.data.document.Document;
//import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.store.graph.neo4j.Neo4jGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static dev.langchain4j.transformer.LLMGraphTransformer.*;

@Testcontainers
class LLMGraphTransformerIT {

    public static final String USERNAME = "neo4j";
    public static final String ADMIN_PASSWORD = "adminPass";

    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.26"))
            .withoutAuthentication()
            .withPlugins("apoc");

    private LLMGraphTransformer graphTransformer;
    private Neo4jGraph graph;
    private Driver driver;
    
    @BeforeEach
    void setUp() {
        driver = GraphDatabase.driver(neo4jContainer.getBoltUrl(), AuthTokens.none());
        
        graphTransformer = new LLMGraphTransformer(new MockLLMService());
        graph = Neo4jGraph.builder()
                .driver(driver)
                .build();
    }

    @AfterEach
    void tearDown() {
        graph = null;
    }

//    @Test
//    void testTransformGraph() {
//        Node node = new Node("1", "Original Data");
//        graph.addNode(node);
//
//        Graph transformedGraph = graphTransformer.transform(graph);
//
//        Assertions.assertEquals(1, transformedGraph.getNodes().size());
//        Assertions.assertNotEquals("Original Data", transformedGraph.getNodes().get(0).getData());
//    }

    @Test
    void testAddGraphDocuments() {
        // TODO - CHANGE DOCUMENT WITH LANGCHAIN DOCUMENT
//        Document doc = new Document("doc1", Metadata.from("Test key", "Test content"));
        Document doc1 = new Document("Elon Musk is suing OpenAI", Metadata.from("key1", "value1"));
        Document doc2 = new Document("The cat is on the table", Metadata.from("key2", "value2"));
        Document doc3 = new Document("Keanu Reeves acted in Matrix", Metadata.from("key3", "value3"));
        final List<Document> documents = List.of(doc1, doc2, doc3);
        List<LLMGraphTransformer.GraphDocument> graphDocs = graphTransformer.convertToGraphDocuments(documents);

        graph.addGraphDocuments(graphDocs, false, false);
        
        
        // TODO - ASSERTIONS 
//        graphTransformer.addGraphDocuments(graph, graphDocs, false, false);

//        Assertions.assertEquals(1, graph.getNodes().size());
//        Assertions.assertEquals("Test content", graph.getNodes().get(0).getData());
    }

    static class MockLLMService implements LLMGraphTransformer.LLMService {
        @Override
        public String callLLM(String prompt) {
            return "Transformed " + prompt;
        }
    }
}
