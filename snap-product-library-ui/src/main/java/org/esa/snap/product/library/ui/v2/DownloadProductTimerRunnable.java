package org.esa.snap.product.library.ui.v2;

import org.esa.snap.product.library.v2.IProgressListener;
import org.esa.snap.product.library.v2.ProductLibraryItem;
import org.esa.snap.product.library.v2.SciHubDownloader;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.ILoadingIndicator;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import java.nio.file.Path;

/**
 * Created by jcoravu on 19/8/2019.
 */
public class DownloadProductTimerRunnable extends AbstractTimerRunnable<Void> {

    private final String dataSourceName;
    private final ProductLibraryItem selectedProduct;
    private final Path selectedFolderPath;
    private final JComponent parentComponent;

    public DownloadProductTimerRunnable(ILoadingIndicator loadingIndicator, int threadId, String dataSourceName,
                                        ProductLibraryItem selectedProduct, Path selectedFolderPath, JComponent parentComponent) {

        super(loadingIndicator, threadId, 500);

        this.dataSourceName = dataSourceName;
        this.selectedProduct = selectedProduct;
        this.selectedFolderPath = selectedFolderPath;
        this.parentComponent = parentComponent;
    }

    @Override
    protected void onTimerWakeUp(String messageToDisplay) {
        String loadingIndicatorMessage = buildLoadingIndicatorMessage((short)0);
        onDisplayLoadingIndicatorMessage(loadingIndicatorMessage);
    }

    @Override
    protected Void execute() throws Exception {
        IProgressListener progressListener = new IProgressListener() {
            @Override
            public void notifyProgress(short progressValue) {
                String loadingIndicatorMessage = buildLoadingIndicatorMessage(progressValue);
                notifyUpdateLoadingIndicatorMessageLater(loadingIndicatorMessage);
            }
        };
        SciHubDownloader.downloadProduct(this.selectedProduct, this.selectedFolderPath.toString(), progressListener);
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to download the product from '" + this.dataSourceName + "'.";
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorDialog("Failed to download the product from " + this.dataSourceName + ".", "Error");
    }

    private void onShowErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(this.parentComponent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private String buildLoadingIndicatorMessage(short progressValue) {
        return "Download product from " + this.dataSourceName+": " + progressValue + "%";
    }
}
