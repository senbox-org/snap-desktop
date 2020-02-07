package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductListTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ThreadListener;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 5/9/2019.
 */
public class LoadProductListTimerRunnable extends AbstractProgressTimerRunnable<List<RepositoryProduct>> {

    private static final Logger logger = Logger.getLogger(LoadProductListTimerRunnable.class.getName());

    private final ThreadListener threadListener;
    private final LocalRepositoryFolder localRepositoryFolder;
    private final RepositoryOutputProductListPanel repositoryProductListPanel;
    private final String remoteMissionName;
    private final Map<String, Object> parameterValues;
    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;

    public LoadProductListTimerRunnable(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener, LocalRepositoryFolder localRepositoryFolder,
                                        String remoteMissionName, Map<String, Object> parameterValues, RepositoryOutputProductListPanel repositoryProductListPanel,
                                        AllLocalFolderProductsRepository allLocalFolderProductsRepository) {

        super(progressPanel, threadId, 500);

        this.threadListener = threadListener;
        this.localRepositoryFolder = localRepositoryFolder;
        this.repositoryProductListPanel = repositoryProductListPanel;
        this.remoteMissionName = remoteMissionName;
        this.parameterValues = parameterValues;
        this.allLocalFolderProductsRepository = allLocalFolderProductsRepository;
    }

    @Override
    public void cancelRunning() {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Cancel searching the product list on the local repository.");
        }

        super.cancelRunning();
    }

    @Override
    protected List<RepositoryProduct> execute() throws Exception {
        return this.allLocalFolderProductsRepository.loadProductList(this.localRepositoryFolder, this.remoteMissionName, this.parameterValues);
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to read the product list from the database.";
    }

    @Override
    protected void onFinishRunning() {
        this.threadListener.onStopExecuting(this);
    }

    @Override
    protected void onSuccessfullyFinish(List<RepositoryProduct> results) {
        this.repositoryProductListPanel.setProducts(results);
    }

    @Override
    protected void onFailed(Exception exception) {
        onShowErrorMessageDialog(this.repositoryProductListPanel, "Failed to read the product list from the database.", "Error");
    }
}
