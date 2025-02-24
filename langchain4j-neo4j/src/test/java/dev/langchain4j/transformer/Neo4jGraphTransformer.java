package dev.langchain4j.transformer;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.graph.neo4j.Neo4jGraph;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.util.Iterables;
import org.neo4j.driver.internal.value.PathValue;
import org.neo4j.driver.internal.value.StringValue;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static dev.langchain4j.store.graph.neo4j.Neo4jGraph.BASE_ENTITY_LABEL;
import static org.assertj.core.api.Assertions.assertThat;

// TODO - mettere i test con Neo4j, mentre nell'altro quelli solo di graphTransformer
// todo - chiamarlo converter or roba del genere

@Testcontainers
public class Neo4jGraphTransformer {

    private static LLMGraphTransformer graphTransformer;
    private static List<GraphDocument> graphDocs;
    private static Neo4jGraph graph;
    private static Driver driver;
    private static ChatLanguageModel model;
    
    // TODO... 
    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.26"))
            .withoutAuthentication()
            .withPlugins("apoc");
    
    @BeforeAll
    static void beforeEach() {
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
        
        
        Document docCat = new DefaultDocument("Sylvester the cat is on the table", Metadata.from("key2", "value2"));
        Document docKeanu = new DefaultDocument("Keanu Reeves acted in Matrix", Metadata.from("key33", "value3"));
        final List<Document> documents = List.of(docCat, docKeanu);
        // TODO -
        graphDocs = graphTransformer.convertToGraphDocuments(documents);
        assertThat(graphDocs.size()).isEqualTo(2);
    }

    @AfterEach
    void afterEach() {
        graph.executeWrite("MATCH (n) DETACH DELETE n");
    }

    @AfterAll
    static void afterAll() {
        graph.close();
    }
    
    // TODO - tests

    @Test
    void testAddGraphDocuments() {
//        Document doc2 = new DefaultDocument("Sylvester the cat is on the table", Metadata.from("key2", "value2"));
//        Document doc3 = new DefaultDocument("Keanu Reeves acted in Matrix", Metadata.from("key3", "value3"));
//        final List<Document> documents = List.of(doc2, doc3);
//        List<GraphDocument> graphDocs = graphTransformer.convertToGraphDocuments(documents);

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
//        Document docCat = new DefaultDocument("Sylvester the cat is on the table", Metadata.from("key2", "value2"));
//        Document docKeanu = new DefaultDocument("Keanu Reeves acted in Matrix", Metadata.from("key33", "value3"));
//        final List<Document> documents = List.of(docCat, docKeanu);

        // TODO - maybe metadata stored here in GraphDocument?
//        List<GraphDocument> graphDocs = graphTransformer.convertToGraphDocuments(documents);

        graph.addGraphDocuments(graphDocs, false, true);

        final List<Record> records = graph.executeRead("MATCH p=()-[]->() RETURN p");
        assertThat(records).hasSize(2);
        records.forEach(record -> {
            final PathValue p = (PathValue) record.get("p");
            final Path path = p.asPath();
            assertThat(path).hasSize(1);
            final Node start = path.start();
            extracted(start);
            final Node end = path.end();
            extracted(end);
            final List<Relationship> rels = Iterables.asList(path.relationships());
            assertThat(rels).hasSize(1);
        });
    }
    
    // TODO - deignore
    @Test
    void testAddGraphDocumentsWithBaseEntityLabelAndIncludeSource() {
//        Document docCat = new DefaultDocument("Sylvester the cat is on the table", Metadata.from("key2", "value2"));
//        Document docKeanu = new DefaultDocument("Keanu Reeves acted in Matrix", Metadata.from("key33", "value3"));
//        final List<Document> documents = List.of(docCat, docKeanu);

        // TODO - maybe metadata stored here in GraphDocument?
//        List<GraphDocument> graphDocs = graphTransformer.convertToGraphDocuments(documents);

        graph.addGraphDocuments(graphDocs, true, true);

        final List<Record> records = graph.executeRead("MATCH p=(:Document)-[:MENTIONS]->()-[]->() RETURN p");
        assertThat(records).hasSize(2);
        records.forEach(record -> {
            final PathValue p = (PathValue) record.get("p");
            final Path path = p.asPath();
            assertThat(path).hasSize(2);
            final Iterator<Node> iterator = path.nodes().iterator();
            Node start = iterator.next();
            assertThat(start.labels()).hasSize(1);
            extractedDocument(start);

            start = iterator.next();
            extracted(start);
//            final Node end = path.end();

            start = iterator.next();
            extracted(start);
            final List<Relationship> rels = Iterables.asList(path.relationships());
            assertThat(rels).hasSize(2);
            assertThat(rels.get(1).type()).isNotEqualTo("MENTIONS");
        });

//        final List<Record> document = graph.executeRead("MATCH (n:Document) RETURN n");
//        assertThat(document.size()).isEqualTo(1);
//
//        final NodeValue nodeValue = (NodeValue) document.get(0).get("n");
//        final Map<String, Object> map = nodeValue.asNode().asMap();
//        // TODO - assertions
//        System.out.println("map = " + map);
        
        // TODO - additional document, check that another document is added
        
    }

    private static void extractedDocument(Node start) {
        assertThat(start.asMap()).containsKey("id");
        assertThat(start.asMap()).containsKey("text");
        assertThat(start.asMap()).containsAnyOf(
                new AbstractMap.SimpleEntry("key2", "value2"),
                new AbstractMap.SimpleEntry("key33", "value3")
        );
    }

    private static void extracted(Node start) {
        assertThat(start.labels()).hasSize(2);
        assertThat(start.labels()).contains(BASE_ENTITY_LABEL);
    }
}
