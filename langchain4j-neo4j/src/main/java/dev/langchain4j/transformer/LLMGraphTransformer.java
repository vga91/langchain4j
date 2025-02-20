package dev.langchain4j.transformer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO --- vedere la parte # Strict mode filtering in python

/* TODO
    The baseEntityLabel parameter assigns an additional __Entity__ label to each node, 
    enhancing indexing and query performance.
    The include_source parameter links nodes to their originating documents, 
    facilitating data traceability and context understanding.
 */

/* TODO 
    # Extract graph data
    graph_documents = llm_transformer.convert_to_graph_documents(documents)
    # Store to neo4j
    graph.add_graph_documents(
      graph_documents, 
      baseEntityLabel=True, 
      include_source=True
    )
 */

public class LLMGraphTransformer {

    public static class Graph {
        private List<Node> nodes;
        private List<Edge> edges;

        public Graph() {
            this.nodes = new java.util.ArrayList<>();
            this.edges = new java.util.ArrayList<>();
        }

        public void addNode(Node node) {
            nodes.add(node);
        }

        public void addEdge(Edge edge) {
            edges.add(edge);
        }

        public List<Node> getNodes() {
            return nodes;
        }

        public List<Edge> getEdges() {
            return edges;
        }

        public boolean constraintExists(String label, String property) {
            return false; // Placeholder for actual implementation
        }

        public void createConstraint(String label, String property) {
            // Placeholder for actual implementation
        }
    }

    
    public static class Node {
        private String id;
//        private String data;
        private String type;

        public Node(String id, String type) {
            this.id = id;
            this.type = type;
        }

        public String getId() {
            return id;
        }

//        public String getData() {
//            return data;
//        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class Edge {
//        private String from;
//        private String to;
        private Node sourceNode;
        private Node targetNode;
        private String type;

        public Edge(final Node sourceNode, final Node targetNode, final String type) {
            this.sourceNode = sourceNode;
            this.targetNode = targetNode;
            this.type = type;
        }

        public Node getSourceNode() {
            return sourceNode;
        }

        public Node getTargetNode() {
            return targetNode;
        }

        public String getType() {
            return type;
        }

        //        public Edge(Node sourceNode, Node targetNode, String type) {
//            this.from = from;
//            this.to = to;
//        }
//
//        public String getSourceLabel() {
//            return sourceLabel;
//        }
//
//        public String getTargetLabel() {
//            return targetLabel;
//        }
//
//        public String getType() {
//            return type;
//        }
//
//        public String getFrom() {
//            return from;
//        }
//
//        public String getTo() {
//            return to;
//        }
//
//        public void setSourceLabel(String sourceLabel) {
//            this.sourceLabel = sourceLabel;
//        }
//
//        public void setTargetLabel(String targetLabel) {
//            this.targetLabel = targetLabel;
//        }
//
//        public void setType(String type) {
//            this.type = type;
//        }
    }

    // TODO - WHAT???
    interface LLMService {
        String callLLM(String prompt);
    }

    // TODO - substitute to Document of Langchain4j
//    static class Document {
//        private String id;
//        private String content;
//
//        public Document(String id, String content) {
//            this.id = id;
//            this.content = content;
//        }
//
//        public String getId() {
//            return id;
//        }
//
//        public String getContent() {
//            return content;
//        }
//    }

    public static class GraphDocument {
//        private String id;
//        private String content;
        private Set<Node> nodes;
        private Set<Edge> relationships;
        private Document source;

        public GraphDocument(Set<Node> nodes, Set<Edge> relationships, Document source) {
            this.nodes = nodes;
            this.relationships = relationships;
            this.source = source;
        }

//        public String getId() {
//            return id;
//        }
//
//        public String getContent() {
//            return content;
//        }
//
//        public void setId(String id) {
//            this.id = id;
//        }


        public Document getSource() {
            return source;
        }

        public Set<Node> getNodes() {
            return nodes;
        }

        public Set<Edge> getRelationships() {
            return relationships;
        }
    }


    // TODO - SPLIT CLASSES
    
    private final LLMService llmService;

    // TODO - use @Builder
    public LLMGraphTransformer(LLMService llmService) {
        this.llmService = llmService;
    }

//    public Graph transform(Graph graph) {
//        Graph transformedGraph = new Graph();
//
//        for (Node node : graph.getNodes()) {
//            Node transformedNode = transformNode(node);
//            transformedGraph.addNode(transformedNode);
//        }
//
//        for (Edge edge : graph.getEdges()) {
//            transformedGraph.addEdge(edge);
//        }
//
//        return transformedGraph;
//    }
//
//    private Node transformNode(Node node) {
//        String prompt = "Transform this node: " + node.getData();
//        String transformedData = llmService.callLLM(prompt);
//        return new Node(node.getId(), transformedData);
//    }

    public List<GraphDocument> convertToGraphDocuments(List<Document> documents) {
        // TODO - MAGARI METTERE ANCHE UN FILTER IS NOT NULL
        return documents.stream().map(this::processResponse).collect(Collectors.toList());
    }

    public List<ChatMessage> createUnstructuredPrompt(String text, List<String> nodeLabels, List<String> relTypes, String relationshipType, String additionalInstructions) {
        String nodeLabelsStr = nodeLabels != null ? nodeLabels.toString() : "";
        String relTypesStr = "";
        if (relTypes != null) {
            if ("tuple".equals(relationshipType)) {
                Set<String> uniqueRelTypes = new HashSet<>();
                for (String rel : relTypes) {
                    uniqueRelTypes.add(rel);
                }
                relTypesStr = uniqueRelTypes.toString();
            } else {
                relTypesStr = relTypes.toString();
            }
        }

        String systemPrompt = String.join("\n", Arrays.asList(
                "You are a top-tier algorithm designed for extracting information in structured formats to build a knowledge graph.",
                "Your task is to identify entities and relations from a given text and generate output in JSON format.",
                "Each object should have keys: 'head', 'head_type', 'relation', 'tail', and 'tail_type'.",
                nodeLabels != null ? "The 'head_type' and 'tail_type' must be one of: " + nodeLabelsStr : "",
                relTypes != null ? "The 'relation' must be one of: " + relTypesStr : "",
                "IMPORTANT NOTES:\n- Don't add any explanation or extra text.",
                additionalInstructions
        ));

        final SystemMessage systemMessage = new SystemMessage(systemPrompt);

        String humanPrompt = String.join("\n", Arrays.asList(
                "Based on the following example, extract entities and relations from the provided text.",
                nodeLabels != null ? "# ENTITY TYPES:\n" + nodeLabelsStr : "",
                relTypes != null ? "# RELATION TYPES:\n" + relTypesStr : "",
                "For the following text, extract entities and relations as in the provided example.",
                "Text: " + text
        ));

        final UserMessage userMessage = new UserMessage(humanPrompt);

        return List.of(systemMessage, userMessage);
    }

    private GraphDocument processResponse(Document document) {
//        String text = document.getContent();
        
        /*
        
            TODO 
           prompt = prompt or create_unstructured_prompt(
                allowed_nodes,
                allowed_relationships,
                self._relationship_type,
                additional_instructions,
            )
            
            rawSchema = self.chain.invoke({"input": text}, config=config)
         */
        
        // TODO - configurable chatLanguageModel
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .baseUrl("demo")
                .apiKey("demo")
                .organizationId(System.getenv("OPENAI_ORGANIZATION_ID"))
                .modelName("gpt-4o-mini")
                .logRequests(true)
                .logResponses(true)
                .build();

        final String text = document.text();
        
        
        // todo - configurable, pr 
        //  use PromptTemplate handling via {{ }}
        final List<ChatMessage> messages = createUnstructuredPrompt(text, null, null, null, "");
        // final PromptTemplate template = PromptTemplate.from(prompt);

        System.out.println("messages = " + messages);
        // TODO -- PromptTemplate

        //final Prompt apply = template.apply(Map.of());
        //final String text1 = apply.text();
        String rawSchema = chatModel.generate(messages).content().text();
                // new MockLLMService().callLLM(text);

        Set<Node> nodesSet = new HashSet<>();
        Set<Edge> relationships = new HashSet<>();

        List<Map<String, String>> parsedJson = parseJson(rawSchema);
        if (parsedJson == null) {
            // TODO - EVALUATE A RETRY MECHANISM CONFIGURABLE
            System.out.println("parsedJson = " + parsedJson);
            return new GraphDocument(Set.of(), Set.of(), document);
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
//            relationships.add(new Edge(sourceNode.getId(), targetNode.getId(), rel.get("relation")));
        }

//        List<Node> nodes = new ArrayList<>(nodesSet);
        // TODO - REMOVE THE TOSTRING()
        return new GraphDocument(nodesSet, relationships, document);
//        return new GraphDocument(nodes, relationships, document);
    }

    private List<Map<String, String>> parseJson(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        // TODO - COMMENT THIS, can output json``` sometimes
        jsonString = jsonString.replaceAll("```", "")
                .replaceAll("json", "");
        try {
            return objectMapper.readValue(jsonString, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            System.out.println("e = " + e);
            return null;
            // throw new RuntimeException("Error parsing JSON", e);
        }
        // Placeholder for actual JSON parsing
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
        if (baseEntityLabel && !graph.constraintExists("BaseEntity", "id")) {
            graph.createConstraint("BaseEntity", "id");
        }

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
