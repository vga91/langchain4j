package dev.langchain4j.rag.content.retriever.neo4j;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.graph.neo4j.Neo4jGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Type;
import org.neo4j.driver.types.TypeSystem;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

/**
 * A {@link ContentRetriever} that retrieves from an {@link Neo4jGraph}.
 * It converts a natural language question into a Neo4j cypher query and then retrieves relevant {@link Content}s by executing the query on Neo4j.
 */
public class Neo4jContentRetriever implements ContentRetriever {

    // todo - https://github.com/neo4j-labs/text2cypher/blob/main/datasets/synthetic_gemini_demodbs/gemini_text2cypher.ipynb
    private static final String DEFAULT_SYSTEM_MESSAGE = """
            Given an input question, convert it to a Cypher query. No pre-amble.
            Additional instructions:
            - Ensure that queries checking for non-null properties use `IS NOT NULL` in a straightforward manner.
            - Don't use `size((n)--(m))` for counting relationships. Instead use the new `count{(n)--(m))}` syntax.
            - Incorporate the new existential subqueries in examples where the query needs to check for the existence of a pattern.
              Example: MATCH (p:Person)-[r:IS_FRIENDS_WITH]->(friend:Person)
                        WHERE exists{ (p)-[:WORKS_FOR]->(:Company {name: 'Neo4j'})}
                        RETURN p, r, friend
            """;
    
    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from("""
            Based on the Neo4j graph schema below, write a Cypher query that would answer the user's question:
            {{schema}}

            Question: {{question}}
            Cypher query:
            """);

    private static final Pattern BACKTICKS_PATTERN = Pattern.compile("```(.*?)```", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Type NODE = TypeSystem.getDefault().NODE();

    private final Neo4jGraph graph;

    private final ChatLanguageModel chatLanguageModel;

    private final PromptTemplate promptTemplate;
    private final List<ChatMessage> previousMessages;

    @Builder
    public Neo4jContentRetriever(Neo4jGraph graph, ChatLanguageModel chatLanguageModel, PromptTemplate promptTemplate, List<ChatMessage> previousMessages) {

        this.graph = ensureNotNull(graph, "graph");
        this.chatLanguageModel = ensureNotNull(chatLanguageModel, "chatLanguageModel");
        this.promptTemplate = getOrDefault(promptTemplate, DEFAULT_PROMPT_TEMPLATE);
        this.previousMessages = getOrDefault(previousMessages, List.of(new SystemMessage(DEFAULT_SYSTEM_MESSAGE)));
    }

    @Override
    public List<Content> retrieve(Query query) {

        String question = query.text();
        String schema = graph.getSchema();
        String cypherQuery = generateCypherQuery(schema, question);
        List<String> response = executeQuery(cypherQuery);
        return response.stream().map(Content::from).toList();
    }

    private String generateCypherQuery(String schema, String question) {

        Prompt cypherPrompt = promptTemplate.apply(Map.of("schema", schema, "question", question));
        final List<ChatMessage> messages = previousMessages == null
                ? new ArrayList<>()
                : new ArrayList<>(previousMessages);
        messages.add(cypherPrompt.toUserMessage());
        
        String cypherQuery = chatLanguageModel.chat(messages)
                .aiMessage()
                .text();
        Matcher matcher = BACKTICKS_PATTERN.matcher(cypherQuery);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return cypherQuery;
    }

    private List<String> executeQuery(String cypherQuery) {

        List<Record> records = graph.executeRead(cypherQuery);
        return records.stream()
                .flatMap(r -> r.values().stream())
                .map(value -> NODE.isTypeOf(value) ? value.asMap().toString() : value.toString())
                .toList();
    }
}
