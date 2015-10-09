/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.file;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.dataio.dimap.DimapProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.netbeans.api.progress.ProgressUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;

/**
 * Action which closes a selected product.
 *
 * @author Norman
 */
@ActionID(
        category = "File",
        id = "SaveProductAction"
)
@ActionRegistration(
        displayName = "#CTL_SaveProductActionName",
        lazy = false
)
@ActionReference(path = "Menu/File", position = 40, separatorBefore = 38)
@NbBundle.Messages({"CTL_SaveProductActionName=Save Product"})
public final class SaveProductAction extends AbstractAction implements ContextAwareAction, LookupListener {


    private WeakReference<Product> productRef;
    private Lookup lookup;
    private Lookup.Result<ProductNode> result;


    public SaveProductAction(Product products) {
        productRef = new WeakReference<>(products);
    }


    public SaveProductAction() {
        this(Utilities.actionsGlobalContext());
    }

    public SaveProductAction(Lookup actionContext) {
        super(Bundle.CTL_SaveProductActionName());
        this.lookup = actionContext;
        result = lookup.lookupResult(ProductNode.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        setEnabled(false);
    }

    static Boolean saveProduct(Product product) {
        Assert.notNull(product.getFileLocation());
        final File file = product.getFileLocation();
        if (file.isFile() && !file.canWrite()) {
            SnapDialogs.showWarning(Bundle.CTL_SaveProductActionName(),
                                    MessageFormat.format("The product\n" +
                                                                 "''{0}''\n" +
                                                                 "exists and cannot be overwritten, because it is read only.\n" +
                                                                 "Please choose another file or remove the write protection.",
                                                         file.getPath()),
                                    null);
            return false;
        }

        SnapApp.getDefault().setStatusBarMessage(MessageFormat.format("Writing product ''{0}'' to {1}...", product.getDisplayName(), file));

        boolean incremental = true;
        WriteProductOperation operation = new WriteProductOperation(product, incremental);
        ProgressUtils.runOffEventThreadWithProgressDialog(operation,
                                                          Bundle.CTL_SaveProductActionName(),
                                                          operation.getProgressHandle(),
                                                          true,
                                                          50,
                                                          1000);

        SnapApp.getDefault().setStatusBarMessage("");

        return operation.getStatus();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new SaveProductAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        ProductNode productNode = lookup.lookup(ProductNode.class);
        setEnabled(productNode != null);
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
        Product product = null;
        if (productRef != null) {
            product = productRef.get();
        } else {
            product = SnapApp.getDefault().getSelectedProduct();
        }
        if (product != null) {
            if (product.getFileLocation() != null && (product.getProductReader() == null || product.getProductReader() instanceof DimapProductReader)) {
                return saveProduct(product);
            } else {
                // if file location not set, delegate to save-as
                return new SaveProductAsAction(product).execute();
            }
        } else {
            // reference was garbage collected, that's fine, no need to save.
            return true;
        }
    }


}
