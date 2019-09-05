package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressPanel;
import org.esa.snap.product.library.v2.database.DerbyDAL;
import org.esa.snap.remote.products.repository.ProductRepositoryDownloader;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.listener.ProgressListener;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.nio.file.Path;

/**
 * Created by jcoravu on 19/8/2019.
 */
public class DownloadProductTimerRunnable extends AbstractProgressTimerRunnable<Path> {

    private final String dataSourceName;
    private final RepositoryProduct productToDownload;
    private final JComponent parentComponent;
    private final RemoteRepositoryProductListPanel productResultsPanel;
    private final ProductRepositoryDownloader productRepositoryDownloader;
    private final Path localRepositoryFolderPath;
    private final ThreadListener threadListener;

    public DownloadProductTimerRunnable(ProgressPanel progressPanel, int threadId, String dataSourceName, ThreadListener threadListener,
                                        ProductRepositoryDownloader productRepositoryDownloader, RepositoryProduct productToDownload,
                                        Path localRepositoryFolderPath, RemoteRepositoryProductListPanel productResultsPanel, JComponent parentComponent) {

        super(progressPanel, threadId, 500);

        this.dataSourceName = dataSourceName;
        this.localRepositoryFolderPath = localRepositoryFolderPath;
        this.productRepositoryDownloader = productRepositoryDownloader;
        this.productToDownload = productToDownload;
        this.parentComponent = parentComponent;
        this.productResultsPanel = productResultsPanel;
        this.threadListener = threadListener;
    }

    @Override
    protected Path execute() throws Exception {
        notifyDownloadingProgressValueLater((short)0);

        ProgressListener progressListener = new ProgressListener() {
            @Override
            public void notifyProgress(short progressPercent) {
                notifyDownloadingProgressValueLater(progressPercent);
            }
        };
        Path productMetadataFilePath = this.productRepositoryDownloader.download(this.productToDownload, this.localRepositoryFolderPath, progressListener);

        // successfully downloaded the product
        notifyDownloadingProgressValueLater((short)100);

//        Path productMetadataFilePath = java.nio.file.Paths.get("D:\\_download-sentinel2\\S2B_MSIL1C_20190803T070629_N0208_R106_T38NQG_20190803T105217.SAFE", "MTD_MSIL1C.xml");
        DerbyDAL.saveProduct(this.productToDownload, productMetadataFilePath, this.productRepositoryDownloader.getRepositoryId(), this.localRepositoryFolderPath);

        return productMetadataFilePath;
    }

    @Override
    protected void onStopExecuting() {
        this.threadListener.onStopExecuting(null);
    }

    @Override
    public void stopRunning() {
        super.stopRunning();

        this.productRepositoryDownloader.cancel();
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to download the product from '" + this.dataSourceName + "'.";
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorMessageDialog(this.parentComponent, "Failed to download the product from " + this.dataSourceName + ".", "Error");
    }

    private void notifyDownloadingProgressValueLater(short progressPercent) {
        GenericRunnable<Short> runnable = new GenericRunnable<Short>(progressPercent) {
            @Override
            protected void execute(Short progressPercentValue) {
                if (isCurrentProgressPanelThread()) {
                    productResultsPanel.setProductDownloadPercent(productToDownload, progressPercentValue);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }
}
