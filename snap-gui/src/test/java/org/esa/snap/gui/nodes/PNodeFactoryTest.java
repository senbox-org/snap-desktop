package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Product;
import org.esa.snap.gui.util.TestProducts;
import org.junit.Test;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class PNodeFactoryTest {
    @Test
    public void testFactory() throws Exception {
        PNodeFactory nodeFactory = new PNodeFactory();
        Product product1 = TestProducts.createProduct1();
        nodeFactory.addProduct(product1);

        ArrayList<Product> list = new ArrayList<>();
        nodeFactory.createKeys(list);

        assertEquals(1, list.size());

        Product product = list.get(0);
        assertSame(product1, product);

        Node productNode = nodeFactory.createNodeForKey(product);
        assertNotNull(productNode);
        assertEquals("Product_1", productNode.getDisplayName());

        Children children = productNode.getChildren();
        assertNotNull(children);

        Node[] groupNodes = children.getNodes();
        assertNotNull(groupNodes);
        // todo - why groupNodes.length==1 ?
        //assertEquals(1, groupNodes.length);
    }
}
