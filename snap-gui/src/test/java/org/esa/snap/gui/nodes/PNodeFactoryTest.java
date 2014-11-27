package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.Product;
import org.esa.snap.gui.util.TestProducts;
import org.junit.Test;
import org.openide.awt.UndoRedo;
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

        Node pNode = nodeFactory.createNodeForKey(product);
        assertTrue(pNode instanceof UndoRedo.Provider);
        assertEquals("Product_1", pNode.getDisplayName());

        Children children = pNode.getChildren();
        assertNotNull(children);

        Node[] groupNodes = children.getNodes();
        assertNotNull(groupNodes);
        assertEquals(5, groupNodes.length);
        Node mdGroupNode = groupNodes[0];
        assertEquals("Metadata", mdGroupNode.getDisplayName());
        assertEquals("Vector Data", groupNodes[1].getDisplayName());
        assertEquals("Tie-Point Grids", groupNodes[2].getDisplayName());
        assertEquals("Bands", groupNodes[3].getDisplayName());
        assertEquals("Masks", groupNodes[4].getDisplayName());
//        assertEquals("Flag-Codings", groupNodes[2].getDisplayName());
//        assertEquals("Index-Codings", groupNodes[3].getDisplayName());

        assertTrue(mdGroupNode instanceof UndoRedo.Provider);

    }
}
