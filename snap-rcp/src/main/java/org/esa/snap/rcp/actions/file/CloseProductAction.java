/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.file;

import eu.esa.snap.netbeans.docwin.DocumentWindow;
import eu.esa.snap.netbeans.docwin.DocumentWindowManager;
import eu.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

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

    private final WeakSet<Product> productSet = new WeakSet<>();
    private Lookup lkp;

    public CloseProductAction() {
        this(Utilities.actionsGlobalContext());
    }

    public CloseProductAction(Lookup actionContext) {
        super(Bundle.CTL_CloseProductActionName());
        this.lkp = actionContext;
        Lookup.Result<ProductNode> productNode = lkp.lookupResult(ProductNode.class);
        productNode.addLookupListener(WeakListeners.create(LookupListener.class, this, productNode));
        setEnableState();
        setActionName();
    }

    public CloseProductAction(List<Product> products) {
        productSet.addAll(products);
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new CloseProductAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        setEnableState();
        setActionName();
    }

    private Set<Product> getSelectedProducts() {
        Collection<? extends ProductNode> selectedNodes = lkp.lookupAll(ProductNode.class);
        return selectedNodes.stream().map(ProductNode::getProduct).collect(Collectors.toSet());
    }

    private void setActionName() {
        Set<Product> selectedProducts = getSelectedProducts();
        if (selectedProducts.size() > 1) {
            this.putValue(Action.NAME, String.format("Close %d Products", selectedProducts.size()));
        } else {
            this.putValue(Action.NAME, "Close Product");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        execute();
    }

    private void setEnableState() {
        setEnabled(lkp.lookup(ProductNode.class) != null);
    }

    /**
     * Executes the action command.
     *
     * @return {@code Boolean.TRUE} on success, {@code Boolean.FALSE} on failure, or {@code null} on cancellation.
     */
    public Boolean execute() {
        Boolean status;
        if (!productSet.isEmpty()) {
            // Case 1: If productSet is not empty, action has been constructed with selected products
            status = closeProducts(productSet);
            productSet.clear();
        } else {
            // Case 2: If productSet is empty, default constructor has been called
            status = closeProducts(getSelectedProducts());
        }
        return status;
    }

    public static Boolean closeProducts(Set<Product> products) {
        List<Product> closeList = new ArrayList<>(products);
        List<Product> saveList = new ArrayList<>();

        HashSet<Product> stillOpenProducts = new HashSet<>(Arrays.asList(SnapApp.getDefault().getProductManager().getProducts()));
        stillOpenProducts.removeAll(closeList);

        if (!stillOpenProducts.isEmpty()) {
            for (Product productToBeClosed : closeList) {
                Product firstSourceProduct = findFirstSourceProduct(productToBeClosed, stillOpenProducts);
                if (firstSourceProduct != null) {
                    Dialogs.showInformation("Close Not Possible",
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
                Dialogs.Answer answer = Dialogs.requestDecision(Bundle.CTL_OpenProductActionName(),
                        MessageFormat.format("Product ''{0}'' has been modified.\n" +
                                        "Do you want to save it?",
                                product.getName()), true, null);
                if (answer == Dialogs.Answer.YES) {
                    saveList.add(product);
                } else if (answer == Dialogs.Answer.CANCELLED) {
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
