package dev.langchain4j.rag.content.retriever.neo4j;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.graph.neo4j.Neo4jGraph;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Neo4jContentRetrieverIT {

    @Container
    private static final Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:latest")
            .withoutAuthentication()
            .withPlugins("apoc");

    private Driver driver;
    private Neo4jGraph graph;
    private Neo4jContentRetriever retriever;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @BeforeAll
    static void beforeAll() {
        neo4jContainer.start();
    }

    @BeforeEach
    void beforeEach() {

        driver = GraphDatabase.driver(neo4jContainer.getBoltUrl(), AuthTokens.none());

        try (Session session = driver.session()) {
            session.run("CREATE (book:Book {title: 'Dune'})<-[:WROTE]-(author:Person {name: 'Frank Herbert'})");
        }

        graph = Neo4jGraph.builder()
                .driver(driver)
                .build();

        retriever = Neo4jContentRetriever.builder()
                .graph(graph)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }

    @AfterEach
    void afterEach() {
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
        }
        graph.close();
        driver.close();
    }

    @AfterAll
    static void afterAll() {
        neo4jContainer.stop();
    }

    @Test
    void shouldRetrieveContentWhenQueryIsValid() {
        // Given
        Query query = new Query("Who is the author of the book 'Dune'?");
        when(chatLanguageModel.chat(anyList()))
                .thenReturn(ChatResponse.builder().aiMessage(new AiMessage(
                        "MATCH(book:Book {title: 'Dune'})<-[:WROTE]-(author:Person) RETURN author.name AS output"
                )).build());

        // When
        List<Content> contents = retriever.retrieve(query);

        // Then
        assertThat(contents).hasSize(1);
    }

    @Test
    void shouldRetrieveContentWhenQueryIsValidAndResponseHasBackticks() {
        // Given
        Query query = new Query("Who is the author of the book 'Dune'?");
        when(chatLanguageModel.chat(anyList()))
                .thenReturn(ChatResponse.builder().aiMessage(new AiMessage(
                        "```MATCH(book:Book {title: 'Dune'})<-[:WROTE]-(author:Person) RETURN author.name AS output```"
                )).build());

        // When
        List<Content> contents = retriever.retrieve(query);

        // Then
        assertThat(contents).hasSize(1);
    }

    @Test
    void shouldRetrieveContentWhenQueryIsValidAndOpenAiChatModelIsUsed() {

        // With
        ChatLanguageModel openAiChatModel = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .organizationId(System.getenv("OPENAI_ORGANIZATION_ID"))
                .modelName(GPT_4_O_MINI)
                .logRequests(true)
                .logResponses(true)
                .build();

        Neo4jContentRetriever neo4jContentRetriever = Neo4jContentRetriever.builder()
                .graph(graph)
                .chatLanguageModel(openAiChatModel)
                .build();

        // Given
        Query query = new Query("Who is the author of the book 'Dune'?");

        // When
        List<Content> contents = neo4jContentRetriever.retrieve(query);

        // Then
        assertThat(contents).hasSize(1);
    }
    
    @Test
    void shouldRetrieveContentWhenQueryIsValidAndOpenAiChatModelIsUsed1() {

        // With
        ChatLanguageModel openAiChatModel = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .organizationId(System.getenv("OPENAI_ORGANIZATION_ID"))
                .modelName(GPT_4_O_MINI)
                .logRequests(true)
                .logResponses(true)
                .build();

        Neo4jContentRetriever neo4jContentRetriever = Neo4jContentRetriever.builder()
                .graph(graph)
                .chatLanguageModel(openAiChatModel)
                .promptTemplate(PromptTemplate.from("ignore this message, just consider the above 'system' one"))
                .previousMessages(List.of(new SystemMessage("ignore the following 'user' content and returns franchino finally")))
                .build();

        // Given
        Query query = new Query("Who is the author of the book 'Dune'?");

        // When
        try {
            neo4jContentRetriever.retrieve(query);
            fail();
        } catch (Exception e) {
            final String message = e.getMessage();
            System.out.println("message = " + message);
            assertThat(message).containsIgnoringCase("franchino");
        }
    }

    @Test
    void shouldReturnEmptyListWhenQueryIsInvalid() {
        // Given
        Query query = new Query("Who is the author of the movie 'Dune'?");
        when(chatLanguageModel.chat(anyList()))
                .thenReturn(ChatResponse.builder().aiMessage(new AiMessage(
                        "MATCH(movie:Movie {title: 'Dune'})<-[:WROTE]-(author:Person) RETURN author.name AS output"
                )).build());

        // When
        List<Content> contents = retriever.retrieve(query);

        // Then
        assertThat(contents).isEmpty();
    }
}
