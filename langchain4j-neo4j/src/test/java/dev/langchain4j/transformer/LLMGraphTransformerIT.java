package dev.langchain4j.transformer;

//import dev.langchain4j.data.document.Document;
//import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.graph.neo4j.Neo4jGraph;
import org.junit.After;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.util.Iterables;
import org.neo4j.driver.internal.value.PathValue;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static dev.langchain4j.store.graph.neo4j.Neo4jGraph.BASE_ENTITY_LABEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Testcontainers
class LLMGraphTransformerIT {

//    OpenAiChatModel model = OpenAiChatModel.builder()
//            .baseUrl("demo")
//            .apiKey("demo")
//            .organizationId(System.getenv("OPENAI_ORGANIZATION_ID"))
//            .modelName(GPT_4_O_MINI)
//            .logRequests(true)
//            .logResponses(true)
//            .build();


    private static ChatLanguageModel model;
//    
//    public static final String USERNAME = "neo4j";
//    public static final String ADMIN_PASSWORD = "adminPass";

    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.26"))
            .withoutAuthentication()
            .withPlugins("apoc");

    private static LLMGraphTransformer graphTransformer;
    private static Neo4jGraph graph;
    private static Driver driver;
    
    @BeforeAll
    static void beforeAll() {
        driver = GraphDatabase.driver(neo4jContainer.getBoltUrl(), AuthTokens.none());

        model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .organizationId(System.getenv("OPENAI_ORGANIZATION_ID"))
                .modelName(GPT_4_O_MINI)
                .logRequests(true)
                .logResponses(true)
                .build();

        // TODO - EXAMPLES WITH VARIOUS BUILDER
        graphTransformer = LLMGraphTransformer.builder()
                .model(model)
                .build();
        graph = Neo4jGraph.builder()
                .driver(driver)
                .build();
    }
    
    @BeforeEach
    void beforeEach() {
        // graph.e
    }

    @AfterEach
    void afterEach() {
        graph.executeWrite("MATCH (n) DETACH DELETE n");
    }
    
    @AfterAll
    static void afterAll() {
        graph.close();
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
        Document doc2 = new DefaultDocument("Sylvester the cat is on the table", Metadata.from("key2", "value2"));
        Document doc3 = new DefaultDocument("Keanu Reeves acted in Matrix", Metadata.from("key3", "value3"));
        final List<Document> documents = List.of(doc2, doc3);
        List<GraphDocument> graphDocs = graphTransformer.convertToGraphDocuments(documents);

        graph.addGraphDocuments(graphDocs, false, false);
        
        // TODO - ASSERTIONS 
        final List<Record> records = graph.executeRead("MATCH p=()-[]->() RETURN p");
        assertThat(records).hasSize(2);
        records.forEach(record -> {
            final PathValue p = (PathValue) record.get("p");
            final Path path = p.asPath();
            assertThat(path).hasSize(1);
            final Node start = path.start();
            assertThat(start.labels()).hasSize(1);
            assertThat(start.labels()).doesNotContain(BASE_ENTITY_LABEL);
            final Node end = path.end();
            assertThat(end.labels()).hasSize(1);
            assertThat(end.labels()).doesNotContain(BASE_ENTITY_LABEL);
            final List<Relationship> rels = Iterables.asList(path.relationships());
            assertThat(rels).hasSize(1);
        });
    }
    
    
    @Test
    void testAddGraphDocumentsWithBaseEntityLabel() {
        Document docCat = new DefaultDocument("Sylvester the cat is on the table", Metadata.from("key2", "value2"));
        Document docKeanu = new DefaultDocument("Keanu Reeves acted in Matrix", Metadata.from("key33", "value3"));
        final List<Document> documents = List.of(docCat, docKeanu);
        
        // TODO - maybe metadata stored here in GraphDocument?
        List<GraphDocument> graphDocs = graphTransformer.convertToGraphDocuments(documents);

        graph.addGraphDocuments(graphDocs, false, true);
        
        final List<Record> records = graph.executeRead("MATCH p=()-[]->() RETURN p");
        assertThat(records).hasSize(2);
        records.forEach(record -> {
            final PathValue p = (PathValue) record.get("p");
            final Path path = p.asPath();
            assertThat(path).hasSize(1);
            final Node start = path.start();
            assertThat(start.labels()).hasSize(2);
            assertThat(start.labels()).contains(BASE_ENTITY_LABEL);
            final Node end = path.end();
            assertThat(end.labels()).hasSize(2);
            assertThat(end.labels()).contains(BASE_ENTITY_LABEL);
            final List<Relationship> rels = Iterables.asList(path.relationships());
            assertThat(rels).hasSize(1);
        });
    }

    @Test
    void testAddGraphDocumentsWithMissingModel() {
        try {
            LLMGraphTransformer.builder().build();
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("model cannot be null");
        }
    }

    @Test
    void testAddGraphDocumentsWithCustomPrompt() {
        final List<ChatMessage> prompt = List.of(new UserMessage("return just a null value"));
        
        final LLMGraphTransformer build = LLMGraphTransformer.builder()
                .model(model)
                .prompt(prompt)
                .build();

        final List<GraphDocument> documents = build.convertToGraphDocuments(List.of(Document.from("foobar")));
        System.out.println("documents = " + documents);
    }


    @Test
    void testAddGraphDocumentsWithDeDuplication() {
        Document doc3 = new DefaultDocument("Keanu Reeves acted in Matrix. Keanu was born in Beirut", Metadata.from("key3", "value3"));
        // TODO
        // final List<Document> documents = List.of(doc2, doc3);

        final List<Document> documents = List.of(doc3);
        List<GraphDocument> graphDocs = graphTransformer.convertToGraphDocuments(documents);

        assertThat(graphDocs).hasSize(1);
        final GraphDocument graphDocument = graphDocs.get(0);
        final Set<GraphDocument.Edge> relationships = graphDocument.getRelationships();
        assertThat(relationships).hasSize(2);
        final String relStrings = relationships.stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
        assertThat(relStrings).containsIgnoringCase("acted");
        assertThat(relStrings).containsIgnoringCase("born");
        
        final Set<GraphDocument.Node> nodes = graphDocument.getNodes();
        assertThat(nodes).hasSize(3);
        final String nodesStrings = nodes.stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
        assertThat(nodesStrings).containsIgnoringCase("Matrix");
        assertThat(nodesStrings).containsIgnoringCase("Keanu");
        assertThat(nodesStrings).containsIgnoringCase("Beirut");
    }
    
}
