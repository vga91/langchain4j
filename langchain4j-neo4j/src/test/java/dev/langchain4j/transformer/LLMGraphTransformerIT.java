package dev.langchain4j.transformer;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class LLMGraphTransformerIT {

    private static ChatLanguageModel model;
    
    @BeforeAll
    static void beforeAll() {
        model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .organizationId(System.getenv("OPENAI_ORGANIZATION_ID"))
                .modelName(GPT_4_O_MINI)
                .logRequests(true)
                .logResponses(true)
                .build();
    }


    @Test
    void testAddGraphDocumentsWithMissingModel() {
        try {
            LLMGraphTransformer.builder().build();
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("model cannot be null");
        }
    }

    @Test
    void testAddGraphDocumentsWithCustomPrompt() {
        final List<ChatMessage> prompt = List.of(new UserMessage("just return a null value, don't add any explanation or extra text."));
        
        final LLMGraphTransformer transformer = LLMGraphTransformer.builder()
                .model(model)
                .prompt(prompt)
                .build();

        Document doc3 = new DefaultDocument("Keanu Reeves acted in Matrix. Keanu was born in Beirut", Metadata.from("key3", "value3"));
        final List<GraphDocument> documents = transformer.convertToGraphDocuments(List.of(doc3));
        assertThat(documents).isEmpty();
    }

    @Test
    void testAddGraphDocumentsWithCustomNodesAndRelationshipsSchema() {
        String cat = "Sylvester the cat";
        String keanu = "Keanu Reeves";
        String lino = "Lino Banfi";
        String goku = "Goku";
        String hajime = "Hajime Isayama";
        String levi = "Levi";
        String table = "table";
        String matrix = "Matrix";
        String vac = "Vieni Avanti Cretino";
        String db = "Dragon Ball";
        String aot = "Attack On Titan";
        
        Document docCat = Document.from("%s is on the %s".formatted(cat, table));
        Document docKeanu = Document.from("%s acted in %s".formatted(keanu, matrix));
        Document docLino = Document.from("%s acted in %s".formatted(lino, vac));
        Document docGoku = Document.from("%s acted in %s".formatted(goku, db));
        Document docHajime = Document.from("%s wrote %s. %s acted in %s".formatted(hajime, aot, levi, aot));

        final List<Document> docs = List.of(docCat, docKeanu, docLino, docGoku, docHajime);
        
        final LLMGraphTransformer build2 = LLMGraphTransformer.builder()
                .model(model)
                .build();
        final List<GraphDocument> documents2 = build2.convertToGraphDocuments(docs);
        final Stream<String> expectedNodes = Stream.of(cat, keanu, lino, goku, hajime, levi, table, matrix, vac, db, aot);
        assertThat(documents2).hasSize(5);
        graphDocsAssertions(documents2, expectedNodes, Stream.of("acted", "acted", "acted", "acted", "wr.", "on"));


        final LLMGraphTransformer build = LLMGraphTransformer.builder()
                .model(model)
                .allowedNodes(List.of("Person"))
                .allowedRelationships(List.of("Acted_in"))
                .build();

        final List<GraphDocument> documents = build.convertToGraphDocuments(docs);
        System.out.println("documents = " + documents);
        assertThat(documents).hasSize(4);
        final String[] strings = {keanu, lino, goku, levi, matrix, vac, db, aot};
        graphDocsAssertions(documents, Stream.of(strings), Stream.of("acted", "acted", "acted", "acted"));
        //assertThat(collect2).isEqualTo(cat12);

        final LLMGraphTransformer build3 = LLMGraphTransformer.builder()
                .model(model)
                .allowedNodes(List.of("Person"))
                .allowedRelationships(List.of("Writes", "Acted_in"))
                .build();


        final List<GraphDocument> documents3 = build3.convertToGraphDocuments(docs);
        System.out.println("documents3 = " + documents3);
        assertThat(documents).hasSize(4);
        final String[] elements3 = {keanu, lino, goku, hajime, levi, matrix, vac, db, aot};

        graphDocsAssertions(documents3, Stream.of(elements3), Stream.of("acted", "acted", "acted", "acted", "wr."));
    }


    @Test
    void testAddGraphDocumentsWithDeDuplication() {
        final LLMGraphTransformer transformer = LLMGraphTransformer.builder()
                .model(model)
                .build();

        Document doc3 = new DefaultDocument("Keanu Reeves acted in Matrix. Keanu was born in Beirut", Metadata.from("key3", "value3"));

        final List<Document> documents = List.of(doc3);
        List<GraphDocument> graphDocs = transformer.convertToGraphDocuments(documents);

        // TODO - change assertions
        assertThat(graphDocs).hasSize(1);
//        final GraphDocument graphDocument = graphDocs.get(0);
//        final Document source = graphDocument.getSource();
//        assertThat(source).isEqualTo(doc3);
//        final Set<GraphDocument.Edge> relationships = graphDocument.getRelationships();
//        assertThat(relationships).hasSize(2);
//        final String relStrings = relationships.stream()
//                .map(Object::toString)
//                .collect(Collectors.joining("\n"));
//        assertThat(relStrings).containsIgnoringCase("acted");
//        assertThat(relStrings).containsIgnoringCase("born");
//        
//        final Set<GraphDocument.Node> nodes = graphDocument.getNodes();
//        assertThat(nodes).hasSize(3);
//        final String nodesStrings = nodes.stream()
//                .map(Object::toString)
//                .collect(Collectors.joining("\n"));
//        assertThat(nodesStrings).containsIgnoringCase("Matrix");
//        assertThat(nodesStrings).containsIgnoringCase("Keanu");
//        assertThat(nodesStrings).containsIgnoringCase("Beirut");

        graphDocsAssertions(graphDocs, Stream.of("matrix", "keanu", "beirut"), Stream.of("acted", "born"));
    }

    private static void graphDocsAssertions(List<GraphDocument> documents, Stream<String> expectedNodeElements, Stream<String> expectedNumRels) {
        final List<String> actualNodes = getNodeIds(documents);
        final List<String> actualRelationships = getRelationshipIds(documents);
        entitiesAssertions(expectedNodeElements, actualNodes);

        entitiesAssertions(expectedNumRels, actualRelationships);
    }

    private static void entitiesAssertions(Stream<String> expectedNodeElements, List<String> actualNodes) {
        final List<String> expectedNodes = expectedNodeElements
                .sorted()
                .toList();
        assertThat(actualNodes.size()).isEqualTo(expectedNodes.size());
        for (int i = 0; i < actualNodes.size(); i++) {
            assertThat(actualNodes.get(i).toLowerCase()).containsPattern("(?i)" + expectedNodes.get(i));
        }
    }

    private static List<String> getNodeIds(List<GraphDocument> documents2) {
        return documents2.stream()
                .flatMap(i -> i.getNodes().stream().map(GraphDocument.Node::getId))
                .sorted()
                .collect(Collectors.toList());
    }
    
    private static List<String> getRelationshipIds(List<GraphDocument> documents2) {
        return documents2.stream()
                .flatMap(i -> i.getRelationships().stream().map(GraphDocument.Edge::getType))
                .sorted()
                .collect(Collectors.toList());
    }
    
}
