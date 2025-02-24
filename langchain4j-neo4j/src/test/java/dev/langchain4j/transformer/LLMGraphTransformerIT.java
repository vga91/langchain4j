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
import org.jetbrains.annotations.NotNull;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static dev.langchain4j.store.graph.neo4j.Neo4jGraph.BASE_ENTITY_LABEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

// TODO - rimuovere da qui
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
        final List<ChatMessage> prompt = List.of(new UserMessage("just return a null value, don't add any explanation or extra text."));
        
        final LLMGraphTransformer build = LLMGraphTransformer.builder()
                .model(model)
                .prompt(prompt)
                .build();

        Document doc3 = new DefaultDocument("Keanu Reeves acted in Matrix. Keanu was born in Beirut", Metadata.from("key3", "value3"));
        final List<GraphDocument> documents = build.convertToGraphDocuments(List.of(doc3));
        assertThat(documents).isEmpty();
    }

    @Test
    void testAddGraphDocumentsWithCustomNodesAndRelationshipsSchema() {

        Document docCat = new DefaultDocument("Sylvester the cat is on the table", Metadata.from("key2", "value2"));
        Document docKeanu = new DefaultDocument("Keanu Reeves acted in Matrix", Metadata.from("key33", "value3"));
        Document docLino = new DefaultDocument("Lino Banfi acted in Vieni Avanti Cretino", Metadata.from("key33", "value3"));
        Document docGoku = new DefaultDocument("Goku acted in Dragon Ball", Metadata.from("key33", "value3"));
        Document docHajime = new DefaultDocument("Hajime Isayama wrote Attack On Titan. Levi acted in Attack On Titan", Metadata.from("key33", "value3"));

        final List<Document> docs = List.of(docCat, docKeanu, docLino, docGoku, docHajime);
        
        String cat = "Sylvester the cat";
        String keanu = "Keanu Reeves";
        String lino = "Lino Banfi";
        String goku = "Goku";
        String hajime = "Hajime Isayama";
        String levi = "Levi";
        String table = "table";
        String matrix = "Matrix";
        String vac = "Vieni Avanti Cretino";
        String db = "Dragon Ball";
        String aot = "Attack On Titan";
        
        final LLMGraphTransformer build2 = LLMGraphTransformer.builder()
                .model(model)
                .build();
        final List<GraphDocument> documents2 = build2.convertToGraphDocuments(docs);
        System.out.println("documents2 = " + documents2);
        final Stream<String> cat2 = Stream.of(cat, keanu, lino, goku, hajime, levi, table, matrix, vac, db, aot);
        assertThat(documents2).hasSize(5);
        extracted(documents2, cat2);

        //assertThat(collect).isEqualTo(cat1);

        final LLMGraphTransformer build = LLMGraphTransformer.builder()
                .model(model)
                .allowedNodes(List.of("Person"))
                .allowedRelationships(List.of("Acted_in"))
                .build();

        final List<GraphDocument> documents = build.convertToGraphDocuments(docs);
        System.out.println("documents = " + documents);
        assertThat(documents).hasSize(4);
        final String[] strings = {keanu, lino, goku, levi, matrix, vac, db, aot};
        extracted(documents, Stream.of(strings));
        //assertThat(collect2).isEqualTo(cat12);

        final LLMGraphTransformer build3 = LLMGraphTransformer.builder()
                .model(model)
                .allowedNodes(List.of("Person"))
                .allowedRelationships(List.of("Writes", "Acted_in"))
                .build();


        final List<GraphDocument> documents3 = build3.convertToGraphDocuments(docs);
        System.out.println("documents3 = " + documents3);
        assertThat(documents).hasSize(4);
        final String[] elements3 = {keanu, lino, goku, hajime, levi, matrix, vac, db, aot};

        extracted(documents3, Stream.of(elements3));

        //assertThat(collect2).containsExactly(keanu, lino, goku, hajime, levi, matrix, vac, db, aot);


    }

    private static void extracted(List<GraphDocument> documents3, Stream<String> expectedElements) {
        final List<String> collect23 = getCollect(documents3);
        final List<String> cat123 = expectedElements
                .sorted()
                .toList();
        extracted(collect23, cat123);
        
        // TODO - relationships assertions?
    }

    // todo - rename
    private static void extracted(List<String> collect, List<String> cat1) {
        System.out.println("collect = " + collect);
        assertThat(collect.size()).isEqualTo(cat1.size());
        for (int i = 0; i < collect.size(); i++) {
            assertThat(collect.get(i)).contains(cat1.get(i));
        }
    }

    // todo - rename
    private static List<String> getCollect(List<GraphDocument> documents2) {
        return documents2.stream()
                .flatMap(i -> i.getNodes().stream().map(GraphDocument.Node::getId))
                .sorted()
                .collect(Collectors.toList());
    }


    @Test
    void testAddGraphDocumentsWithDeDuplication() {
        Document doc3 = new DefaultDocument("Keanu Reeves acted in Matrix. Keanu was born in Beirut", Metadata.from("key3", "value3"));
        // TODO
        // final List<Document> documents = List.of(doc2, doc3);

        final List<Document> documents = List.of(doc3);
        List<GraphDocument> graphDocs = graphTransformer.convertToGraphDocuments(documents);

        // TODO - change assertions
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
