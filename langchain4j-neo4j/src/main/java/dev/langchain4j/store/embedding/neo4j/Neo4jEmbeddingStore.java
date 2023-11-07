package dev.langchain4j.store.embedding.neo4j;

import com.google.gson.Gson;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.Builder;
import org.neo4j.cypherdsl.support.schema_name.SchemaNames;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static dev.langchain4j.internal.Utils.isCollectionEmpty;
import static dev.langchain4j.internal.Utils.randomUUID;
import static dev.langchain4j.internal.ValidationUtils.*;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;

/**
 * TODO: Represents a <a href="https://neo4j.io/">Neo4j</a> index as an embedding store.
 * TODO - RIGHT? Current implementation assumes the index uses the cosine distance metric.
 */


// TODO - setVectorProperty first or after createIndex???
    
public class Neo4jEmbeddingStore implements EmbeddingStore<TextSegment> {


    private static final Logger log = LoggerFactory.getLogger(Neo4jEmbeddingStore.class);
    private static final Gson GSON = new Gson();
    // TODO ??
    private static final String DEFAULT_EMBEDDING_PROPERTY = "embedding";
    public static final String EMBEDDING_PROP = "embeddingProp";
    public static final String ID_PROP = "idProp";
    public static final String PROPS = "props";

    // TODO - maybe in Neo4jSchema?
    private final Driver driver;
//    private final Session session;
//    private final SessionConfig sessionConfig;
    private final Neo4jSchema schema;


    @Builder.Default
    private SessionConfig config = SessionConfig.forDatabase("neo4j");
    
    /**
     * Creates an instance of Neo4jEmbeddingStore
     *
//     * @param host               Neo4j Stack Server host
//     * @param port               Neo4j Stack Server port
     * @param user               Neo4j Stack username (optional)
     * @param password           Neo4j Stack password (optional)
     * @param dimension          embedding vector dimension
     * @param metadataFieldsName metadata fields name (optional)
     */
    @Builder
    public Neo4jEmbeddingStore(
            // TODO - MAYBE CHANGE NAME..
//            String host,
//            Integer port,
            String user,
            String password,
            SessionConfig config,
            Driver driver,
            // todo - useful?
            Integer dimension,
            String label,
            String property,
            List<String> metadataFieldsName,
            String indexName) {
        ensureNotNull(driver, "driver");
//        ensureNotBlank(host, "host");
//        ensureNotNull(port, "port");
        ensureNotNull(dimension, "dimension");

        this.driver = driver;
        
//        user = getValOrDefault(user);
//        dbName = getValOrDefault(dbName);

//        this.driver = GraphDatabase.driver(host, AuthTokens.basic(user, password));
//        this.sessionConfig = SessionConfig.forDatabase(dbName);

//        this.session = driver.session(sessionConfig);

//        TODO - DECOMMENT label = SchemaNames.sanitize(label).orElse("Document");
        this.schema = Neo4jSchema.builder()
                .dimension(dimension)
                .metadataFieldsName(metadataFieldsName)
                .label(label)
                .embeddingProperty(property)
                .indexName(indexName)
                .build();
        
        createIndexIfNotExist();
    }

//    public static Builder builder() {
//        return new Builder();
//    }

    private <T> T withSession(Function<Session, T> function) {
        try (var session = session()) {
            return function.apply(session);
        }
    }

    private void withEmptySession(Consumer<Session> function) {
        try (Session session = session()) {
            function.accept(session);
        }
    }

    private Session session() {
        return this.driver.session(this.config);
    }

    private static String getValOrDefault(String user) {
        if (user == null) {
            return "neo4j";
        }
        return user;
    }

    @Override
    public String add(Embedding embedding) {
        String id = randomUUID();
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        addInternal(id, embedding, null);
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = randomUUID();
        addInternal(id, embedding, textSegment);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = embeddings.stream()
                .map(ignored -> randomUUID())
                .collect(toList());
        addAllInternal(ids, embeddings, null);
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
        List<String> ids = embeddings.stream()
                .map(ignored -> randomUUID())
                .collect(toList());
        addAllInternal(ids, embeddings, embedded);
        return ids;
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {
        
        
//        // Using KNN query on @vector field
//        String queryTemplate = "*=>[ KNN %d @%s $BLOB AS %s ]";
//        List<String> returnFields = new ArrayList<>(schema.getMetadataFieldsName());
//        returnFields.addAll(asList(schema.getVectorFieldName(), schema.getScalarFieldName(), SCORE_FIELD_NAME));
//        Query query = new Query(format(queryTemplate, maxResults, schema.getVectorFieldName(), SCORE_FIELD_NAME))
//                .addParam("BLOB", ToByteArray(referenceEmbedding.vector()))
//                .returnFields(returnFields.toArray(new String[0]))
//                .setSortBy(SCORE_FIELD_NAME, true)
//                .dialect(2);
//
//        SearchResult result = client.ftSearch(schema.getIndexName(), query);
//        List<Document> documents = result.getDocuments();
//
//        return toEmbeddingMatch(documents, minScore);
        
        
        return null;
    }

    // todo - maybe create index after MERGE nodes
    private void createIndex(String indexName) {
        // merge and create vector property
        
        // es: "CALL db.index.vector.createNodeIndex('abstract-embeddings', 'Abstract', 'embedding', 1536, 'cosine')"

        Map<String, Object> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("indexName", indexName);
        objectObjectHashMap.put("label", schema.getLabel());
        objectObjectHashMap.put("embeddingProperty", schema.getEmbeddingProperty());
        objectObjectHashMap.put("embeddingDimension", schema.getDimension());
        objectObjectHashMap.put("distanceType", schema.getDistanceType().getValue());
        
        // create vector index
        withEmptySession(session -> {
            session.run("CALL db.index.vector.createNodeIndex($indexName, $label, $embeddingProperty, $embeddingDimension, $distanceType)",
                    objectObjectHashMap);
        });
        
//        IndexDefinition indexDefinition = new IndexDefinition(JSON);
//        indexDefinition.setPrefixes(schema.getPrefix());
//        String res = client.ftCreate(indexName, FTCreateParams.createParams()
//                .on(IndexDataType.JSON)
//                .addPrefix(schema.getPrefix()), schema.toSchemaFields());
//        if (!"OK".equals(res)) {
//            if (log.isErrorEnabled()) {
//                log.error("create index error, msg={}", res);
//            }
//            throw new Neo4jRequestFailedException("create index error, msg=" + res);
//        }
    }

    // TODO - TEST IT.
    private boolean isIndexExist(String indexName) {
        Map<String, Object> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("name", indexName);
        return withSession(session -> {
            boolean name = session.run("SHOW INDEX WHERE type = 'VECTOR' AND name = $name",
                                    objectObjectHashMap)
                            .hasNext();
            System.out.println("name = " + name);
                    return name;
                }
        );
    }

    private void addInternal(String id, Embedding embedding, TextSegment embedded) {
        addAllInternal(singletonList(id), singletonList(embedding), embedded == null ? null : singletonList(embedded));
    }

    private void addAllInternal(List<String> ids, List<Embedding> embeddings, List<TextSegment> embedded) {
        if (isCollectionEmpty(ids) || isCollectionEmpty(embeddings)) {
            log.info("[do not add empty embeddings to neo4j]");
            return;
        }
        ensureTrue(ids.size() == embeddings.size(), "ids size is not equal to embeddings size");
        ensureTrue(embedded == null || embeddings.size() == embedded.size(), "embeddings size is not equal to embedded size");

//        try {
//            createIndexIfNotExist(embeddings.get(0).dimensions());

            bulk(ids, embeddings, embedded);
//        } catch (IOException e) {
//            log.error("[ElasticSearch encounter I/O Exception]", e);
//            throw new Neo4jRequestFailedException(e.getMessage());
//        }
        
//        Pipeline pipeline = client.pipelined();
//
//        int size = ids.size();
//        for (int i = 0; i < size; i++) {
//            String id = ids.get(i);
//            Embedding embedding = embeddings.get(i);
//            TextSegment textSegment = embedded == null ? null : embedded.get(i);
//            Map<String, Object> fields = new HashMap<>();
//            fields.put(schema.getVectorFieldName(), embedding.vector());
//            if (textSegment != null) {
//                // do not check metadata key is included in Neo4jSchema#metadataFieldsName
//                fields.put(schema.getScalarFieldName(), textSegment.text());
//                fields.putAll(textSegment.metadata().asMap());
//            }
//            String key = schema.getPrefix() + id;
//            pipeline.jsonSetWithEscape(key, Path2.of("$"), fields);
//        }
//        List<Object> responses = pipeline.syncAndReturnAll();
//        Optional<Object> errResponse = responses.stream().filter(response -> !"OK".equals(response)).findAny();
//        if (errResponse.isPresent()) {
//            if (log.isErrorEnabled()) {
//                log.error("add embedding failed, msg={}", errResponse.get());
//            }
//            throw new Neo4jRequestFailedException("add embedding failed, msg=" + errResponse.get());
//        }
    }

    private void bulk(List<String> ids, List<Embedding> embeddings, List<TextSegment> embedded) {
        int size = ids.size();

        List<Map<String, Object>> rows = IntStream.range(0, ids.size())
                .mapToObj(i -> toRecord(i, ids, embeddings, embedded))
                .collect(toList());
        
        withEmptySession(session -> {
            // TODO : MAYBE IN THIS WAY, no...?
            /*
            												UNWIND $rows AS row
						MERGE (u:%1$s {id: row.2$s})
						ON CREATE
							SET u += row.%2$s
						ON MATCH
							SET u.2$s = row.id,
								u += row.%2$s
						WITH row, u
						CALL db.create.setVectorProperty(u, $embeddingProperty, row.%3$s)
						RETURN *
             */
            String statement = String.format("UNWIND $rows AS row\n" +
                             "MERGE (u:%1$s {id: row.2$s})\n" +
                             "SET u += row.%3$s\n" +
                             "WITH row, u\n" +
                             "CALL db.create.setVectorProperty(u, $embeddingProperty, row.%4$s)\n" +
                             "RETURN *",
                            this.schema.getLabel(),
                    ID_PROP,
                    PROPS,
                    EMBEDDING_PROP);

            Map<String, Object> map = new HashMap<>();
            map.put("rows", rows);
            map.put("embeddingProperty", this.schema.getEmbeddingProperty());
            
            session.run(statement,
                            map)
                    .consume();
        });

    }

    private Map<String, Object> toRecord(int i, List<String> ids, List<Embedding> embeddings, List<TextSegment> embedded) {
        String id = ids.get(i);
        Embedding embedding = embeddings.get(i);
        TextSegment segment = embedded.get(i);
        
        Map<String, Object> row = new HashMap<>();
        row.put(ID_PROP, id);

        Map<String, Object> properties = new HashMap<>();
        properties.put("text", segment.text());
        
        Map<String, String> metadata = segment.metadata().asMap();
        metadata.forEach((k,v) -> properties.put("metadata." + k, Values.value(v)));
        

        // TODO - dimensions what is??...
        // TODO - ??
        row.put(EMBEDDING_PROP, Values.value(embedding.vector()));
        row.put(PROPS, properties);
        return row;
    }

    private void createIndexIfNotExist() {
        String idxName = schema.getIndexName();
        if (!isIndexExist(idxName)) {
            createIndex(idxName);
        }
    }

//    private List<EmbeddingMatch<TextSegment>> toEmbeddingMatch(List<Document> documents, double minScore) {
//        if (documents == null || documents.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        return documents.stream()
//                .map(document -> {
//                    double score = (2 - Double.parseDouble(document.getString(SCORE_FIELD_NAME))) / 2;
//                    String id = document.getId().substring(schema.getPrefix().length());
//                    String text = document.hasProperty(schema.getScalarFieldName()) ? document.getString(schema.getScalarFieldName()) : null;
//                    TextSegment embedded = null;
//                    if (text != null) {
//                        List<String> metadataFieldsName = schema.getMetadataFieldsName();
//                        Map<String, String> metadata = metadataFieldsName.stream()
//                                .filter(document::hasProperty)
//                                .collect(Collectors.toMap(metadataFieldName -> metadataFieldName, document::getString));
//                        embedded = new TextSegment(text, new Metadata(metadata));
//                    }
//                    Embedding embedding = new Embedding(GSON.fromJson(document.getString(schema.getVectorFieldName()), float[].class));
//                    return new EmbeddingMatch<>(score, id, embedding, embedded);
//                })
//                .filter(embeddingMatch -> embeddingMatch.score() >= minScore)
//                .collect(toList());
//    }

//    public static Neo4jVectorStoreConfig builder() {
//        return new Neo4jVectorStoreConfig();
//    }

//    @Override
//    public void close() throws Exception {
//        
//    }

    
    
/*
	/**
	 * Configuration for the Neo4j vector store.
    TODO: this in the spring ai
public static final class Neo4jVectorStoreConfig {

    private final SessionConfig sessionConfig;

    private final int embeddingDimension;

    private final Neo4jDistanceType distanceType;

    private final String label;

    private final String embeddingProperty;

    private final String quotedLabel;    
 */
    
    
    // TODO - forse si può fare con l'annotation @Builder    
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @Builder
//    public static class Builder {
//
//        private String host;
//        private Integer port;
//        private String user;
//        private String password;
//        private String dbName;
//        private Integer dimension;
//        
//        // TODO ????
//        private List<String> metadataFieldsName = new ArrayList<>();
//
//        /**
//         * @param host Neo4j Stack host
//         */
//        public Builder host(String host) {
//            this.host = host;
//            return this;
//        }
//
//        /**
//         * @param port Neo4j Stack port
//         */
//        public Builder port(Integer port) {
//            this.port = port;
//            return this;
//        }
//
//        /**
//         * @param user Neo4j Stack username (optional)
//         */
//        public Builder user(String user) {
//            this.user = user;
//            return this;
//        }
//
//        /**
//         * @param password Neo4j Stack password (optional)
//         */
//        public Builder password(String password) {
//            this.password = password;
//            return this;
//        }
//
//        public Builder dbName(String dbName) {
//            this.dbName = dbName;
//            return this;
//        }
//
//        public Builder label(String dbName) {
//            this.dbName = dbName;
//            return this;
//        }
//
//        /**
//         * @param dimension embedding vector dimension
//         * @return builder
//         */
//        public Builder dimension(Integer dimension) {
//            this.dimension = dimension;
//            return this;
//        }
//
//        /**
//         * @param metadataFieldsName metadata fields name (optional)
//         */
//        public Builder metadataFieldsName(List<String> metadataFieldsName) {
//            this.metadataFieldsName = metadataFieldsName;
//            return this;
//        }
//
////        @SuperBuilder
//        public Neo4jEmbeddingStore build() {
//            return new Neo4jEmbeddingStore(host, port, user, dbName, password, dimension, metadataFieldsName);
//        }
//    }
}
