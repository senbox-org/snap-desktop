package org.esa.snap.graphbuilder.gpf.core;

import java.util.HashMap;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.graph.NodeSource;

/**
 * Simple class used to store the intermediate products of the graph to be able to quickly validate or 
 * recompute the graph during editing.
 */
public class BuilderGraphContext {
    private Node[] nodes;

    private HashMap<Node, Product> products = new HashMap<>();

    /**
     * Initialize BuilderGraphContext with current graph.
     * 
     * @param graph current graph
     */
    public BuilderGraphContext(Graph graph) {
        this.nodes = graph.getNodes();
        for (Node node : nodes) {
            products.put(node, null);
        }
    }

    /** 
     * Updates the graph builder context.
     * @param graph updated graph
    */
    public void update(Graph graph) {
        for (Node node : graph.getNodes()) {
            if (!products.containsKey(node)) {
                products.put(node, null);
            }
        }
    }

    /**
     * Retrives list of source products of a node 
     * 
     * @param node selected
     * @return null if product list is incomplete else the list of products
     */
    public Product[] getSourceProducts(Node node) {
        Product[] sources = new Product[node.getSources().length];
        for (int i = 0; i < sources.length; i++) {
            Product p = products.get(node.getSource(i).getSourceNode());
            if (p == null)
                return null;
            sources[i] = p;
        }
        return sources;
    }

    /**
     * Stores the current output product of a specific node.
     * @param node source of the product
     * @param product to store
     */
    public void setProduct(Node node, Product product) {
        this.products.put(node, product);
    }

    /**
     * Evaluates a single node using the stored source products if available.
     * @param node to be evaluated
     * @return status of the node
     * @throws GraphException if an evaluation error happens.
     */
    public NodeStatus eval(Node node) throws GraphException {
        BuilderNodeContext context = new BuilderNodeContext(node);
        for (NodeSource src: node.getSources()) {
            Product product = products.get(src.getSourceNode());
            if (product == null) return NodeStatus.INCOMPLETE;
            context.addSource(src.getName(), product);
        }
        this.products.put(node, context.getProduct());
        return NodeStatus.VALIDATED;
    }
}