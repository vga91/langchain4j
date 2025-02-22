package dev.langchain4j.transformer;

import dev.langchain4j.data.document.Document;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Getter
public class GraphDocument {
    private Set<Node> nodes;
    private Set<Edge> relationships;
    private Document source;

    public GraphDocument(Set<GraphDocument.Node> nodes, Set<GraphDocument.Edge> relationships, Document source) {
        this.nodes = nodes;
        this.relationships = relationships;
        this.source = source;
    }

//    public Document getSource() {
//        return source;
//    }
//
//    public Set<GraphDocument.Node> getNodes() {
//        return nodes;
//    }
//
//    public Set<GraphDocument.Edge> getRelationships() {
//        return relationships;
//    }

    @Getter
    @Setter
    @EqualsAndHashCode // TODO - WRITE that is to de-duplicate nodes, e.g. testAddGraphDocumentsWithDeDuplication
    // @JsonSerialize
    @ToString // for testing purpose
    public static class Node {
        private String id;
        //        private String data;
        private String type;

        public Node(String id, String type) {
            this.id = id;
            this.type = type;
        }
    }

    @Getter
    @EqualsAndHashCode
    // @JsonSerialize
    @ToString // for testing purpose
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

    }
}
