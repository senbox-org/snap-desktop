/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.file;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.netbeans.docwin.DocumentWindow;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.util.WeakSet;


import static org.esa.snap.rcp.SnapApp.SelectionSourceHint.EXPLORER;

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
    private Collection collectionSelectProduct;
    private static Collection selectedProduct = new ArrayList();

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
        Lookup.Result result = (Lookup.Result) lookupEvent.getSource();
        selectedProduct = result.allInstances();
        setActionName();
    }

    private void setActionName() {
        if (selectedProduct.size() > 1) {
            this.putValue(Action.NAME, String.format("Close %d Products", selectedProduct.size()));
        } else {
            this.putValue(Action.NAME, "Close Product");
        }
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
        Boolean status;
        if (selectedProduct.size() > 1) {
            Set<Product> collect = (Set<Product>) selectedProduct.stream().collect(Collectors.toSet());
            status = closeProducts(collect);
            return status;
        }

        if (!productSet.isEmpty()) {
            // Case 1: If productSet is not empty, action has been constructed with selected products
            status = closeProducts(new HashSet<>(productSet));
            productSet.clear();
        } else {
            // Case 2: If productSet is empty, default constructor has been called
            ProductNode productNode = SnapApp.getDefault().getSelectedProductNode(EXPLORER);
            if (productNode != null) {
                Product product = productNode.getProduct();
                status = closeProducts(new HashSet<>(Collections.singletonList(product)));
            } else {
                status = false;
            }
        }
        return status;
    }


    private static Boolean closeProducts(Set<Product> products) {
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
