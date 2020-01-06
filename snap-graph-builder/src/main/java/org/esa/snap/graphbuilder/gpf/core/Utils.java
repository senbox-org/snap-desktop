package org.esa.snap.graphbuilder.gpf.core;

import java.util.List;
import java.util.ArrayList;

import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.NodeSource;

/*
Utility for graph exploration and reconstruction.
*/
public class Utils {
    /*
    Returns connected nodes.
    */
    public static List<Node> connectedNodes(Graph graph, Node node) {
        ArrayList<Node> connected = new ArrayList<Node>();
        int n_nodes = graph.getNodeCount();
        for (int i = 0; i < n_nodes; i++) {
            Node tmp = graph.getNode(i);
            for (NodeSource source: tmp.getSources()) {
                if (node == source.getSourceNode()) {
                    connected.add(tmp);
                }
            }
        }
        return connected;
    }

    /*
    Returns the list of nodes affected by one node.
    */
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

    /*
    Return list of input conntected nodes.
    */
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