package org.esa.snap.rcp.actions.file;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.Cancellable;
import org.openide.util.RequestProcessor;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

class ReadProductOperation implements Runnable {

    private static final Logger logger = Logger.getLogger(ReadProductOperation.class.getName());

    private final File file;
    private final String formatName;
    private ProgressWrapper ph;
    private ProductSubsetDef productSubsetDef = null;
    private ProductReaderPlugIn plugin = null;

    public ReadProductOperation(File file, String formatName) {
        Assert.notNull(file, "file");
        Assert.notNull(formatName, "formatName");
        this.file = file;
        this.formatName = formatName;
        ph = PhWrapper.NULL;
    }

    public File getFile() {
        return file;
    }

    public String getFormatName() {
        return formatName;
    }

    public Cancellable createCancellable(RequestProcessor.Task task) {
        return new Cancel(this, task);
    }

    public void attacheProgressHandle(ProgressHandle handle) {
        ph = new PhWrapper(handle);
    }

    @Override
    public void run() {
        try {
            ph.start();
            ph.switchToIndeterminate();
            Product product;
            if(productSubsetDef==null) {
                product = ProductIO.readProduct(file, formatName);
            }else{
                product = plugin.createReaderInstance().readProductNodes(file,productSubsetDef);
            }
            boolean interrupted = Thread.interrupted();
            if (!interrupted) {
                if (product == null) {
                    SwingUtilities.invokeLater(
                            () -> Dialogs.showError(Bundle.LBL_NoReaderFoundText() + String.format("%nFile '%s' can not be opened.", file)));
                } else {
                    OpenProductAction.getRecentProductPaths().add(file.getPath());
                    SwingUtilities.invokeLater(() -> SnapApp.getDefault().getProductManager().addProduct(product));
                }
            }
        } catch (IOException problem) {
            logger.log(Level.SEVERE, "Failed to read the product.", problem);
            SwingUtilities.invokeLater(() -> Dialogs.showError(Bundle.CTL_OpenProductActionName(), problem.getMessage()));
        } finally {
            ph.finish();
        }
    }

    static class Cancel implements Cancellable {

        private final ReadProductOperation operation;
        private final RequestProcessor.Task task;

        public Cancel(ReadProductOperation operation, RequestProcessor.Task task) {
            this.operation = operation;
            this.task = task;
        }

        @Override
        public boolean cancel() {
            task.cancel();
            operation.ph.finish();
            return true;
        }
    }

    private static abstract class ProgressWrapper {

        void start() {
        }

        void switchToIndeterminate() {
        }

        void finish() {
        }

    }

    private static class PhWrapper extends ProgressWrapper {

        public static ProgressWrapper NULL = new ProgressWrapper() {};

        private final ProgressHandle handle;

        public PhWrapper(ProgressHandle handle) {
            this.handle = handle;
        }

        @Override
        void start() {
            handle.start();
        }

        @Override
        void switchToIndeterminate() {
            handle.switchToIndeterminate();
        }

        @Override
        void finish() {
            handle.finish();
        }
    }

    public void setProductSubsetDef(ProductSubsetDef productSubsetDef) {
        this.productSubsetDef = productSubsetDef;
    }

    public void setProductReaderPlugIn(ProductReaderPlugIn plugin) {
        this.plugin = plugin;
    }
}
