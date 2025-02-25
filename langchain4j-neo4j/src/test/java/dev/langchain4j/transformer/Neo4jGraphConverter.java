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
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.Neo4jTestUtils.getNeo4jContainer;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static dev.langchain4j.store.graph.neo4j.Neo4jGraph.BASE_ENTITY_LABEL;
import static org.assertj.core.api.Assertions.assertThat;


@Testcontainers
public class Neo4jGraphConverter {

    public static final String CAT_ON_THE_TABLE = "Sylvester the cat is on the table";
    public static final String KEANU_REEVES_ACTED = "Keanu Reeves acted in Matrix";
    public static final String START_NODE_REGEX = "(?i)Sylvester|Keanu";
    public static final String END_NODE_REGEX = "(?i)table|matrix";
    private static LLMGraphTransformer graphTransformer;
    private static List<GraphDocument> graphDocs;
    private static Neo4jGraph graph;
    private static Driver driver;
    private static ChatLanguageModel model;
    
    @Container
    static Neo4jContainer<?> neo4jContainer = getNeo4jContainer()
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

        graphTransformer = LLMGraphTransformer.builder()
                .model(model)
                .build();
        graph = Neo4jGraph.builder()
                .driver(driver)
                .build();


        // given
        Document docCat = new DefaultDocument(CAT_ON_THE_TABLE, Metadata.from("key2", "value2"));
        Document docKeanu = new DefaultDocument(KEANU_REEVES_ACTED, Metadata.from("key33", "value3"));
        final List<Document> documents = List.of(docCat, docKeanu);
        // TODO - retry util??
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

        // when
        graph.addGraphDocuments(graphDocs, false, false);

        // then
        final List<Record> records = graph.executeRead("MATCH p=()-[]->() RETURN p");
        assertThat(records).hasSize(2);
        records.forEach(record -> {
            final PathValue p = (PathValue) record.get("p");
            final Path path = p.asPath();
            assertThat(path).hasSize(1);
            final Node start = path.start();
            assertNodeWithoutBaseEntityLabel(start, START_NODE_REGEX);
            final Node end = path.end();
            assertNodeWithoutBaseEntityLabel(end, END_NODE_REGEX);
            final List<Relationship> rels = Iterables.asList(path.relationships());
            assertThat(rels).hasSize(1);
        });

        // todo - re-execute graph.addGraphDocuments(graphDocs, true, true); and see what happens 
    }

    @Test
    void testAddGraphDocumentsWithBaseEntityLabel() {
        // when
        graph.addGraphDocuments(graphDocs, false, true);

        // then
        final List<Record> records = graph.executeRead("MATCH p=()-[]->() RETURN p");
        assertThat(records).hasSize(2);
        records.forEach(record -> {
            final PathValue p = (PathValue) record.get("p");
            final Path path = p.asPath();
            assertThat(path).hasSize(1);
            final Node start = path.start();
            assertNodeWithBaseEntityLabel(start, START_NODE_REGEX);
            final Node end = path.end();
            assertNodeWithBaseEntityLabel(end, END_NODE_REGEX);
            final List<Relationship> rels = Iterables.asList(path.relationships());
            assertThat(rels).hasSize(1);
        });

        // todo - re-execute graph.addGraphDocuments(graphDocs, true, true); and see what happens 
    }
    
    @Test
    void testAddGraphDocumentsWithBaseEntityLabelAndIncludeSource() {
        // when
        graph.addGraphDocuments(graphDocs, true, true);

        // then
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
            assertNodeWithBaseEntityLabel(start, START_NODE_REGEX);

            start = iterator.next();
            assertNodeWithBaseEntityLabel(start, END_NODE_REGEX);
            final List<Relationship> rels = Iterables.asList(path.relationships());
            assertThat(rels).hasSize(2);
            assertThat(rels.get(1).type()).isNotEqualTo("MENTIONS");
        });
        
        // todo - re-execute graph.addGraphDocuments(graphDocs, true, true); and see what happens 
    }

    @Test
    void testAddGraphDocumentsWithIncludeSource() {
        // when
        graph.addGraphDocuments(graphDocs, true, false);

        // then
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
            assertNodeWithoutBaseEntityLabel(start, START_NODE_REGEX);

            start = iterator.next();
            assertNodeWithoutBaseEntityLabel(start, END_NODE_REGEX);
            final List<Relationship> rels = Iterables.asList(path.relationships());
            assertThat(rels).hasSize(2);
            assertThat(rels.get(1).type()).isNotEqualTo("MENTIONS");
        });

        // todo - re-execute graph.addGraphDocuments(graphDocs, true, true); and see what happens 
    }

    private static void assertNodeWithoutBaseEntityLabel(Node start, String propRegex) {
        final Iterable<String> labels = start.labels();
        assertThat(labels).hasSize(1);
        assertThat(labels).doesNotContain(BASE_ENTITY_LABEL);
        assertNodeProps(start, propRegex);
    }

    private static void assertNodeWithBaseEntityLabel(Node start, String propRegex) {
        final Iterable<String> labels = start.labels();
        assertThat(labels).hasSize(2);
        assertThat(labels).contains(BASE_ENTITY_LABEL);
        assertNodeProps(start, propRegex);
    }

    private static void assertNodeProps(Node start, String propRegex) {
        final Map<String, Object> map = start.asMap();
        assertThat(map).hasSize(1);
        assertThat((String) map.get("id")).containsPattern(propRegex);
    }

    private static void extractedDocument(Node start) {
        final Map<String, Object> map = start.asMap();
        assertThat(map).containsKey("id");
        final Object text = map.get("text");
        if (text.equals(CAT_ON_THE_TABLE)) {
            assertThat(map.get("key2")).isEqualTo("value2");
        } else if (text.equals(KEANU_REEVES_ACTED)){
            assertThat(map.get("key33")).isEqualTo("value3");
        } else {
            throw new RuntimeException("The text property is invalid: " + text);
        }
    }
}
