package dev.langchain4j.store.graph.neo4j;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.transformer.GraphDocument;
import dev.langchain4j.transformer.LLMGraphTransformerUtils;
import lombok.Builder;
import lombok.Getter;
import org.neo4j.driver.Driver;
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
import static dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingUtils.DEFAULT_LABEL;
import static dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingUtils.sanitizeOrThrows;
import static dev.langchain4j.transformer.LLMGraphTransformerUtils.generateMD5;
import static dev.langchain4j.transformer.LLMGraphTransformerUtils.removeBackticks;

public class Neo4jGraph extends BaseNeo4jBuilder implements AutoCloseable {
    
    // TODO - configurable
    public static final String BASE_ENTITY_LABEL = "__Entity__";

    private static final String NODE_PROPERTIES_QUERY = """
            CALL apoc.meta.data()
            YIELD label, other, elementType, type, property
            WHERE NOT type = "RELATIONSHIP" AND elementType = "node"
            WITH label AS nodeLabels, collect({property:property, type:type}) AS properties
            RETURN {labels: nodeLabels, properties: properties} AS output
            """;

    private static final String REL_PROPERTIES_QUERY = """
            CALL apoc.meta.data()
            YIELD label, other, elementType, type, property
            WHERE NOT type = "RELATIONSHIP" AND elementType = "relationship"
            WITH label AS nodeLabels, collect({property:property, type:type}) AS properties
            RETURN {type: nodeLabels, properties: properties} AS output
            """;

    private static final String RELATIONSHIPS_QUERY = """
            CALL apoc.meta.data()
            YIELD label, other, elementType, type, property
            WHERE type = "RELATIONSHIP" AND elementType = "node"
            UNWIND other AS other_node
            RETURN {start: label, type: property, end: toString(other_node)} AS output
            """;
    
    /* default configs */
    public static final String DEFAULT_ID_PROP = "id";
    public static final String DEFAULT_TEXT_PROP = "text";
    
//    private final Driver driver;
//    private final String idProperty;
//    private final String textProperty;
//    private final String label;
//    private final String sanitizedIdProperty;
//    private final String sanitizedTextProperty;
//    private final String sanitizedLabel;

    @Getter
    private String schema;
    
    // TODO - add withBasicAuth like Neo4jEmbeddingStore ????
    //      TODO - base Class??? or util???

    /**
     * TODO - docs
     * @param driver
     */
    @Builder
    public Neo4jGraph(SessionConfig config, String databaseName, Driver driver, String label, String idProperty, String textProperty) {
        super(config, databaseName, driver, label, idProperty, textProperty);

//        /* required configs */
//        this.driver = ensureNotNull(driver, "driver");
//        this.driver.verifyConnectivity();
//
//        this.label = getOrDefault(label, DEFAULT_LABEL);
//        this.idProperty = getOrDefault(idProperty, DEFAULT_ID_PROP);
//        this.textProperty = getOrDefault(textProperty, DEFAULT_TEXT_PROP);
//        /* sanitize labels and property names, to prevent from Cypher Injections */
//        this.sanitizedLabel = sanitizeOrThrows(this.label, "label");
//        this.sanitizedIdProperty = sanitizeOrThrows(this.idProperty, "idProperty");
//        this.sanitizedTextProperty = sanitizeOrThrows(this.textProperty, "textProperty");
        
        try {
            refreshSchema();
        } catch (ClientException e) {
            if ("Neo.ClientError.Procedure.ProcedureNotFound".equals(e.code())) {
                throw new Neo4jException("Please ensure the APOC plugin is installed in Neo4j", e);
            }
            throw e;
        }
    }

    public ResultSummary executeWrite(String queryString) {
        return executeWrite(queryString, Map.of());
    }
    
    public ResultSummary executeWrite(String queryString, Map<String, Object> params) {

        try (Session session = session()) {
            return session.executeWrite(tx -> tx.run(queryString, params).consume());
        } catch (ClientException e) {
            throw new Neo4jException("Error executing query: " + queryString, e);
        }
    }

    public List<Record> executeRead(String queryString) {

        try (Session session = session()) {
            return session.executeRead(tx -> {
                Query query = new Query(queryString);
                Result result = tx.run(query);
                return result.list();
            });
        } catch (ClientException e) {
            throw new Neo4jException("Error executing query: " + queryString, e);
        }
    }

    public void refreshSchema() {

        List<String> nodeProperties = formatNodeProperties(executeRead(NODE_PROPERTIES_QUERY));
        List<String> relationshipProperties = formatRelationshipProperties(executeRead(REL_PROPERTIES_QUERY));
        List<String> relationships = formatRelationships(executeRead(RELATIONSHIPS_QUERY));

        this.schema = "Node properties are the following:\n" +
                String.join("\n", nodeProperties) + "\n\n" +
                "Relationship properties are the following:\n" +
                String.join("\n", relationshipProperties) + "\n\n" +
                "The relationships are the following:\n" +
                String.join("\n", relationships);
    }
    
    /*
        TODO - MAYBE THIS
            self.structured_schema = {
            "node_props": {el["labels"]: el["properties"] for el in node_properties},
            "rel_props": {el["type"]: el["properties"] for el in rel_properties},
            "relationships": relationships,
            "metadata": {"constraint": constraint, "index": index},
        }
     */
    
    
    // TODO - SANITIZE LABEL AND PROPS???
    
    /*
    TODO:
        * @param idProperty: the optional id property name (default: "id")
        * @param textProperty: the optional textProperty property name (default: "text") 
     */

    // TODO - utils
    public void addGraphDocuments(List<GraphDocument> graphDocuments, boolean includeSource, boolean baseEntityLabel) {
        if (baseEntityLabel) { // Check 
            // Create constraint if not exists
            executeWrite(
                    "CREATE CONSTRAINT IF NOT EXISTS FOR (b:" + BASE_ENTITY_LABEL + ") REQUIRE b.%s IS UNIQUE;"
                            .formatted(sanitizedIdProperty)
            );
            refreshSchema(); // Refresh constraint information
        }

        String nodeImportQuery = getNodeImportQuery(baseEntityLabel, includeSource);
        String relImportQuery = getRelImportQuery(baseEntityLabel);

        for (GraphDocument graphDoc : graphDocuments) {
            final Document source = graphDoc.getSource();
            if (!source.metadata().containsKey(sanitizedIdProperty)) {
                // TODO - CHECK THIS
                source.metadata().put(sanitizedIdProperty, generateMD5(source.text()));
            }

            // Remove backticks from node types
            for (GraphDocument.Node node : graphDoc.getNodes()) {
                node.setType(removeBackticks(node.getType()));
            }

            // Import nodes
            Map<String, Object> nodeParams = new HashMap<>();
            nodeParams.put("data", graphDoc.getNodes().stream()
                    .map(LLMGraphTransformerUtils::toMap)
                    .collect(Collectors.toList()));
//            nodeParams.put("data", document.getNodes().stream().map(GraphNode::toMap).collect(Collectors.toList()));
            
            // TODO - add document param only if includeSource=true
            final Map<String, Object> metadata = source.metadata().toMap();
            final Map<String, Object> document = Map.of("metadata", metadata, "text", source.text());
            nodeParams.put("document", document);
            executeWrite(nodeImportQuery, nodeParams);

            // Import relationships
            List<Map<String, String>> relData = graphDoc.getRelationships().stream()
                    .map(rel -> Map.of(
                            "source", rel.getSourceNode().getId(),
                            "source_label", removeBackticks(rel.getSourceNode().getType()),
                            "target", rel.getTargetNode().getId(),
                            "target_label", removeBackticks(rel.getTargetNode().getType()),
                            "type", removeBackticks(rel.getType().replace(" ", "_").toUpperCase())
                    )).toList();

            executeWrite(relImportQuery, Map.of("data", relData));
        }
    }


    private String getNodeImportQuery(boolean baseEntityLabel, boolean includeSource) {

        String includeDocsQuery = getIncludeDocsQuery(includeSource);
        if (baseEntityLabel) {
            return includeDocsQuery +
                    "UNWIND $data AS row " +
                    "MERGE (source:`" + BASE_ENTITY_LABEL + "` {id: row.id}) " +
                    "SET source += {} " +
//                    "SET source += row.properties " +
                    (includeSource ? "MERGE (d)-[:MENTIONS]->(source) " : "") +
                    "WITH source, row " +
                    "CALL apoc.create.addLabels(source, [row.type]) YIELD node " +
                    "RETURN distinct 'done' AS result";
        } else {
            return includeDocsQuery +
                    "UNWIND $data AS row " +
                    "CALL apoc.merge.node([row.type], {id: row.id}, {}, {}) YIELD node " +
//                    "CALL apoc.merge.node([row.type], {id: row.id}, row.properties, {}) YIELD node " +
                    (includeSource ? "MERGE (d)-[:MENTIONS]->(node) " : "") +
                    "RETURN distinct 'done' AS result";
        }
    }

    private static String getIncludeDocsQuery(boolean includeSource) {
        if (!includeSource) {
            return "";
        }
        return """
                MERGE (d:Document {id: $document.metadata.id})
                SET d.text = $document.text
                SET d += $document.metadata
                WITH d
                """;
    }

    private String getRelImportQuery(boolean baseEntityLabel) {
        if (baseEntityLabel) {
            return """
                UNWIND $data AS row
                MERGE (source:`%1$s` {id: row.source})
                MERGE (target:`%1$s` {id: row.target})
                WITH source, target, row
                CALL apoc.merge.relationship(source, row.type, {}, {}, target) YIELD rel
                RETURN distinct 'done'
                """.formatted(BASE_ENTITY_LABEL);
//            return "UNWIND $data AS row " +
//                    "MERGE (source:`" + BASE_ENTITY_LABEL + "` {id: row.source}) " +
//                    "MERGE (target:`" + BASE_ENTITY_LABEL + "` {id: row.target}) " +
//                    "WITH source, target, row " +
//                    "CALL apoc.merge.relationship(source, row.type, {}, {}, target) YIELD rel " +
//                    "RETURN distinct 'done'";
        } else {
            return """
                    UNWIND $data AS row
                    CALL apoc.merge.node([row.source_label], {id: row.source}, {}, {}) YIELD node as source
                    CALL apoc.merge.node([row.target_label], {id: row.target}, {}, {}) YIELD node as target
                    CALL apoc.merge.relationship(source, row.type, {}, {}, target) YIELD rel
                    RETURN distinct 'done'
                    """;
//            return "UNWIND $data AS row " +
//                    "CALL apoc.merge.node([row.source_label], {id: row.source}, {}, {}) YIELD node as source " +
//                    "CALL apoc.merge.node([row.target_label], {id: row.target}, {}, {}) YIELD node as target " +
//                    "CALL apoc.merge.relationship(source, row.type, {}, {}, target) YIELD rel " +
//                    "RETURN distinct 'done'";
        }
    }

    private List<String> formatNodeProperties(List<Record> records) {

        return records.stream()
                .map(this::getOutput)
                .map(r -> String.format("%s %s", r.asMap().get("labels"), formatMap(r.get("properties").asList(Value::asMap))))
                .toList();
    }

    private List<String> formatRelationshipProperties(List<Record> records) {

        return records.stream()
                .map(this::getOutput)
                .map(r -> String.format("%s %s", r.get("type"), formatMap(r.get("properties").asList(Value::asMap))))
                .toList();
    }

    private List<String> formatRelationships(List<Record> records) {

        return records.stream()
                .map(r -> getOutput(r).asMap())
                .map(r -> String.format("(:%s)-[:%s]->(:%s)", r.get("start"), r.get("type"), r.get("end")))
                .toList();
    }

    private Value getOutput(Record record) {

        return record.get("output");
    }

    private String formatMap(List<Map<String, Object>> properties) {

        return properties.stream()
                .map(prop -> prop.get("property") + ":" + prop.get("type"))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    @Override
    public void close() {

        this.driver.close();
    }
}
