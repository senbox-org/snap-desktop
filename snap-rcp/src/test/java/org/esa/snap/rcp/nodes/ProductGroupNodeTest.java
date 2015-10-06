package org.esa.snap.rcp.nodes;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.rcp.util.TestProducts;
import org.junit.Test;
import org.openide.awt.UndoRedo;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class ProductGroupNodeTest {
    @Test
    public void testDefaultTree() throws Exception {
        ProductManager productManager = new ProductManager();

        ProductGroupNode rootNode = new ProductGroupNode(productManager);

        Product product1 = TestProducts.createProduct1();
        productManager.addProduct(product1);

        assertEquals(1, rootNode.getChildren().getNodesCount());
        assertEquals(PNode.class, rootNode.getChildren().getNodeAt(0).getClass());
        PNode pNode = (PNode) rootNode.getChildren().getNodeAt(0);

        assertSame(product1, pNode.getProduct());
        assertEquals("[1] Test_Product_1", pNode.getDisplayName());

        Children children = pNode.getChildren();
        assertNotNull(children);

        Node[] groupNodes = children.getNodes();
        assertNotNull(groupNodes);
        assertEquals(5, groupNodes.length);
        assertEquals("Metadata", groupNodes[0].getDisplayName());
        assertEquals("Vector Data", groupNodes[1].getDisplayName());
        assertEquals("Tie-Point Grids", groupNodes[2].getDisplayName());
        assertEquals("Bands", groupNodes[3].getDisplayName());
        assertEquals("Masks", groupNodes[4].getDisplayName());
        //assertEquals("Flag-Codings", groupNodes[2].getDisplayName());
        //assertEquals("Index-Codings", groupNodes[3].getDisplayName());

        for (Node groupNode : groupNodes) {
            assertTrue(groupNode instanceof UndoRedo.Provider);
        }
    }
}
