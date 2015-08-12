/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.file;

import com.bc.jexp.ParseException;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.netbeans.docwin.DocumentWindow;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.*;

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
        displayName = "#CTL_CloseProductActionName", lazy = false
)

@ActionReferences({
        @ActionReference(path = "Menu/File", position = 20, separatorBefore = 18),
        @ActionReference(path = "Context/Product/Product", position = 60)
})
@NbBundle.Messages({
        "CTL_CloseProductActionName=Close Product"
})
public final class CloseProductAction extends AbstractAction implements ContextAwareAction, LookupListener {

    private WeakSet<Product> productSet = new WeakSet<>();
    private Lookup lkp;

    public CloseProductAction(List<Product> products) {
        productSet.addAll(products);
    }

    public CloseProductAction() {
        this(Utilities.actionsGlobalContext());
    }

    public CloseProductAction(Lookup actionContext) {
        super(Bundle.CTL_CloseProductActionName());
        this.lkp = actionContext;
        Lookup.Result<ProductNode> productNode = lkp.lookupResult(ProductNode.class);
        productNode.addLookupListener(WeakListeners.create(LookupListener.class, this, productNode));
        setEnableState();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new CloseProductAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        setEnableState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        execute();
    }


    private void setEnableState() {
        ProductNode productNode = lkp.lookup(ProductNode.class);
        setEnabled(productNode != null);
    }
    /**
     * Executes the action command.
     *
     * @return {@code Boolean.TRUE} on success, {@code Boolean.FALSE} on failure, or {@code null} on cancellation.
     */
    public Boolean execute() {
        if (productSet.isEmpty()) {
            Product product = SnapApp.getDefault().getSelectedProductNode().getProduct();
            productSet.add(product);
        }
        boolean result = closeProducts(new HashSet<>(productSet));
        for (Product aProductSet : productSet) {
            productSet.remove(aProductSet);
        }
        return result;
    }

    private static Boolean closeProducts(Set<Product> products) {
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
                            String.format("Can't close product '%s' because it is in use%n" +
                                            "by product '%s'.%n" +
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
                    .filter(dw -> (dw.getDocument() instanceof ProductNode) && ((ProductNode) dw.getDocument()).getProduct() == product)
                    .forEach(dw -> DocumentWindowManager.getDefault().closeWindow(dw));
            SnapApp.getDefault().getProductManager().removeProduct(product);
        }

        closeList.forEach(Product::dispose);

        return true;
    }

    static Product findFirstSourceProduct(Product productToClose, Set<Product> productsStillOpen) {
        Product firstSourceProduct = findFirstDirectSourceProduct(productToClose, productsStillOpen);
        if (firstSourceProduct != null) {
            return firstSourceProduct;
        }
        return findFirstExpressionSourceProduct(productToClose, productsStillOpen);
    }

    private static Product findFirstDirectSourceProduct(Product productToBeClosed, Set<Product> productsStillOpen) {
        for (Product openProduct : productsStillOpen) {
            final ProductReader reader = openProduct.getProductReader();
            if (reader != null) {
                final Object input = reader.getInput();
                if (input instanceof Product) {
                    Product sourceProduct = (Product) input;
                    if (productToBeClosed.equals(sourceProduct)) {
                        return openProduct;
                    } else {
                        Product indirectSourceProduct = findFirstDirectSourceProduct(sourceProduct, productsStillOpen);
                        if (indirectSourceProduct != null && productToBeClosed.equals(indirectSourceProduct)) {
                            return openProduct;
                        }
                    }
                } else {
                    if (input instanceof Product[]) {
                        for (final Product sourceProduct : (Product[]) input) {
                            if (productToBeClosed.equals(sourceProduct)) {
                                return openProduct;
                            }
                            Product indirectSourceProduct = findFirstDirectSourceProduct(sourceProduct, productsStillOpen);
                            if (indirectSourceProduct != null && productToBeClosed.equals(indirectSourceProduct)) {
                                return openProduct;
                            }
                        }
                    }
                }
            }

        }
        return null;
    }

    static Product findFirstExpressionSourceProduct(Product productToBeClosed, Set<Product> productsStillOpen) {
        for (Product openProduct : productsStillOpen) {
            Band[] bands = openProduct.getBands();
            for (Band band : bands) {
                if (band instanceof VirtualBand) {
                    VirtualBand virtualBand = (VirtualBand) band;
                    try {
                        RasterDataNode[] nodes = openProduct.getRefRasterDataNodes(virtualBand.getExpression());
                        for (RasterDataNode node : nodes) {
                            if (productToBeClosed.equals(node.getProduct())) {
                                return openProduct;
                            }
                        }
                    } catch (ParseException e) {
                        // ok
                    }
                }
            }
        }
        return null;
    }


}
