package dev.langchain4j.store.graph.neo4j;


import dev.langchain4j.data.document.Document;
import dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingStore;
import dev.langchain4j.transformer.GraphDocument;
import dev.langchain4j.transformer.LLMGraphTransformerUtils;
import lombok.Builder;
import lombok.Getter;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.summary.ResultSummary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingUtils.DEFAULT_DATABASE_NAME;
//import static dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingUtils.DEFAULT_LABEL;
import static dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingUtils.sanitizeOrThrows;
import static dev.langchain4j.transformer.LLMGraphTransformerUtils.generateMD5;
import static dev.langchain4j.transformer.LLMGraphTransformerUtils.removeBackticks;

@Getter
public abstract class BaseNeo4jBuilder {
    // TODO - default label as abstract method
    
    /* default configs */
    public static final String DEFAULT_ID_PROP = "id";
    public static final String DEFAULT_TEXT_PROP = "text";

    /* Neo4j Java Driver settings */
    protected final Driver driver;
    protected final SessionConfig config;
    protected final String databaseName;

    /* Neo4j schema field settings */
    protected final String label;
    protected final String idProperty;
    protected final String textProperty;
    protected final String sanitizedLabel;
    protected final String sanitizedIdProperty;
    protected final String sanitizedTextProperty;

    /**
     * Creates an instance of Neo4jEmbeddingStore defining a {@link Driver}
     * starting from uri, user and password
     */
    // todo - the name is strange, ..BuilderBuilder
//    public static class BaseNeo4jBuilderBuilder {
//        public BaseNeo4jBuilderBuilder withBasicAuth(String uri, String user, String password) {
//            return this.driver(GraphDatabase.driver(uri, AuthTokens.basic(user, password)));
//        }
//    }
    
//    @Builder
    protected BaseNeo4jBuilder(SessionConfig config, String databaseName, Driver driver, String label, String idProperty, String textProperty) {
        /* required configs */
        this.driver = ensureNotNull(driver, "driver");
        this.driver.verifyConnectivity();

        /* optional configs */
        this.databaseName = getOrDefault(databaseName, DEFAULT_DATABASE_NAME);
        this.config = getOrDefault(config, SessionConfig.forDatabase(this.databaseName));
        this.label = getOrDefault(label, getDefaultLabel());
        this.idProperty = getOrDefault(idProperty, DEFAULT_ID_PROP);
        this.textProperty = getOrDefault(textProperty, DEFAULT_TEXT_PROP);

        /* sanitize labels and property names, to prevent from Cypher Injections */
        this.sanitizedLabel = sanitizeOrThrows(this.label, "label");
        this.sanitizedIdProperty = sanitizeOrThrows(this.idProperty, "idProperty");
        this.sanitizedTextProperty = sanitizeOrThrows(this.textProperty, "textProperty");
    }
    
    protected abstract String getDefaultLabel();

    protected Session session() {
        return this.driver.session(this.config);
    }
}
