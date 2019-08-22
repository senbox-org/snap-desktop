package org.esa.snap.product.library.ui.v2;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.v2.IProductsDownloaderListener;
import org.esa.snap.product.library.v2.IThread;
import org.esa.snap.product.library.v2.ProductLibraryItem;
import org.esa.snap.product.library.v2.SciHubDownloader;
import org.esa.snap.ui.loading.AbstractTimerRunnable;
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
    private final JComponent parentComponent;
    private final Credentials credentials;
    private final QueryProductResultsPanel productResultsPanel;

    public DownloadProductListTimerRunnable(ILoadingIndicator loadingIndicator, int threadId, Credentials credentials,
                                            JComponent parentComponent, QueryProductResultsPanel productResultsPanel,
                                            String dataSourceName, String sensor, Map<String, Object> parametersValues) {

        super(loadingIndicator, threadId, 500);

        this.sensor = sensor;
        this.parametersValues = parametersValues;
        this.dataSourceName = dataSourceName;
        this.credentials = credentials;
        this.parentComponent = parentComponent;
        this.productResultsPanel = productResultsPanel;
    }

    @Override
    protected List<ProductLibraryItem> execute() throws Exception {
        IProductsDownloaderListener downloaderListener = new IProductsDownloaderListener() {
            @Override
            public void notifyProductCount(long totalProductCount) {
                String loadingIndicatorMessage = buildLoadingIndicatorMessage(totalProductCount, 0);
                notifyUpdateLoadingIndicatorMessageLater(loadingIndicatorMessage);
            }

            @Override
            public void notifyPageProducts(int pageNumber, List<ProductLibraryItem> pageResults, long totalProductCount, int retrievedProductCount) {
                notifyPageProductsLater(pageResults, totalProductCount, retrievedProductCount);
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
        return "Failed to retrieve the product list from '" + this.dataSourceName + "'.";
    }

    @Override
    protected void onTimerWakeUp(String messageToDisplay) {
        onDisplayLoadingIndicatorMessage("Retrieving product list from " + this.dataSourceName+"...");
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
        onShowErrorDialog("Failed to retrieve the product list from " + this.dataSourceName + ".", "Error");
    }

    protected final Credentials getCredentials() {
        return credentials;
    }

    private void notifyPageProductsLater(List<ProductLibraryItem> pageResults, long totalProductCount, int retrievedProductCount) {
        Runnable runnable = new ProductPageResultsRunnable(pageResults, totalProductCount, retrievedProductCount) {
            @Override
            protected void execute(List<ProductLibraryItem> results, long totalProductCount, int retrievedProductCount) {
                if (isRunning()) {
                    onDownloadPageProducts(results, totalProductCount, retrievedProductCount);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private String buildLoadingIndicatorMessage(long totalProductCount, int retrievedProductCount) {
        return "Retrieving product list from " + this.dataSourceName+": " + retrievedProductCount + " out of " + totalProductCount;
    }

    private void onDownloadPageProducts(List<ProductLibraryItem> results, long totalProductCount, int retrievedProductCount) {
//        ProductsTableModel productsTableModel = (ProductsTableModel)this.productsTable.getModel();
//        int[] selectedRows = this.productsTable.getSelectedRows();
//        productsTableModel.addAvailableProducts(results);
//        // selected again the rows
//        for (int i=0; i<selectedRows.length; i++) {
//            this.productsTable.setRowSelectionInterval(selectedRows[i], selectedRows[i]);
//        }


        this.productResultsPanel.addProducts(results);
        String loadingIndicatorMessage = buildLoadingIndicatorMessage(totalProductCount, retrievedProductCount);
        onDisplayLoadingIndicatorMessage(loadingIndicatorMessage);
    }

    private void onShowErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(this.parentComponent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void onShowInformationDialog(String message, String title) {
        JOptionPane.showMessageDialog(this.parentComponent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private static abstract class ProductPageResultsRunnable implements Runnable {

        private final List<ProductLibraryItem> pageResults;
        private final long totalProductCount;
        private final int retrievedProductCount;

        public ProductPageResultsRunnable(List<ProductLibraryItem> pageResults, long totalProductCount, int retrievedProductCount) {
            this.pageResults = pageResults;
            this.totalProductCount = totalProductCount;
            this.retrievedProductCount = retrievedProductCount;
        }

        protected abstract void execute(List<ProductLibraryItem> pageResults, long totalProductCount, int retrievedProductCount);

        @Override
        public void run() {
            execute(this.pageResults, this.totalProductCount, this.retrievedProductCount);
        }
    }
}
