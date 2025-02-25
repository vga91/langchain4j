package dev.langchain4j.store.graph.neo4j;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.transformer.GraphDocument;
import dev.langchain4j.transformer.LLMGraphTransformerUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.langchain4j.transformer.LLMGraphTransformerUtils.generateMD5;
import static dev.langchain4j.transformer.LLMGraphTransformerUtils.removeBackticks;

public class Neo4jGraphUtils {


    public static void addGraphDocuments(List<GraphDocument> graphDocuments, boolean includeSource, boolean baseEntityLabel, /*String idProperty, String sanitizedIdProperty, String sanitizedTextProperty, String sanitizedLabel,*/ Neo4jGraph graph) {
        if (baseEntityLabel) { 
            // Check and reate constraint if not exists
            final String constraintQuery = "CREATE CONSTRAINT IF NOT EXISTS FOR (b:%s) REQUIRE b.%s IS UNIQUE;"
                    .formatted(graph.sanitizedLabel, graph.sanitizedIdProperty);
            graph.executeWrite(constraintQuery);
            graph.refreshSchema();
        }
        
        for (GraphDocument graphDoc : graphDocuments) {
            final Document source = graphDoc.getSource();
            if (!source.metadata().containsKey(graph.idProperty)) {
                source.metadata().put(graph.idProperty, generateMD5(source.text()));
            }


            // Import nodes
            Map<String, Object> nodeParams = new HashMap<>();
            nodeParams.put("data", graphDoc.getNodes().stream()
                    .map(LLMGraphTransformerUtils::toMap)
                    .collect(Collectors.toList()));

            if (includeSource) {
                final Map<String, Object> metadata = source.metadata().toMap();
                final Map<String, Object> document = Map.of("metadata", metadata, "text", source.text());
                nodeParams.put("document", document);
            }

            String nodeImportQuery = getNodeImportQuery(baseEntityLabel, includeSource, graph);//, sanitizedLabel, sanitizedIdProperty, sanitizedTextProperty);
            graph.executeWrite(nodeImportQuery, nodeParams);

            // Import relationships
            List<Map<String, String>> relData = graphDoc.getRelationships().stream()
                    .map(rel -> Map.of(
                            "source", rel.getSourceNode().getId(),
                            "source_label", removeBackticks(rel.getSourceNode().getType()),
                            "target", rel.getTargetNode().getId(),
                            "target_label", removeBackticks(rel.getTargetNode().getType()),
                            "type", removeBackticks(rel.getType().replace(" ", "_").toUpperCase())
                    )).toList();

            String relImportQuery = getRelImportQuery(baseEntityLabel, graph);//, sanitizedLabel, sanitizedIdProperty);
            graph.executeWrite(relImportQuery, Map.of("data", relData));
        }
    }


//    private static String getNodeImportQuery(boolean baseEntityLabel, boolean includeSource, String sanitizedLabel, String sanitizedIdProperty, String sanitizedTextProperty) {
    private static String getNodeImportQuery(boolean baseEntityLabel, boolean includeSource, Neo4jGraph graph) {//} String sanitizedLabel, String sanitizedIdProperty, String sanitizedTextProperty) {

        String includeDocsQuery = getIncludeDocsQuery(includeSource, graph.sanitizedTextProperty);
        if (baseEntityLabel) {
            return includeDocsQuery +
                    "UNWIND $data AS row " +
                    "MERGE (source:%1$s {%2$s: row.id}) ".formatted(graph.sanitizedLabel, graph.sanitizedIdProperty) +
                    (includeSource ? "MERGE (d)-[:MENTIONS]->(source) " : "") +
                    "WITH source, row " +
                    "SET source:$(row.type) " +
                    "RETURN distinct 'done' AS result";
        } else {
            return includeDocsQuery +
                    "UNWIND $data AS row " +
                    "MERGE (node:$(row.type) {%1$s: row.id}) ".formatted(graph.sanitizedIdProperty) +
                    (includeSource ? "MERGE (d)-[:MENTIONS]->(node) " : "") +
                    "RETURN distinct 'done' AS result";
        }
    }

    private static String getIncludeDocsQuery(boolean includeSource, String sanitizedTextProperty) {
        if (!includeSource) {
            return "";
        }
        return """
                MERGE (d:Document {id: $document.metadata.id})
                SET d.%1$s = $document.text
                SET d += $document.metadata
                WITH d
                """.formatted(sanitizedTextProperty);
    }

    private static String getRelImportQuery(boolean baseEntityLabel, Neo4jGraph graph) { //String sanitizedLabel, String sanitizedIdProperty) {
        if (baseEntityLabel) {
            return """
                UNWIND $data AS row
                MERGE (source:%1$s {%2$s: row.source})
                MERGE (target:%1$s {%2$s: row.target})
                WITH source, target, row
                MERGE (source)-[rel:$(row.type)]->(target)
                RETURN distinct 'done'
                """.formatted(graph.sanitizedLabel, graph.sanitizedIdProperty);
        } else {
            return """
                    UNWIND $data AS row
                    MERGE (source:$(row.source_label) {%1$s: row.source})
                    MERGE (target:$(row.target_label) {%1$s: row.target})
                    MERGE (source)-[rel:$(row.type)]->(target)
                    RETURN distinct 'done'
                    """.formatted(graph.sanitizedIdProperty);
        }
    }
}
