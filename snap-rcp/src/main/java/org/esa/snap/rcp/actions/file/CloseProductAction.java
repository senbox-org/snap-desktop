/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.file;

import com.bc.jexp.ParseException;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.snap.netbeans.docwin.DocumentWindow;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.WeakSet;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Action which closes a selected product.
 *
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "CloseProductAction"
)
@ActionRegistration(
        displayName = "#CTL_CloseProductActionName"
)
@ActionReference(path = "Menu/File", position = 30)
@NbBundle.Messages({
        "CTL_CloseProductActionName=Close Product"
})
public final class CloseProductAction extends AbstractAction {

    private final WeakSet<Product> productSet;

    public CloseProductAction(List<Product> products) {
        productSet = new WeakSet<>();
        productSet.addAll(products);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        execute();
    }

    /**
     * Executes the action command.
     *
     * @return {@code Boolean.TRUE} on success, {@code Boolean.FALSE} on failure, or {@code null} on cancellation.
     */
    public Boolean execute() {
        return closeProducts(new HashSet<>(productSet));
    }

    private static Boolean closeProducts(Set<Product> products) {
        SnapDialogs.showInformation("Hi!", "x");
        List<Product> closeList = new ArrayList<>(products);
        List<Product> saveList = new ArrayList<>();

        Product[] products1 = SnapApp.getDefault().getProductManager().getProducts();
        HashSet<Product> stillOpenProducts = new HashSet<>(Arrays.asList(products1));
        stillOpenProducts.removeAll(closeList);

        if (!stillOpenProducts.isEmpty()) {
            for (Product productToBeClosed : closeList) {
                Product firstSourceProduct = findFirstSourceProduct(productToBeClosed, stillOpenProducts);
                if (firstSourceProduct != null) {
                    SnapDialogs.showInformation("Close Not Possible",
                                                MessageFormat.format(
                                                        "Can't close product ''{0}'' because it is in use\n" +
                                                                "by product ''{1}''.\n" +
                                                                "Please close the latter first.",
                                                        productToBeClosed.getName(),
                                                        firstSourceProduct.getName()), null);
                    return false;
                }
            }
        }

        for (Product product : products) {
            if (product.isModified()) {
                SnapDialogs.Answer answer = SnapDialogs.requestDecision(Bundle.CTL_OpenProductActionName(),
                                                                        MessageFormat.format("Product ''{0}'' has been modified.\n" +
                                                                                                     "Do you want to save it?",
                                                                                             product.getName()), true, null);
                if (answer == SnapDialogs.Answer.YES) {
                    saveList.add(product);
                } else if (answer == SnapDialogs.Answer.CANCELLED) {
                    return null;
                }
            }
        }

        for (Product product : saveList) {
            Boolean status = new SaveProductAction(product).execute();
            if (status == null) {
                // cancelled
                return null;
            }
        }

        for (Product product : closeList) {
            WindowUtilities.getOpened(DocumentWindow.class)
                    .filter(dw -> (dw.getDocument() instanceof ProductNode)
                            && ((ProductNode) dw.getDocument()).getProduct() == product)
                    .forEach(DocumentWindow::documentClosing);
            SnapApp.getDefault().getProductManager().removeProduct(product);
        }

        closeList.forEach(Product::dispose);
        return true;
    }

    private static Product findFirstSourceProduct(Product product, Set<Product> productsToBeClosed) {
        Product firstSourceProduct = findFirstDirectSourceProduct(product, productsToBeClosed);
        if (firstSourceProduct != null) {
            return firstSourceProduct;
        }
        return findFirstExpressionSourceProduct(product, productsToBeClosed);
    }

    private static Product findFirstDirectSourceProduct(Product product, Set<Product> productsToBeClosed) {
        final ProductReader reader = product.getProductReader();
        if (reader != null) {
            final Object input = reader.getInput();
            if (input instanceof Product) {
                Product sourceProduct = (Product) input;
                if (productsToBeClosed.contains(sourceProduct)) {
                    return sourceProduct;
                } else {
                    return findFirstDirectSourceProduct(sourceProduct, productsToBeClosed);
                }
            } else {
                if (input instanceof Product[]) {
                    for (final Product sourceProduct : (Product[]) input) {
                        if (productsToBeClosed.contains(sourceProduct)) {
                            return sourceProduct;
                        }
                        Product indirectSourceProduct = findFirstDirectSourceProduct(sourceProduct, productsToBeClosed);
                        if (indirectSourceProduct != null) {
                            return indirectSourceProduct;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Product findFirstExpressionSourceProduct(Product product, Set<Product> productsToBeClosed) {
        Band[] bands = product.getBands();
        for (Band band : bands) {
            if (band instanceof VirtualBand) {
                VirtualBand virtualBand = (VirtualBand) band;
                try {
                    RasterDataNode[] nodes = product.getRefRasterDataNodes(virtualBand.getExpression());
                    for (RasterDataNode node : nodes) {
                        if (productsToBeClosed.contains(node.getProduct())) {
                            return node.getProduct();
                        }
                    }
                } catch (ParseException e) {
                    // ok
                }
            }
        }
        return null;
    }
}
