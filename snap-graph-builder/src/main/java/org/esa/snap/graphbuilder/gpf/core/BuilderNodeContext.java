package org.esa.snap.graphbuilder.gpf.core;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.Node;
import org.esa.snap.runtime.Config;

/**
 * Simplify NodeContext to be able to generate the target products of a given node 
 * using the information given by the BuilderGraphContext.
 */
public class BuilderNodeContext {
    private Node node;
    private Operator operator;

    /**
     * Initializes the node contenxt and recover the operator from the Operator registry.
     * 
     * @param node source of the context 
     */
    public BuilderNodeContext(Node node) throws GraphException{
        this.node = node;
        initOperator();   
    }

    private void initOperator() throws GraphException{
        final OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi operatorSpi = spiRegistry.getOperatorSpi(node.getOperatorName());
        if (operatorSpi == null) {
            String msg = Config.instance().preferences().get("snap.gpf.unsupported." + node.getOperatorName(), null);
            if (msg == null) {
                msg = "SPI not found for operator '" + node.getOperatorName() + "'";
            }
            throw new GraphException(msg);
        }

        try {
            this.operator = operatorSpi.createOperator();
        } catch (OperatorException e) {
            throw new GraphException("Failed to create instance of operator '" + node.getOperatorName() + "'", e);
        }
    }

    /**
     * Adds source product to the context.
     * @param id source product id
     * @param product source product
     */
    public void addSource(String id, Product product){
        this.operator.setSourceProduct(id, product);
    }
    
    /**
     * Creates and returns the target product of the node.
     * @return the target prodcut
     * @throws GraphException if any exection are thrown during the target product generation
     */
    public Product getProduct() throws GraphException {
        try {
            return operator.getTargetProduct();
        } catch (OperatorException e) {
            throw new GraphException("[NodeId: " + node.getId() + "] " + e.getMessage(), e);
        }
    }


}