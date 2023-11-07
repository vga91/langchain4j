package dev.langchain4j.store.embedding.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
//import neo4j.clients.jedis.search.schemafields.SchemaField;
//import neo4j.clients.jedis.search.schemafields.TextField;
//import neo4j.clients.jedis.search.schemafields.VectorField;
//import neo4j.clients.jedis.search.schemafields.VectorField.VectorAlgorithm;

import java.util.ArrayList;
import java.util.List;

// TODO??
import static dev.langchain4j.store.embedding.neo4j.MetricType.COSINE;
//import static neo4j.clients.jedis.search.schemafields.VectorField.VectorAlgorithm.HNSW;

/**
 * Neo4j Schema Description
 */
@Builder
@AllArgsConstructor
@Data
class Neo4jSchema {


    @AllArgsConstructor
    @Getter
    public enum Neo4jDistanceType {
        COSINE("cosine"), EUCLIDEAN("euclidean");
        private final String value;
    }

    public static final String SCORE_FIELD_NAME = "vector_score";
    private static final String JSON_PATH_PREFIX = "$.";
//    private static final VectorAlgorithm DEFAULT_VECTOR_ALGORITHM = HNSW;
    private static final MetricType DEFAULT_METRIC_TYPE = COSINE;

    /* Neo4j schema field settings */

    @Builder.Default
    private String indexName = "langchain-embedding-index";
    @Builder.Default
    private String prefix = "embedding:";
    @Builder.Default
    private String vectorFieldName = "vector";
    @Builder.Default
    private String scalarFieldName = "text";

    @Builder.Default
    private String embeddingProperty = "embedding";

    // TODO - IS USED ??
    @Builder.Default
    private List<String> metadataFieldsName = new ArrayList<>();

    @Builder.Default
    private Neo4jDistanceType distanceType = Neo4jDistanceType.COSINE;

    /* Vector field settings */

//    @Builder.Default TODO??
//    private VectorAlgorithm vectorAlgorithm = DEFAULT_VECTOR_ALGORITHM;
    private int dimension;

//    @Builder.Default
    private String label = "Document";

    
//    public Neo4jSchema label(String label) {
//        this.label = SchemaNames.sanitize(label).orElseThrow();
//        return this;
//    }
    
    @Builder.Default
    private MetricType metricType = DEFAULT_METRIC_TYPE;

//    public static class Neo4jSchemaBuilder {
//        private String label;
//        public Neo4jSchemaBuilder label(String label ) {
//            this.label = ;
//            return this;
//        }
//    }
    
    public Neo4jSchema(int dimension) {
        this.dimension = dimension;
    }

//    public SchemaField[] toSchemaFields() {
//        Map<String, Object> vectorAttrs = new HashMap<>();
//        vectorAttrs.put("DIM", dimension);
//        vectorAttrs.put("DISTANCE_METRIC", metricType.name());
//        vectorAttrs.put("TYPE", "FLOAT32");
//        vectorAttrs.put("INITIAL_CAP", 5);
//        List<SchemaField> fields = new ArrayList<>();
//        fields.add(TextField.of(JSON_PATH_PREFIX + scalarFieldName).as(scalarFieldName).weight(1.0));
//        fields.add(VectorField.builder()
//                .fieldName(JSON_PATH_PREFIX + vectorFieldName)
//                .algorithm(vectorAlgorithm)
//                .attributes(vectorAttrs)
//                .as(vectorFieldName)
//                .build());
//
//        if (metadataFieldsName != null && !metadataFieldsName.isEmpty()) {
//            for (String metadataFieldName : metadataFieldsName) {
//                fields.add(TextField.of(JSON_PATH_PREFIX + metadataFieldName).as(metadataFieldName).weight(1.0));
//            }
//        }
//        return fields.toArray(new SchemaField[0]);
//    }

//    public String getIndexName() {
//        return indexName;
//    }

    public String getPrefix() {
        return prefix;
    }

    public String getVectorFieldName() {
        return vectorFieldName;
    }

    public String getScalarFieldName() {
        return scalarFieldName;
    }

    public List<String> getMetadataFieldsName() {
        return metadataFieldsName;
    }
}
