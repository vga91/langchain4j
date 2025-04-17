---
sidebar_position: 16
---

# Neo4j

[Neo4j](https://neo4j.com/) is a high-performance, open-source graph database designed for managing connected data.
Neo4j's native graph model is ideal for modelling complex and highly interconnected domains, like social graphs, recommendation systems, and knowledge networks.
With its integration in LangChain4j, the [Neo4j Vector](https://github.com/neo4j-documentation/labs-pages/blob/publish/modules/genai-ecosystem/pages/vector-search.adoc) capabilities can be used in the Langchain4j library.

## Maven Dependency
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-community-neo4j</artifactId>
    <version>1.0.0-beta3</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-community-neo4j-retriever</artifactId>
    <version>1.0.0-beta3</version>
</dependency>
```
## APIs
LangChain4j provides the following classes for Neo4j integration:
- `Neo4jEmbeddingStore`: Implements the EmbeddingStore interface, enabling storing and querying vector embeddings in a Neo4j database.
- `Neo4jContentRetriever`: Supports retrieving original content associated with stored embeddings.

## Usage Example
To create `Neo4jEmbeddingStore` instance, you need to provide proper settings:
```java
// ---> MINIMAL EMBEDDING <---
Neo4jEmbeddingStore minimalEmbedding = Neo4jEmbeddingStore.builder()
    .withBasicAuth(NEO4J_CONNECTION_STRING, USERNAME, ADMIN_PASSWORD)
    .dimension(384)
    .build();
// ---> CUSTOM EMBEDDING <---
Neo4jEmbeddingStore customEmbeddingStore = Neo4jEmbeddingStore.builder()
        .withBasicAuth(NEO4J_CONNECTION_STRING, USERNAME, ADMIN_PASSWORD)
        .dimension(384)
        .indexName(CUSTOM_INDEX)
        .metadataPrefix(CUSTOM_METADATA_PREF)
        .label(CUSTOM_LABEL)
        .embeddingProperty(CUSTOM_PROP)
        .idProperty(CUSTOM_ID)
        .textProperty(CUSTOM_TEXT)
        .build();
```
Then you can add the embeddings in many different ways, and search them:
```java
// ---> ADD MINIMAL EMBEDDING <---
Embedding embedding = embeddingModel.embed("embedText").content();
String id = minimalEmbedding.add(embedding); // output: id of the embedding
// ---> ADD MINIMAL EMBEDDING WITH ID <---
String id = randomUUID();
Embedding embedding = embeddingModel.embed("embedText").content();
minimalEmbedding.add(id, embedding);
// ---> ADD EMBEDDING WITH SEGMENT <---
TextSegment segment = TextSegment.from(randomUUID());
Embedding embedding = embeddingModel.embed(segment.text()).content();
String id = minimalEmbedding.add(embedding, segment);
// ---> ADD EMBEDDING WITH SEGMENT AND METADATA <---
TextSegment segment = TextSegment.from(randomUUID(), Metadata.from(METADATA_KEY, "test-value"));
Embedding embedding = embeddingModel.embed(segment.text()).content();
String id = minimalEmbedding.add(embedding, segment);
// ---> ADD MULTIPLE EMBEDDINGS <---
Embedding firstEmbedding = embeddingModel.embed("firstEmbedText").content();
Embedding secondEmbedding = embeddingModel.embed("secondEmbedText").content();
List<String> ids = minimalEmbedding.addAll(asList(firstEmbedding, secondEmbedding));
// ---> ADD MULTIPLE EMBEDDINGS WITH SEGMENTS <---
TextSegment firstSegment = TextSegment.from("firstText");
Embedding firstEmbedding = embeddingModel.embed(firstSegment.text()).content();
TextSegment secondSegment = TextSegment.from("secondText");
Embedding secondEmbedding = embeddingModel.embed(secondSegment.text()).content();
List<String> ids = minimalEmbedding.addAll(
        asList(firstEmbedding, secondEmbedding),
        asList(firstSegment, secondSegment)
);
```
Then you can search the stored embeddings:
```java
// ---> SEARCH EMBEDDING WITH MAX RESULTS <---
String id = minimalEmbedding.add(embedding);
final EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
        .queryEmbedding(embedding)
        .maxResults(10)
        .build();
final List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.search(request).matches();
// ---> SEARCH EMBEDDING WITH MIN SCORE <---
Embedding embedding = embeddingModel.embed("embedText").content();
String id = minimalEmbedding.add(embedding);
final EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
        .queryEmbedding(embedding)
        .maxResults(10)
        .minScore(0.15)
        .build();
final List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.search(request).matches();
// ---> SEARCH EMBEDDING WITH CUSTOM METADATA PREFIX <---
String metadataCompleteKey = CUSTOM_METADATA_PREF + METADATA_KEY;
TextSegment segment = TextSegment.from(randomUUID(), Metadata.from(METADATA_KEY, "test-value"));
Embedding embedding = embeddingModel.embed(segment.text()).content();
String id = customEmbeddingStore.add(embedding, segment);
final EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
        .queryEmbedding(embedding)
        .maxResults(10)
        .build();
final List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.search(request).matches();
// ---> SEARCH EMBEDDING WITH CUSTOM ID PROPERTY <---
String metadataCompleteKey = CUSTOM_METADATA_PREF + METADATA_KEY;
TextSegment segment = TextSegment.from(randomUUID(), Metadata.from(METADATA_KEY, "test-value"));
Embedding embedding = embeddingModel.embed(segment.text()).content();
String id = embeddingStore.add(embedding, segment);
final EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(10)
                .build();
final List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.search(request).matches();
// ---> SEARCH MULTIPLE EMBEDDING <---
List<String> ids = minimalEmbedding.addAll(asList(firstEmbedding, secondEmbedding));
final EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
        .queryEmbedding(firstEmbedding)
        .maxResults(10)
        .build();
final List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.search(request).matches();
// ---> SEARCH MULTIPLE EMBEDDING WITH SEGMENTS <---
List<String> ids = minimalEmbedding.addAll(
        asList(firstEmbedding, secondEmbedding),
        asList(firstSegment, secondSegment)
);
final EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
        .queryEmbedding(firstEmbedding)
        .maxResults(10)
        .build();
final List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.search(request).matches();
```

### Simple Flow Examples
The following are a few examples of the use flow for the two APIs.
- `Neo4jEmbeddingStore`:
```java
private static final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

public static void minimalEmbedding() {
    try (Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5.26")) {
        neo4j.start();

        EmbeddingStore<TextSegment> minimalEmbedding = Neo4jEmbeddingStore.builder()
                .withBasicAuth(neo4j.getBoltUrl(), "neo4j", neo4j.getAdminPassword())
                .dimension(384)
                .build();

        System.out.println("Neo4jEmbeddingStoreExample.searchEmbeddingsWithAddAllWithMetadataMaxResultsAndMinScore");

        TextSegment segment1 = TextSegment.from("I like football.", Metadata.from("test-key-1", "test-value-1"));
        Embedding embedding1 = embeddingModel.embed(segment1).content();

        TextSegment segment2 = TextSegment.from("The weather is good today.", Metadata.from("test-key-2", "test-value-2"));
        Embedding embedding2 = embeddingModel.embed(segment2).content();

        TextSegment segment3 = TextSegment.from("I like basketball.", Metadata.from("test-key-3", "test-value-3"));
        Embedding embedding3 = embeddingModel.embed(segment3).content();
        minimalEmbedding.addAll(
                List.of(embedding1, embedding2, embedding3),
                List.of(segment1, segment2, segment3)
        );

        Embedding queryEmbedding = embeddingModel.embed("What are your favourite sports?").content();
        final EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(2)
                .minScore(0.15)
                .build();
        List<EmbeddingMatch<TextSegment>> relevant = minimalEmbedding.search(request).matches();
        relevant.forEach(match -> {
            System.out.println(match.score()); // 0.8144289255142212
            System.out.println(match.embedded().text()); // I like football. || I like basketball.
        });
    }
}

public static void customEmbeddingStore() {
    try (Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5.26")) {
        neo4j.start();
        
        Neo4jEmbeddingStore customEmbeddingStore = Neo4jEmbeddingStore.builder()
                .withBasicAuth(neo4j.getBoltUrl(), "neo4j", neo4j.getAdminPassword())
                .dimension(384)
                .indexName("customidx")
                .label("CustomLabel")
                .embeddingProperty("customProp")
                .idProperty("customId")
                .textProperty("customText")
                .build();
        
        System.out.println("Neo4jEmbeddingStoreExample.searchEmbeddingsWithSingleMaxResult");

        TextSegment segment1 = TextSegment.from("I like football.");
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        customEmbeddingStore.add(embedding1, segment1);

        TextSegment segment2 = TextSegment.from("The weather is good today.");
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        customEmbeddingStore.add(embedding2, segment2);

        Embedding queryEmbedding = embeddingModel.embed("What is your favourite sport?").content();
        final EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(1)
                .build();
        List<EmbeddingMatch<TextSegment>> relevant = customEmbeddingStore.search(request).matches();
        EmbeddingMatch<TextSegment> embeddingMatch = relevant.get(0);

        System.out.println(embeddingMatch.score()); // 0.8144289255142212
        System.out.println(embeddingMatch.embedded().text()); // I like football.
    }
}
```
- `Neo4jContentRetriever`:
```java
    private final ChatLanguageModel chatLanguageModel;

    public void neo4jContentRetriever() {
        try (
                Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5.16.0")
                                                        .withoutAuthentication()
                                                        .withLabsPlugins("apoc")
        ) {
            neo4jContainer.start();
            try (Driver driver = GraphDatabase.driver(neo4jContainer.getBoltUrl(), AuthTokens.none())) {
                try (Neo4jGraph graph = Neo4jGraph.builder().driver(driver).build()) {
                    try (Session session = driver.session()) {
                        session.run("CREATE (book:Book {title: 'Dune'})<-[:WROTE]-(author:Person {name: 'Frank Herbert'})");
                    }
                    graph.refreshSchema();
                    
                    Neo4jContentRetriever retriever = Neo4jContentRetriever.builder()
                            .graph(graph)
                            .chatLanguageModel(chatLanguageModel)
                            .build();

                    Query query = new Query("Who is the author of the book 'Dune'?");

                    List<Content> contents = retriever.retrieve(query);

                    System.out.println(contents.get(0).textSegment().text()); // "Frank Herbert"
                }
            }
        }
    }
```
[Examples Sources](https://github.com/langchain4j/langchain4j-examples/tree/main/neo4j-example/src/main/java)
