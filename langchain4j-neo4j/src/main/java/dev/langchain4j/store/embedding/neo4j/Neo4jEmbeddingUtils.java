package dev.langchain4j.store.embedding.neo4j;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.neo4j.cypherdsl.support.schema_name.SchemaNames.sanitize;

public class Neo4jEmbeddingUtils {
    
    /* not-configurable strings, just used under-the-hood in `UNWIND $rows ...` statement */
    public static final String EMBEDDINGS_ROW_KEY = "embeddingRow";
    
    // todo - change it with config
//    public static final String ID_ROW_KEY = "id";
    
    /* default configs */
    public static final String DEFAULT_ID_PROP = "id";
    public static final String DEFAULT_DATABASE_NAME = "neo4j";
    public static final String DEFAULT_EMBEDDING_PROP = "embedding";
    public static final String PROPS = "props";
    public static final String DEFAULT_IDX_NAME = "langchain-embedding-index";
//    public static final String DEFAULT_IDX_NAME = "langchain-document-index";
//    public static final String DEFAULT_IDX_NAME = "langchain-document-index";
    public static final String DEFAULT_LABEL = "Document";
    public static final String DEFAULT_TEXT_PROP = "text";
    

    public static EmbeddingMatch<TextSegment> toEmbeddingMatch(Neo4jEmbeddingStore store, Record neo4jRecord) {
        Node node = neo4jRecord.get("node").asNode();

        Map<String, String> metaData = new HashMap<>();
        node.keys().forEach(key -> {
//            Set<String> notMetaKeys = Arrays.asList(store.getIdProperty(), store.getEmbeddingProperty(), store.getText())
//                    .stream()
//                    .collect(Collectors.toSet());
//            Set<String> notMetaKeys = Arrays.asList(DEFAULT_ID_PROP, store.getEmbeddingProperty(), store.getText())
            Set<String> notMetaKeys = Arrays.asList(store.getIdProperty(), store.getEmbeddingProperty(), store.getText())
                    .stream()
                    .collect(Collectors.toSet());
            if (!notMetaKeys.contains(key)) {
                metaData.put(key.replace(store.getMetadataPrefix(), ""), node.get(key).asString());
            }
        });

        Metadata metadata = new Metadata(metaData);

        Value text = node.get(store.getText());
        TextSegment textSegment = text.isNull()
                ? null
                : TextSegment.from(text.asString(), metadata);
        List<Number> embeddingList = node.get(store.getEmbeddingProperty())
                .asList(Value::asNumber);

        Embedding embedding = new Embedding(toFloatArray(embeddingList));

//        return new EmbeddingMatch<>(neo4jRecord.get("score").asDouble(), node.get(DEFAULT_ID_PROP).asString(), embedding, textSegment);
        return new EmbeddingMatch<>(neo4jRecord.get("score").asDouble(), node.get(store.getIdProperty()).asString(), embedding, textSegment);
//        return new EmbeddingMatch<>(neo4jRecord.get("score").asDouble(), node.get(store.getIdProperty()).asString(), embedding, textSegment);
    }
    
    public static float[] toFloatArray(List<Number> numberList) {
        float[] embeddingFloat = new float[numberList.size()];
        int i = 0;
        for(Number num: numberList) {
            embeddingFloat[i++] = num.floatValue();
        }
        return embeddingFloat;
    }

    public static Map<String, Object> toRecord(Neo4jEmbeddingStore store, int idx, List<String> ids, List<Embedding> embeddings, List<TextSegment> embedded) {
        String id = ids.get(idx);
        Embedding embedding = embeddings.get(idx);

        Map<String, Object> row = new HashMap<>();
//        row.put(DEFAULT_ID_PROP, id);
        row.put(store.getIdProperty(), id);

        Map<String, Object> properties = new HashMap<>();
        if (embedded != null) {
            TextSegment segment = embedded.get(idx);
            properties.put(store.getText(), segment.text());
            Map<String, String> metadata = segment.metadata().asMap();
            metadata.forEach((k, v) -> properties.put(store.getMetadataPrefix() + k, Values.value(v)));
        }

        row.put(EMBEDDINGS_ROW_KEY, Values.value(embedding.vector()));
        row.put(PROPS, properties);
        return row;
    }

    public static Collection<List<Map<String, Object>>> getRowsBatched(Neo4jEmbeddingStore store, List<String> ids, List<Embedding> embeddings, List<TextSegment> embedded) {
        int batchSize = 10_000;
        AtomicInteger batchCounter = new AtomicInteger();
        return IntStream.range(0, ids.size())
                .mapToObj(idx -> toRecord(store, idx, ids, embeddings, embedded))
                .collect(Collectors.groupingBy(it -> batchCounter.getAndIncrement() / batchSize))
                .values();
    }

//    public static String sanitizeOrThrows(String value, String defaultValue, String config) {
//        if (value == null) {
//            return defaultValue;
//        }
//        
//        return sanitize(value)
//                .orElseThrow(() -> {
//                    String invalidSanitizeValue = String.format("The value %s, to assign to configuration %s, cannot be safely quoted",
//                            value,
//                            config);
//                    throw new RuntimeException(invalidSanitizeValue);
//                });
//    }

    public static String sanitizeOrThrows(String value, String config) {
        
        return sanitize(value)
                .orElseThrow(() -> {
                    String invalidSanitizeValue = String.format("The value %s, to assign to configuration %s, cannot be safely quoted",
                            value,
                            config);
                    throw new RuntimeException(invalidSanitizeValue);
                });
    }
}
