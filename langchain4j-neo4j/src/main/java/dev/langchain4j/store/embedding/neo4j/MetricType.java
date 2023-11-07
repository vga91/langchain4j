package dev.langchain4j.store.embedding.neo4j;

/**
 * Similarity metric used by Neo4j
 */
enum MetricType {

    /**
     * cosine similarity
     */
    COSINE,

    /**
     * inner product
     */
    IP,

    /**
     * euclidean distance
     */
    L2
}
