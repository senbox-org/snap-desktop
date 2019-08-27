package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressPanel;
import org.esa.snap.product.library.v2.DataSourceProductDownloader;
import org.esa.snap.product.library.v2.ProgressListener;
import org.esa.snap.product.library.v2.ProductLibraryItem;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Created by jcoravu on 19/8/2019.
 */
public class DownloadProductTimerRunnable extends AbstractProgressTimerRunnable<Void> {

    private final String dataSourceName;
    private final ProductLibraryItem selectedProduct;
    private final JComponent parentComponent;
    private final QueryProductResultsPanel productResultsPanel;
    private final DataSourceProductDownloader dataSourceProductDownloader;

    public DownloadProductTimerRunnable(ProgressPanel progressPanel, int threadId, String dataSourceName,
                                        DataSourceProductDownloader dataSourceProductDownloader, ProductLibraryItem selectedProduct,
                                        QueryProductResultsPanel productResultsPanel, JComponent parentComponent) {

        super(progressPanel, threadId, 500);

        this.dataSourceName = dataSourceName;
        this.dataSourceProductDownloader = dataSourceProductDownloader;
        this.selectedProduct = selectedProduct;
        this.parentComponent = parentComponent;
        this.productResultsPanel = productResultsPanel;
    }

    @Override
    protected Void execute() throws Exception {
        notifyDownloadingProgressValueLater((short)0);

        ProgressListener progressListener = new ProgressListener() {
            @Override
            public void notifyProgress(short progressPercent) {
                notifyDownloadingProgressValueLater(progressPercent);
            }
        };
        this.dataSourceProductDownloader.download(this.selectedProduct, progressListener);

        // successfully downloaded the product
        notifyDownloadingProgressValueLater((short)100);

        return null; // nothing to return
    }

    @Override
    public void stopRunning() {
        super.stopRunning();

        this.dataSourceProductDownloader.cancel();
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to download the product from '" + this.dataSourceName + "'.";
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorMessageDialog("Failed to download the product from " + this.dataSourceName + ".", "Error");
    }

    private void onShowErrorMessageDialog(String message, String title) {
        JOptionPane.showMessageDialog(this.parentComponent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void notifyDownloadingProgressValueLater(short progressPercent) {
        GenericRunnable<Short> runnable = new GenericRunnable<Short>(progressPercent) {
            @Override
            protected void execute(Short progressPercentValue) {
                if (isCurrentProgressPanelThread()) {
                    productResultsPanel.setProductDownloadPercent(selectedProduct, progressPercentValue);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }
}
