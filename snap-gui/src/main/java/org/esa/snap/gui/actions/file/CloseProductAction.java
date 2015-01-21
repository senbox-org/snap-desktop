/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.actions.file;

import com.bc.jexp.ParseException;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.SnapDialogs;
import org.esa.snap.netbeans.docwin.DocumentWindow;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.WeakSet;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action which closes a selected product.
 *
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "org.esa.snap.gui.actions.file.CloseProductAction"
)
@ActionRegistration(
        displayName = "#CTL_CloseProductActionName"
)
@ActionReference(path = "Menu/File", position = 30)
@NbBundle.Messages({
        "CTL_CloseProductActionName=Close Product"
})
public final class CloseProductAction extends AbstractAction {

    private static final Logger LOG = Logger.getLogger(CloseProductAction.class.getName());

    private final WeakSet<Product> productSet;

    public CloseProductAction(List<Product> products) {
        productSet = new WeakSet<>();
        productSet.addAll(products);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        execute();
    }

    public void execute() {
        closeProducts(new HashSet<>(productSet));
    }

    static boolean closeProducts(Set<Product> products) {

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
                                                                        MessageFormat.format("Product ''{0}'' has been modified.\nDo you want to save it?", product.getName()), true, null);
                if (answer == SnapDialogs.Answer.YES) {
                    saveList.add(product);
                } else if (answer == SnapDialogs.Answer.CANCELLED) {
                    return false;
                }
            }
        }

        SwingWorker<List<IOException>, Object> swingWorker = new SwingWorker<List<IOException>, Object>() {
            @Override
            protected List<IOException> doInBackground() {
                List<IOException> problems = new ArrayList<>();
                for (Product product : saveList) {
                    // todo - save product
                    // SaveProductAction.saveProduct(product);
                    LOG.info("Saving to " + product.getFileLocation());
                }
                return problems;
            }

            @Override
            protected void done() {

                List<IOException> problems;
                try {
                    problems = get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                if (!problems.isEmpty()) {
                    StringBuilder problemsMessage = new StringBuilder();
                    problemsMessage.append(MessageFormat.format("<html>{0} problem(s) occurred:<br/>", problems.size()));
                    for (IOException problem : problems) {
                        LOG.log(Level.SEVERE, problem.getMessage(), problem);
                        problemsMessage.append(MessageFormat.format("<b>  {0}</b>: {1}<br/>", problem.getClass().getSimpleName(), problem.getMessage()));
                    }
                    SnapDialogs.showError(Bundle.CTL_OpenProductActionName(), problemsMessage.toString());
                }

                for (Product product : closeList) {
                    WindowUtilities.getOpened(DocumentWindow.class)
                            .filter(dw -> (dw.getDocument() instanceof ProductNode)
                                    && ((ProductNode) dw.getDocument()).getProduct() == product)
                            .forEach(DocumentWindow::documentClosing);
                    SnapApp.getDefault().getProductManager().removeProduct(product);
                }

                closeList.forEach(Product::dispose);
            }
        };

        swingWorker.execute();
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
