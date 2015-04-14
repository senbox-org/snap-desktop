package org.esa.snap.rcp.actions.file;

import com.bc.ceres.core.Assert;
import org.esa.snap.framework.dataio.ProductIO;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.openide.util.Cancellable;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;

/**
 * @author Norman
 */
class ReadProductOperation implements Runnable, Cancellable {

    private final File file;
    private final String formatName;
    private Boolean status;

    public ReadProductOperation(File file, String formatName) {
        Assert.notNull(file, "file");
        this.file = file;
        this.formatName = formatName;
    }

    public Boolean getStatus() {
        return status;
    }

    @Override
    public void run() {
        try {
            Product product = formatName != null ? ProductIO.readProduct(file, formatName) : ProductIO.readProduct(file);
            if (!Thread.interrupted()) {
                if (product == null) {
                    status = false;
                    SwingUtilities.invokeLater(() -> SnapDialogs.showError(Bundle.LBL_NoReaderFoundText()));
                } else {
                    status = true;
                    OpenProductAction.getRecentProductPaths().add(file.getPath());
                    SwingUtilities.invokeLater(() -> SnapApp.getDefault().getProductManager().addProduct(product));
                }
            } else {
                status = null;
            }
        } catch (IOException problem) {
            status = false;
            SwingUtilities.invokeLater(() -> {
                SnapDialogs.showError(Bundle.CTL_OpenProductActionName(), problem.getMessage());
            });
        }
    }

    @Override
    public boolean cancel() {
        SnapDialogs.Answer answer = SnapDialogs.requestDecision(Bundle.CTL_OpenProductActionName(),
                                                                "Do you really want to cancel the read process?",
                                                                false, null);
        boolean cancel = answer == SnapDialogs.Answer.YES;
        if (cancel) {
            status = null;
        }
        return cancel;
    }

}
