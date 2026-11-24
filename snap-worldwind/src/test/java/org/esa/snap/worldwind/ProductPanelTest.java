package org.esa.snap.worldwind;

import com.bc.ceres.annotation.STTM;
import gov.nasa.worldwind.WorldWindow;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.worldwind.layers.DefaultProductLayer;
import org.esa.snap.worldwind.layers.ProductImageLayer;
import org.junit.Test;

import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProductPanelTest {

    @Test
    @STTM("SNAP-4380")
    public void testProductsWithoutOutlineAreListedToHideTheirImage() throws Exception {
        final DefaultProductLayer productLayer = new DefaultProductLayer();
        final ProductImageLayer imageLayer = new ProductImageLayer();
        imageLayer.linkTo(productLayer);
        // reference numbers, as the product names in the list carry them
        final ProductManager productManager = new ProductManager();
        // outlines of wave mode products are made at once, from metadata
        final Product outlined = new Product("outlined", "ASA_WVW_2P", 10, 10);
        // drawn by a layer of its own (as S-1 OCN), so not in the product layer, but it may have an image
        final Product ocn = new Product("ocn", "OCN", 10, 10);
        productManager.addProduct(outlined);
        productManager.addProduct(ocn);

        final ProductPanel[] panel = new ProductPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            productLayer.addProduct(outlined, null);
            imageLayer.addProduct(outlined, null);
            imageLayer.addProduct(ocn, null);
            panel[0] = new ProductPanel(null, productLayer, imageLayer);
        });

        final List<JCheckBox> checkBoxes = panel[0].getCheckBoxes();
        assertEquals(2, checkBoxes.size());
        assertEquals(outlined.getProductRefString() + "outlined", checkBoxes.get(0).getText());
        assertEquals(ocn.getProductRefString() + "ocn", checkBoxes.get(1).getText());
        assertTrue(checkBoxes.get(1).isSelected());

        SwingUtilities.invokeAndWait(() -> checkBoxes.get(1).doClick());
        assertFalse(imageLayer.isProductVisible(ocn));
        assertTrue("only that product", imageLayer.isProductVisible(outlined));

        // the state survives a rebuild of the list
        SwingUtilities.invokeAndWait(() -> panel[0].update((WorldWindow) null));
        assertFalse(panel[0].getCheckBoxes().get(1).isSelected());

        // the outlined product is hidden through the product layer, which hides its image too
        SwingUtilities.invokeAndWait(() -> panel[0].getCheckBoxes().get(0).doClick());
        assertFalse(productLayer.isProductVisible(productLayer.getProductNames()[0]));
        assertFalse(imageLayer.isProductVisible(outlined));
    }
}
