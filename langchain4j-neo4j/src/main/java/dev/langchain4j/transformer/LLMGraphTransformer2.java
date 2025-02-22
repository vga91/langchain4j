//package dev.langchain4j.transformer;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import dev.langchain4j.data.document.Document;
//import dev.langchain4j.data.message.ChatMessage;
//import dev.langchain4j.data.message.SystemMessage;
//import dev.langchain4j.data.message.UserMessage;
//import dev.langchain4j.model.chat.ChatLanguageModel;
//import dev.langchain4j.model.openai.OpenAiChatModel;
//import lombok.Builder;
//import lombok.Getter;
//
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import static dev.langchain4j.internal.Utils.getOrDefault;
//import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
//import static dev.langchain4j.transformer.GraphDocument.Edge;
//import static dev.langchain4j.transformer.GraphDocument.Node;
//
//// TODO --- vedere la parte # Strict mode filtering in python
//
///* TODO
//    The baseEntityLabel parameter assigns an additional __Entity__ label to each node, 
//    enhancing indexing and query performance.
//    The include_source parameter links nodes to their originating documents, 
//    facilitating data traceability and context understanding.
// */
//
///* TODO 
//    # Extract graph data
//    graph_documents = llm_transformer.convert_to_graph_documents(documents)
//    # Store to neo4j
//    graph.add_graph_documents(
//      graph_documents, 
//      baseEntityLabel=True, 
//      include_source=True
//    )
// */
//
//@Getter
//public class LLMGraphTransformer2 {
//
//
//    // TODO - WHAT???
//    interface LLMService {
//        String callLLM(String prompt);
//    }
//
//    // TODO - substitute to Document of Langchain4j
////    static class Document {
////        private String id;
////        private String content;
////
////        public Document(String id, String content) {
////            this.id = id;
////            this.content = content;
////        }
////
////        public String getId() {
////            return id;
////        }
////
////        public String getContent() {
////            return content;
////        }
////    }
//
//
//    /*
//    TODO - RICOPIARE QUESTO COMMENTO
//    
//    """Transform documents into graph-based documents using a LLM.
//
//    It allows specifying constraints on the types of nodes and relationships to include
//    in the output graph. The class supports extracting properties for both nodes and
//    relationships.
//
//    Args:
//        llm (BaseLanguageModel): An instance of a language model supporting structured
//          output.
//        allowed_nodes (List[str], optional): Specifies which node types are
//          allowed in the graph. Defaults to an empty list, allowing all node types.
//        allowed_relationships (List[str], optional): Specifies which relationship types
//          are allowed in the graph. Defaults to an empty list, allowing all relationship
//          types.
//        prompt (Optional[ChatPromptTemplate], optional): The prompt to pass to
//          the LLM with additional instructions.
//        strict_mode (bool, optional): Determines whether the transformer should apply
//          filtering to strictly adhere to `allowed_nodes` and `allowed_relationships`.
//          Defaults to True.
//        node_properties (Union[bool, List[str]]): If True, the LLM can extract any
//          node properties from text. Alternatively, a list of valid properties can
//          be provided for the LLM to extract, restricting extraction to those specified.
//        relationship_properties (Union[bool, List[str]]): If True, the LLM can extract
//          any relationship properties from text. Alternatively, a list of valid
//          properties can be provided for the LLM to extract, restricting extraction to
//          those specified.
//        ignore_tool_usage (bool): Indicates whether the transformer should
//          bypass the use of structured output functionality of the language model.
//          If set to True, the transformer will not use the language model's native
//          function calling capabilities to handle structured output. Defaults to False.
//        additional_instructions (str): Allows you to add additional instructions
//          to the prompt without having to change the whole prompt.
//
//    Example:
//        .. code-block:: python
//            from langchain_experimental.graph_transformers import LLMGraphTransformer
//            from langchain_core.documents import Document
//            from langchain_openai import ChatOpenAI
//
//            llm=ChatOpenAI(temperature=0)
//            transformer = LLMGraphTransformer(
//                llm=llm,
//                allowed_nodes=["Person", "Organization"])
//
//            doc = Document(page_content="Elon Musk is suing OpenAI")
//            graph_documents = transformer.convert_to_graph_documents([doc])
//     */
//
//    /*
//            self,
//        llm: BaseLanguageModel,
//        allowed_nodes: List[str] = [],
//        allowed_relationships: Union[List[str], List[Tuple[str, str, str]]] = [],
//        prompt: Optional[ChatPromptTemplate] = None,
//        strict_mode: bool = True,
//        node_properties: Union[bool, List[str]] = False,
//        relationship_properties: Union[bool, List[str]] = False,
//        ignore_tool_usage: bool = False,
//        additional_instructions: str = "",
//     */
//
//    /*
//    A general concept of NER / Entity / Relationship extraction
//    Then processing / storing these entities in databases .e.g graph databases like neo4j (but relational would work as well)
//    TODO - done via allowedNodes, allowedRelationships, Options for providing graph schema and additional extraction for the construction
//    TODO Option to link entities back to the source document
//    TODO Entity de-duplication with the LLM and possibly other methods (slightly different spelling of names/ids)
//    TODO Additional metadata about the entities and their relationships (e.g. facts, claims, confidence-scores)
//    TODO - JUST A FACT These knowledge graphs form the foundation for advanced RAG patterns in GraphRAG (see https://graphrag.com)
//    TODO- JUST A FACT, WRITE ON THE PR Besides data from document loaders also data that already exists in large text fields in databases (semi-structured data) could be analyzed and extracted
//    This also offers the means to then run additional algorithms on the data e.g. for query focused summarization that compute communities/clusters of topics and summarize them with an LLM
//     */
//
//    private final List<String> allowedNodes;
//    private final List<String> allowedRelationships;
//    private final List<ChatMessage> prompt;
//    private final boolean strictMode;
////    private final Object nodeProperties;
////    private final Object relationshipProperties;
//    private final boolean ignoreToolUsage;
//    private final String additionalInstructions;
//    
//    // TODO - use @Builder
//    @Builder
//    public LLMGraphTransformer2(List<String> allowedNodes,
//                                List<String> allowedRelationships,
//                                List<ChatMessage> prompt,
//                                Boolean strictMode,
////                               Object nodeProperties,
////                               Object relationshipProperties,
//                                Boolean ignoreToolUsage,
//                                String additionalInstructions) {
//        
//        this.allowedNodes = getOrDefault(allowedNodes, List.of());
//        this.allowedRelationships = getOrDefault(allowedRelationships, List.of());
//        this.prompt = prompt;
//        this.strictMode = getOrDefault(strictMode, true);
////        this.nodeProperties = nodeProperties;
////        this.relationshipProperties = relationshipProperties;
//        this.ignoreToolUsage = getOrDefault(ignoreToolUsage, false);
//        this.additionalInstructions = getOrDefault(additionalInstructions, "");
//        
////        this.llmService = llmService;
//    }
//
////    public Graph transform(Graph graph) {
////        Graph transformedGraph = new Graph();
////
////        for (Node node : graph.getNodes()) {
////            Node transformedNode = transformNode(node);
////            transformedGraph.addNode(transformedNode);
////        }
////
////        for (Edge edge : graph.getEdges()) {
////            transformedGraph.addEdge(edge);
////        }
////
////        return transformedGraph;
////    }
////
////    private Node transformNode(Node node) {
////        String prompt = "Transform this node: " + node.getData();
////        String transformedData = llmService.callLLM(prompt);
////        return new Node(node.getId(), transformedData);
////    }
//
//    public List<GraphDocument> convertToGraphDocuments(List<Document> documents) {
//        return documents.stream().map(this::processResponse)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }
//
//    public List<ChatMessage> createUnstructuredPrompt(String text/*, List<String> nodeLabels, List<String> relTypes, String relationshipType, String additionalInstructions*/) {
//        // TODO - test with this
//        if (prompt != null && !prompt.isEmpty()) {
//            return prompt;
//        }
//        
//        String nodeLabelsStr = allowedNodes != null ? allowedNodes.toString() : "";
//        String relTypesStr = "";
//        if (allowedRelationships != null) {
////            if ("tuple".equals(relationshipType)) {
////                Set<String> uniqueRelTypes = new HashSet<>();
////                for (String rel : relTypes) {
////                    uniqueRelTypes.add(rel);
////                }
////                relTypesStr = uniqueRelTypes.toString();
////            } else {
//                relTypesStr = allowedRelationships.toString();
////            }
//        }
//        
//        /* TODO
//        StringBuilder message = new StringBuilder();
//
//if (relTypes != null && !relTypes.isEmpty()) {
//    message.append("");  // Equivalent to Python's `else ""`
//}
//
//if (nodeLabels != null && !nodeLabels.isEmpty()) {
//    message.append(String.format(
//        "The \"tail\" key must represent the text of an extracted entity which is " +
//        "the tail of the relation, and the \"tail_type\" key must contain the type " +
//        "of the tail entity from %s.", nodeLabelsStr
//    ));
//} else {
//    message.append(""); // Equivalent to Python's `else ""`
//}
//
//if ("tuple".equals(relationshipType)) {
//    message.append(String.format(
//        "Your task is to extract relationships from text strictly adhering " +
//        "to the provided schema. The relationships can only appear " +
//        "between specific node types are presented in the schema format " +
//        "like: (Entity1Type, RELATIONSHIP_TYPE, Entity2Type) /n" +
//        "Provided schema is %s.", relTypes
//    ));
//} else {
//    message.append("");
//}
//
//message.append(
//    "Attempt to extract as many entities and relations as you can. Maintain " +
//    "Entity Consistency: When extracting entities, it's vital to ensure " +
//    "consistency. If an entity, such as \"John Doe\", is mentioned multiple " +
//    "times in the text but is referred to by different names or pronouns " +
//    "(e.g., \"Joe\", \"he\"), always use the most complete identifier for " +
//    "that entity. The knowledge graph should be coherent and easily " +
//    "understandable, so maintaining consistency in entity references is " +
//    "crucial."
//);
//
//message.append("IMPORTANT NOTES:\n- Don't add any explanation and text. ");
//
//String finalMessage = message.toString(); // Convert StringBuilder to String
//
//         */
//
//        // TODO - optimize
//        String systemPrompt = String.join("\n", Arrays.asList(
//                "You are a top-tier algorithm designed for extracting information in structured formats to build a knowledge graph.",
//                "Your task is to identify entities and relations from a given text and generate output in JSON format.",
//                "Each object should have keys: 'head', 'head_type', 'relation', 'tail', and 'tail_type'.",
//                allowedNodes != null ? "The 'head_type' and 'tail_type' must be one of: " + nodeLabelsStr : "",
//                allowedRelationships != null ? "The 'relation' must be one of: " + relTypesStr : "",
//                "IMPORTANT NOTES:\n- Don't add any explanation or extra text.",
//                additionalInstructions
//        ));
//
//        final SystemMessage systemMessage = new SystemMessage(systemPrompt);
//
//        /* TODO
//        List<String> parts = new ArrayList<>();
//
//if (relTypes != null && !relTypes.isEmpty()) {
//    parts.add(""); // Equivalent to Python's `else ""`
//}
//
//if ("tuple".equals(relationshipType)) {
//    parts.add(String.format(
//        "Your task is to extract relationships from text strictly adhering " +
//        "to the provided schema. The relationships can only appear " +
//        "between specific node types presented in the schema format " +
//        "like: (Entity1Type, RELATIONSHIP_TYPE, Entity2Type) /n" +
//        "Provided schema is %s.", relTypes
//    ));
//}
//
//parts.add("Below are a number of examples of text and their extracted " +
//          "entities and relationships.\n{examples}\n");
//
//if (additionalInstructions != null && !additionalInstructions.isEmpty()) {
//    parts.add(additionalInstructions);
//}
//
//parts.add("For the following text, extract entities and relations as " +
//          "in the provided example.\n" +
//          "{format_instructions}\nText: {input}");
//
//String finalMessage = String.join("\n", parts);
//
//         */
//        
//        String humanPrompt = String.join("\n", Arrays.asList(
//                "Based on the following example, extract entities and relations from the provided text.",
//                allowedNodes != null ? "# ENTITY TYPES:\n" + nodeLabelsStr : "",
//                allowedRelationships != null ? "# RELATION TYPES:\n" + relTypesStr : "",
//                "For the following text, extract entities and relations as in the provided example.",
//                "Text: " + text
//        ));
//
//        final UserMessage userMessage = new UserMessage(humanPrompt);
//
//        return List.of(systemMessage, userMessage);
//    }
//
//    private GraphDocument processResponse(Document document) {
//        
//        /*
//        
//            TODO 
//           prompt = prompt or create_unstructured_prompt(
//                allowed_nodes,
//                allowed_relationships,
//                self._relationship_type,
//                additional_instructions,
//            )
//            
//            rawSchema = self.chain.invoke({"input": text}, config=config)
//         */
//        
//        // TODO - configurable chatLanguageModel
//        ChatLanguageModel chatModel = OpenAiChatModel.builder()
//                .baseUrl("http://langchain4j.dev/demo/openai/v1")
//                .apiKey("demo")
//                .organizationId(System.getenv("OPENAI_ORGANIZATION_ID"))
//                .modelName(GPT_4_O_MINI)
//                .logRequests(true)
//                .logResponses(true)
//                .build();
//
//        final String text = document.text();
//        
//        
//        // todo - configurable, pr 
//        //  use PromptTemplate handling via {{ }}
//        final List<ChatMessage> messages = createUnstructuredPrompt(text/*, null, null, null, ""*/);
//        // final PromptTemplate template = PromptTemplate.from(prompt);
//
//        System.out.println("messages = " + messages);
//        // TODO -- PromptTemplate
//
//        //final Prompt apply = template.apply(Map.of());
//        //final String text1 = apply.text();
//        
//        // TODO - DEPRECATED
//        String rawSchema = chatModel.generate(messages)
//                .content()
//                .text();
//
//        Set<Node> nodesSet = new HashSet<>();
//        Set<Edge> relationships = new HashSet<>();
//
//        List<Map<String, String>> parsedJson = parseJson(rawSchema);
//        if (parsedJson == null) {
//            // TODO - EVALUATE A RETRY MECHANISM CONFIGURABLE
//            System.out.println("parsedJson = " + parsedJson);
//            return null;
//        }
//
//        for (Map<String, String> rel : parsedJson) {
//            if (!rel.containsKey("head") || !rel.containsKey("tail") || !rel.containsKey("relation")) {
//                continue;
//            }
//
//            Node sourceNode = new Node(rel.get("head"), rel.getOrDefault("head_type", "DefaultNodeType"));
//            Node targetNode = new Node(rel.get("tail"), rel.getOrDefault("tail_type", "DefaultNodeType"));
//
//            nodesSet.add(sourceNode);
//            nodesSet.add(targetNode);
//
//            final String relation = rel.get("relation");
//            final Edge edge = new Edge(sourceNode, targetNode, relation);
//            relationships.add(edge);
////            relationships.add(new Edge(sourceNode.getId(), targetNode.getId(), rel.get("relation")));
//        }
//
////        List<Node> nodes = new ArrayList<>(nodesSet);
//        // TODO - REMOVE THE TOSTRING()
//        return new GraphDocument(nodesSet, relationships, document);
////        return new GraphDocument(nodes, relationships, document);
//    }
//
//    private List<Map<String, String>> parseJson(String jsonString) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        // TODO - COMMENT THIS, can output json``` sometimes
//        jsonString = jsonString.replaceAll("```", "")
//                .replaceAll("json", "");
//        try {
//            return objectMapper.readValue(jsonString, new TypeReference<List<Map<String, String>>>() {});
//        } catch (Exception e) {
//            System.out.println("e = " + e);
//            return null;
//            // throw new RuntimeException("Error parsing JSON", e);
//        }
//        // Placeholder for actual JSON parsing
//    }
//    
////    public List<GraphDocument> convertToGraphDocuments(List<Document> documents) {
////        return documents.stream()
////                .map(doc -> new GraphDocument(doc.getId(), doc.getContent()))
////                .collect(Collectors.toList());
////    }
//
//    // TODO - QUESTO IN REALTA DOVREBBE VERAMENTE GENERARE ENTITÀ
//    //  QUINDI VA IN UNA CLASSE SEPARATA IN TEORIA...
//    // TODO - METTERE UPPERCASE PER LE RELAZIONI, PERCHE PUO CAPITARE is_on INVECE DI IS_ON
//    //      CAPITALIZE LABEL???
//    public void addGraphDocuments(Graph graph, List<GraphDocument> graphDocuments, boolean includeSource, boolean baseEntityLabel) {
//        if (baseEntityLabel && !graph.constraintExists("BaseEntity", "id")) {
//            graph.createConstraint("BaseEntity", "id");
//        }
//
//            // TODO - STA ROBA VA IN NEO4J
////        for (GraphDocument doc : graphDocuments) {
////            if (doc.getId() == null || doc.getId().isEmpty()) {
////                doc.setId(generateMD5(doc.getContent()));
////            }
////
////            for (Node node : doc.getNodes()) {
////                node.setType(removeBackticks(node.getType()));
////                graph.addNode(node);
////            }
////
////            for (Edge edge : doc.getRelationships()) {
////                edge.setSourceLabel(removeBackticks(edge.getSourceLabel()));
////                edge.setTargetLabel(removeBackticks(edge.getTargetLabel()));
////                edge.setType(removeBackticks(edge.getType().replace(" ", "_").toUpperCase()));
////                graph.addEdge(edge);
////            }
////        }
//    }
//
//    public static String generateMD5(String input) {
//        try {
//            MessageDigest md = MessageDigest.getInstance("MD5");
//            md.update(input.getBytes());
//            byte[] digest = md.digest();
//            StringBuilder sb = new StringBuilder();
//            for (byte b : digest) {
//                sb.append(String.format("%02x", b));
//            }
//            return sb.toString();
//        } catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException("MD5 algorithm not found", e);
//        }
//    }
//
//    public static String removeBackticks(String input) {
//        return input.replace("`", "");
//    }
//}
