package dev.langchain4j.store.embedding.neo4j;

public class Neo4jRequestFailedException extends RuntimeException {

    public Neo4jRequestFailedException() {
        super();
    }

    public Neo4jRequestFailedException(String message) {
        super(message);
    }

    public Neo4jRequestFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
