package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.RepositoryProductListPanel;
import org.esa.snap.product.library.ui.v2.ThreadListener;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.model.RemoteMission;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 5/9/2019.
 */
public class LoadProductListTimerRunnable extends AbstractProgressTimerRunnable<List<RepositoryProduct>> {

    private final ThreadListener threadListener;
    private final LocalRepositoryFolder localRepositoryFolder;
    private final RepositoryProductListPanel repositoryProductListPanel;
    private final String remoteMissionName;
    private final Map<String, Object> parameterValues;
    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;

    public LoadProductListTimerRunnable(ProgressBarHelper progressPanel, int threadId, ThreadListener threadListener, LocalRepositoryFolder localRepositoryFolder,
                                        String remoteMissionName, Map<String, Object> parameterValues, RepositoryProductListPanel repositoryProductListPanel,
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
    protected List<RepositoryProduct> execute() throws Exception {
        return this.allLocalFolderProductsRepository.loadProductList(this.localRepositoryFolder, this.remoteMissionName, this.parameterValues);
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to read the product list from the database.";
    }

    @Override
    protected void onStopExecuting() {
        this.threadListener.onStopExecuting();
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
