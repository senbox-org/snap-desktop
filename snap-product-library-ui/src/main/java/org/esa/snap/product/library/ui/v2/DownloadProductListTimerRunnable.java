package org.esa.snap.product.library.ui.v2;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.ui.v2.table.CustomTable;
import org.esa.snap.product.library.v2.IProductsDownloaderListener;
import org.esa.snap.product.library.v2.IThread;
import org.esa.snap.product.library.v2.ProductLibraryItem;
import org.esa.snap.product.library.v2.SciHubDownloader;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
import org.esa.snap.ui.loading.GenericRunnable;
import org.esa.snap.ui.loading.ILoadingIndicator;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 9/8/2019.
 */
public class DownloadProductListTimerRunnable extends AbstractTimerRunnable<List<ProductLibraryItem>> {

    private final String sensor;
    private final Map<String, Object> parametersValues;
    private final String dataSourceName;
    private final ProductsTableModel productsTableModel;
    private final JComponent parentComponent;
    private final Credentials credentials;

    public DownloadProductListTimerRunnable(ILoadingIndicator loadingIndicator, int threadId, Credentials credentials,
                                            JComponent parentComponent, ProductsTableModel productsTableModel,
                                            String dataSourceName, String sensor, Map<String, Object> parametersValues) {

        super(loadingIndicator, threadId, 500);

        this.sensor = sensor;
        this.parametersValues = parametersValues;
        this.dataSourceName = dataSourceName;
        this.credentials = credentials;
        this.parentComponent = parentComponent;
        this.productsTableModel = productsTableModel;
    }

    @Override
    protected List<ProductLibraryItem> execute() throws Exception {
        IProductsDownloaderListener downloaderListener = new IProductsDownloaderListener() {
            @Override
            public void notifyProductCount(long productCount) {
                String loadingIndicatorMessage;
                if (productCount == 1) {
                    loadingIndicatorMessage = "Download " + productCount + " product from " + dataSourceName + "...";
                } else {
                    loadingIndicatorMessage = "Download " + productCount + " products from " + dataSourceName + "...";
                }
                notifyUpdateLoadingIndicatorMessageLater(loadingIndicatorMessage);
            }

            @Override
            public void notifyPageProducts(int pageNumber, List<ProductLibraryItem> pageResults) {
                notifyPageProductsLater(pageResults);
            }
        };
        IThread thread = new IThread() {
            @Override
            public boolean isRunning() {
                return DownloadProductListTimerRunnable.this.isRunning();
            }
        };
        return SciHubDownloader.downloadProductList(credentials, sensor, parametersValues, downloaderListener, thread, 1);
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to download the product list.";
    }

    @Override
    protected void onTimerWakeUp(String messageToDisplay) {
        onDisplayLoadingIndicatorMessage("Searching products to " + this.dataSourceName+"...");
    }

    @Override
    protected void onSuccessfullyFinish(List<ProductLibraryItem> results) {
        if (results.size() > 0) {
            // do nothing
        } else {
            onShowInformationDialog("No product available according to the filter values.", "Information");
        }
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorDialog("Failed to download the list containing the products.", "Error");
    }

    protected final Credentials getCredentials() {
        return credentials;
    }

    private void notifyPageProductsLater(List<ProductLibraryItem> pageResults) {
        Runnable runnable = new GenericRunnable<List<ProductLibraryItem>>(pageResults) {
            @Override
            protected void execute(List<ProductLibraryItem> results) {
                if (isRunning()) {
                    onDownloadPageProducts(results);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void onDownloadPageProducts(List<ProductLibraryItem> pageResults) {
        this.productsTableModel.addRecordsAndFireEvent(pageResults);
    }

    private void onShowErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(this.parentComponent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void onShowInformationDialog(String message, String title) {
        JOptionPane.showMessageDialog(this.parentComponent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
