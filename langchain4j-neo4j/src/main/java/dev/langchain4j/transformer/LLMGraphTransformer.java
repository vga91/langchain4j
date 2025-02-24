package dev.langchain4j.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Builder;
import lombok.Getter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.langchain4j.transformer.GraphDocument.*;
import dev.langchain4j.internal.RetryUtils;

/**
 * TODO - vedere se lo fa da solo,
 * fare un document con Keanu Reeves e K. Reeves
 * se non lo fa, aggiustare il prompt magari? O forse tra le options?
 * https://chatgpt.com/c/67b8568b-db80-800c-8aab-b4c3f1fb434f
 * 
 */

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.transformer.LLMGraphTransformerUtils.*;


@Getter
public class LLMGraphTransformer {


    /*
    TODO - RICOPIARE QUESTO COMMENTO
    

     */

    /*

    TODO Option to link entities back to the source document
    
    TODO Additional metadata about the entities and their relationships (e.g. facts, claims, confidence-scores)
    TODO - JUST A FACT These knowledge graphs form the foundation for advanced RAG patterns in GraphRAG (see https://graphrag.com)
    TODO- JUST A FACT, WRITE ON THE PR Besides data from document loaders also data that already exists in large text fields in databases (semi-structured data) could be analyzed and extracted
    This also offers the means to then run additional algorithms on the data e.g. for query focused summarization that compute communities/clusters of topics and summarize them with an LLM
     */

    private final List<String> allowedNodes;
    private final List<String> allowedRelationships;
    private final List<ChatMessage> prompt;
    private final boolean strictMode;
    private final boolean ignoreToolUsage;
    private final String additionalInstructions;
    private final ChatLanguageModel model;
    private final List<Map<String, String>> examples;
    private final Integer maxAttempts;
    
    /**
     * It allows specifying constraints on the types of nodes and relationships to include in the output graph.
     * The class supports extracting properties for both nodes and relationships.
     * 
     * Example: TODO
     * 
     * @param model the {@link ChatLanguageModel} (required)
     * @param allowedNodes Specifies which node types are allowed in the graph. If null or empty allows all node types (default: [])
     * @param allowedRelationships Specifies which relationship types are allowed in the graph. If null or empty allows all relationship types (default: [])
     * @param prompt TODO
     * @param strictMode TODO
     * @param ignoreToolUsage TODO
     * @param additionalInstructions Allows you to add additional instructions to the prompt without having to change the whole prompt (default: '')
     * @param examples Allows you to add additional instructions to the prompt without having to change the whole prompt (default: {@link LLMGraphTransformerUtils#EXAMPLES_PROMPT})
     * @param maxAttempts Retry N times the transformation if it fails (default: 1)
     */
    @Builder
    public LLMGraphTransformer(ChatLanguageModel model,
                               List<String> allowedNodes,
                               List<String> allowedRelationships,
                               List<ChatMessage> prompt,
                               Boolean strictMode,
                               Boolean ignoreToolUsage,
                               String additionalInstructions,
                               List<Map<String, String>> examples,
                               Integer maxAttempts) {

        this.model = ensureNotNull(model, "model");
        
        this.allowedNodes = getOrDefault(allowedNodes, List.of());
        this.allowedRelationships = getOrDefault(allowedRelationships, List.of());
        this.prompt = prompt;
        
        this.maxAttempts = getOrDefault(maxAttempts, 1);
        // TODO ??
        this.strictMode = getOrDefault(strictMode, true);
//        this.nodeProperties = nodeProperties;
//        this.relationshipProperties = relationshipProperties;

        // TODO ??
        this.ignoreToolUsage = getOrDefault(ignoreToolUsage, false);
        this.additionalInstructions = getOrDefault(additionalInstructions, "");
        
        this.examples = getOrDefault(examples, EXAMPLES_PROMPT);
        
    }
    

    public List<GraphDocument> convertToGraphDocuments(List<Document> documents) {
        return documents.stream().map(this::processResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<ChatMessage> createUnstructuredPrompt(String text) {
        // TODO - test with this
        if (prompt != null && !prompt.isEmpty()) {
            return prompt;
        }

        // TODO - commonize
        final boolean withAllowedNodes = allowedNodes != null && !allowedNodes.isEmpty();
        String nodeLabelsStr = withAllowedNodes ? allowedNodes.toString() : "";
        // TODO - commonize
        final boolean withAllowedRels = allowedRelationships != null && !allowedRelationships.isEmpty();
        String relTypesStr = withAllowedRels ? allowedRelationships.toString() : "";

        // TODO - optimize
        // TODO - use prompt template via promptTemplate.apply()
        String systemPrompt = String.join("\n", Arrays.asList(
                "You are a top-tier algorithm designed for extracting information in structured formats to build a knowledge graph.",
                "Your task is to identify entities and relations from a given text and generate output in JSON format.",
                "Each object should have keys: 'head', 'head_type', 'relation', 'tail', and 'tail_type'.",
                withAllowedNodes ? "The 'head_type' and 'tail_type' must be one of: " + nodeLabelsStr : "",
                withAllowedRels ? "The 'relation' must be one of: " + relTypesStr : "",
                "IMPORTANT NOTES:\n- Don't add any explanation or extra text.",
                additionalInstructions
        ));

        final SystemMessage systemMessage = new SystemMessage(systemPrompt);
        

        final String examplesString = getString(examples);

        String humanPrompt = String.join("\n", Arrays.asList(
                "Based on the following example, extract entities and relations from the provided text.",
                withAllowedNodes ? "# ENTITY TYPES:\n" + nodeLabelsStr : "",
                withAllowedRels ? "# RELATION TYPES:\n" + relTypesStr : "",
                "Below are a number of examples of text and their extracted entities and relationships.",
                examplesString,
                additionalInstructions,
                "For the following text, extract entities and relations as in the provided example.",
                "Text: " + text
        ));

        final UserMessage userMessage = new UserMessage(humanPrompt);

        return List.of(systemMessage, userMessage);
    }

    private static String getString(List<Map<String, String>> examples1) {
        final String examplesString;
        try {
            examplesString = new ObjectMapper().writeValueAsString(examples1);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return examplesString;
    }

    private GraphDocument processResponse(Document document) {

        final String text = document.text();
        
        
        // todo - configurable, pr 
        //  use PromptTemplate handling via {{ }}
        final List<ChatMessage> messages = createUnstructuredPrompt(text);
        // final PromptTemplate template = PromptTemplate.from(prompt);

        System.out.println("messages = " + messages);
        // TODO -- PromptTemplate

        //final Prompt apply = template.apply(Map.of());
        //final String text1 = apply.text();
        
        Set<Node> nodesSet = new HashSet<>();
        Set<Edge> relationships = new HashSet<>();
        
        // TODO - DEPRECATED
        List<Map<String, String>> parsedJson = getJsonResult(messages);
        if (parsedJson == null || parsedJson.isEmpty()) {
            return null;
        }

        for (Map<String, String> rel : parsedJson) {
            if (!rel.containsKey("head") || !rel.containsKey("tail") || !rel.containsKey("relation")) {
                continue;
            }

            Node sourceNode = new Node(rel.get("head"), rel.getOrDefault("head_type", "DefaultNodeType"));
            Node targetNode = new Node(rel.get("tail"), rel.getOrDefault("tail_type", "DefaultNodeType"));

            nodesSet.add(sourceNode);
            nodesSet.add(targetNode);

            final String relation = rel.get("relation");
            final Edge edge = new Edge(sourceNode, targetNode, relation);
            relationships.add(edge);
        }
        
        if (nodesSet.isEmpty()) {
            return null;
        }

        return new GraphDocument(nodesSet, relationships, document);
//        return new GraphDocument(nodes, relationships, document);
    }

    private List<Map<String, String>> getJsonResult(List<ChatMessage> messages) {

        List<Map<String, String>> parsedJson = RetryUtils.withRetry(() -> {
            String rawSchema = model.generate(messages)
                    .content()
                    .text();
            
            // TODO - util??
            // TODO - COMMENT THIS, can output json``` sometimes
            rawSchema = rawSchema.replaceAll("```", "")
                    .replaceAll("json", "");
            
            return parseJson(rawSchema);
        }, maxAttempts);
        
//        if (parsedJson == null) {
//            // TODO - EVALUATE A RETRY MECHANISM CONFIGURABLE
//            System.out.println("parsedJson = " + parsedJson);
//            return null;
//        }
        return parsedJson;
    }

    // TODO - util??
    private <T> T parseJson(String jsonString) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(jsonString, new TypeReference<>() {});
    }
    
//    public List<GraphDocument> convertToGraphDocuments(List<Document> documents) {
//        return documents.stream()
//                .map(doc -> new GraphDocument(doc.getId(), doc.getContent()))
//                .collect(Collectors.toList());
//    }

    // TODO - QUESTO IN REALTA DOVREBBE VERAMENTE GENERARE ENTITÀ
    //  QUINDI VA IN UNA CLASSE SEPARATA IN TEORIA...
    // TODO - METTERE UPPERCASE PER LE RELAZIONI, PERCHE PUO CAPITARE is_on INVECE DI IS_ON
    //      CAPITALIZE LABEL???
    public void addGraphDocuments(Graph graph, List<GraphDocument> graphDocuments, boolean includeSource, boolean baseEntityLabel) {
//        if (baseEntityLabel && !graph.constraintExists("BaseEntity", "id")) {
//            graph.createConstraint("BaseEntity", "id");
//        }

            // TODO - STA ROBA VA IN NEO4J
//        for (GraphDocument doc : graphDocuments) {
//            if (doc.getId() == null || doc.getId().isEmpty()) {
//                doc.setId(generateMD5(doc.getContent()));
//            }
//
//            for (Node node : doc.getNodes()) {
//                node.setType(removeBackticks(node.getType()));
//                graph.addNode(node);
//            }
//
//            for (Edge edge : doc.getRelationships()) {
//                edge.setSourceLabel(removeBackticks(edge.getSourceLabel()));
//                edge.setTargetLabel(removeBackticks(edge.getTargetLabel()));
//                edge.setType(removeBackticks(edge.getType().replace(" ", "_").toUpperCase()));
//                graph.addEdge(edge);
//            }
//        }
    }

    public static String generateMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    public static String removeBackticks(String input) {
        return input.replace("`", "");
    }
}
