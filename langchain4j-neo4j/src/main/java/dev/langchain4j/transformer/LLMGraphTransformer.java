package dev.langchain4j.transformer;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.internal.RetryUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.retriever.neo4j.Neo4jContentRetriever;
import lombok.Builder;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.transformer.GraphDocument.Edge;
import static dev.langchain4j.transformer.GraphDocument.Node;
import static dev.langchain4j.transformer.LLMGraphTransformerUtils.EXAMPLES_PROMPT;
import static dev.langchain4j.transformer.LLMGraphTransformerUtils.getStringFromListOfMaps;
import static dev.langchain4j.transformer.LLMGraphTransformerUtils.parseJson;


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
     * @param additionalInstructions Allows you to add additional instructions to the prompt without having to change the whole prompt (default: '')
     * @param examples Allows you to add additional instructions to the prompt without having to change the whole prompt (default: {@link LLMGraphTransformerUtils#EXAMPLES_PROMPT})
     * @param maxAttempts Retry N times the transformation if it fails (default: 1)
     */
    @Builder
    public LLMGraphTransformer(ChatLanguageModel model,
                               List<String> allowedNodes,
                               List<String> allowedRelationships,
                               List<ChatMessage> prompt,
                               String additionalInstructions,
                               List<Map<String, String>> examples,
                               Integer maxAttempts) {

        this.model = ensureNotNull(model, "model");
        
        this.allowedNodes = getOrDefault(allowedNodes, List.of());
        this.allowedRelationships = getOrDefault(allowedRelationships, List.of());
        this.prompt = prompt;
        
        this.maxAttempts = getOrDefault(maxAttempts, 1);
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

        final boolean withAllowedNodes = allowedNodes != null && !allowedNodes.isEmpty();
        String nodeLabelsStr = withAllowedNodes ? allowedNodes.toString() : "";
        
        final boolean withAllowedRels = allowedRelationships != null && !allowedRelationships.isEmpty();
        String relTypesStr = withAllowedRels ? allowedRelationships.toString() : "";

        // TODO - optimize
        // TODO - use prompt template via promptTemplate.apply()
        String systemPrompt = String.join("\n", Arrays.asList(
                "You are a top-tier algorithm designed for extracting information in structured formats to build a knowledge graph.",
                "Your task is to identify entities and relations from a given text and generate output in JSON format.",
                "Each object should have keys: 'head', 'head_type', 'relation', 'tail', and 'tail_type'.",
                withAllowedNodes ? "The 'head_type' and 'tail_type' must be one of: " + allowedNodes.toString() : "",
                withAllowedRels ? "The 'relation' must be one of: " + allowedRelationships.toString() : "",
                "IMPORTANT NOTES:\n- Don't add any explanation or extra text.",
                additionalInstructions
        ));

        final PromptTemplate from = PromptTemplate.from(
                """
                        You are a top-tier algorithm designed for extracting information in structured formats to build a knowledge graph.
                        Your task is to identify entities and relations from a given text and generate output in JSON format.
                        Each object should have keys: 'head', 'head_type', 'relation', 'tail', and 'tail_type'.
                        {{nodes}}
                        {{rels}}
                        IMPORTANT NOTES:\n- Don't add any explanation or extra text.
                        {{additional}}
                        """
        );

//        final Prompt systemPrompt = 

        final SystemMessage systemMessage = from.apply(
                Map.of(
                        "nodes", withAllowedNodes ? "The 'head_type' and 'tail_type' must be one of: " + allowedNodes.toString() : "",
                        "rels", withAllowedRels ? "The 'relation' must be one of: " + allowedRelationships.toString() : "",
                        "additional", additionalInstructions
                )
        ).toSystemMessage();
        

        final String examplesString = getStringFromListOfMaps(EXAMPLES_PROMPT);

        String humanPrompt = String.join("\n", Arrays.asList(
                "Based on the following example, extract entities and relations from the provided text.",
                withAllowedNodes ? "# ENTITY TYPES:\n" + allowedNodes.toString() : "",
                withAllowedRels ? "# RELATION TYPES:\n" + allowedRelationships.toString() : "",
                "Below are a number of examples of text and their extracted entities and relationships.",
                examplesString,
                additionalInstructions,
                "For the following text, extract entities and relations as in the provided example.",
                "Text: " + text
        ));

        final PromptTemplate from1 = PromptTemplate.from("""
                Based on the following example, extract entities and relations from the provided text.
                {{nodes}}
                {{rels}}
                Below are a number of examples of text and their extracted entities and relationships.
                {{examples}}
                {{additional}}
                For the following text, extract entities and relations as in the provided example.
                Text: {{input}}
                """);

        final UserMessage userMessage = from1.apply(
                Map.of(
                        "nodes", withAllowedNodes ? "# ENTITY TYPES:\n" + allowedNodes.toString() : "",
                        "rels", withAllowedRels ? "# RELATION TYPES:\n" + allowedRelationships.toString() : "",
                        "examples", examplesString,
                        "additional", additionalInstructions,
                        "input", text
                )
        ).toUserMessage();
//                new UserMessage(humanPrompt);

        return List.of(systemMessage, userMessage);
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
            
            rawSchema = Neo4jContentRetriever.getString(rawSchema);

            return parseJson(rawSchema);
        }, maxAttempts);
        
        return parsedJson;
    }

    // TODO - util??
}
