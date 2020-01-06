package org.esa.snap.graphbuilder.gpf.core;

import java.util.List;
import java.util.ArrayList;

import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.NodeSource;

public class Utils {
    public static List<Node> affectedNodes(Graph graph, Node node) {
        ArrayList<Node> affected = new ArrayList<Node>();
        affected.add(node);
        Boolean changed = true;
        int n_nodes = graph.getNodeCount();
        while (changed) {
            changed = false;
            for (int i = 0; i < n_nodes; i++) {
                Node tmp = graph.getNode(i);
                if (!affected.contains(tmp)){
                    for (NodeSource source: tmp.getSources()) {
                        if (affected.contains(source.getSourceNode())) {
                            affected.add(tmp);
                            changed = true;
                        }
                    }
                }
            }
        }
        affected.remove(node);
        return affected;
    }

    public static List<Node> dependencyNodes(Node node) {
        ArrayList<Node> nodes = new ArrayList<Node>();
        for (NodeSource source: node.getSources()) {
            Node m = source.getSourceNode();
            nodes.add(m);
            nodes.addAll(dependencyNodes(m));
        }
        return nodes;
    }
}