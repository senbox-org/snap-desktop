package org.esa.snap.gui.actions.file;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.SnapDialogs;
import org.openide.util.Cancellable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * @author Norman
 */
class ReadProductOperation implements Runnable, Cancellable {

    private final File file;
    private final String formatName;
    private Object status;

    public ReadProductOperation(File file, String formatName) {
        Assert.notNull(file, "file");
        this.file = file;
        this.formatName = formatName;
    }

    public Object getStatus() {
        return status;
    }

    @Override
    public boolean cancel() {
        SnapDialogs.Answer answer = SnapDialogs.requestDecision(Bundle.CTL_OpenProductActionName(),
                                                                "Do you really want to cancel the read process?",
                                                                false, null);
        return answer == SnapDialogs.Answer.YES;
    }

    @Override
    public void run() {
        try {
            Product product = formatName != null ? ProductIO.readProduct(file, formatName) : ProductIO.readProduct(file);
            if (!Thread.interrupted()) {
                if (product == null) {
                    status = null;
                    SwingUtilities.invokeLater(() -> {
                        SnapDialogs.showError(Bundle.LBL_NoReaderFoundText());
                    });
                } else {
                    OpenProductAction.getRecentProductPaths().add(file.getPath());
                    SwingUtilities.invokeLater(() -> SnapApp.getDefault().getProductManager().addProduct(product));
                    status = true;
                }
            } else {
                status = null;
            }
        } catch (IOException problem) {
            status = problem;
        }
    }
}
