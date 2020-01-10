package org.esa.snap.graphbuilder.gpf.core;

import java.util.HashMap;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.core.gpf.graph.NodeSource;

public class BuilderGraphContext {
    private Node[] nodes;

    private HashMap<Node, Product> products = new HashMap<>();

    public BuilderGraphContext(Graph graph) {
        this.nodes = graph.getNodes();
        for (Node node : nodes) {
            products.put(node, null);
        }
    }

    public void update(Graph graph) {
        for (Node node : graph.getNodes()) {
            if (!products.containsKey(node)) {
                products.put(node, null);
            }
        }
    }

    /**
     * Return soruce products
     * 
     * @param node
     * @return list of products
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

    public void setProduct(Node node, Product product) {
        this.products.put(node, product);
    }

    public boolean eval(Node n) throws GraphException {
        BuilderNodeContext context = new BuilderNodeContext(n);
        for (NodeSource src: n.getSources()) {
            Product product = products.get(src.getSourceNode());
            if (product == null) return false;
            context.addSource(src.getName(), product);
        }
        this.products.put(n, context.getProduct());
        return true;
    }
}