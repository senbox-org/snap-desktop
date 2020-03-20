package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.engine_utilities.util.FileIOUtils;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryProduct;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.AppContext;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 27/9/2019.
 */
public class CopyLocalProductsRunnable extends AbstractProcessLocalProductsRunnable {

    private static final Logger logger = Logger.getLogger(CopyLocalProductsRunnable.class.getName());

    private final Path localTargetFolder;

    public CopyLocalProductsRunnable(ProgressBarHelper progressPanel, int threadId, RepositoryOutputProductListPanel repositoryProductListPanel, Path localTargetFolder, List<RepositoryProduct> productsToCopy) {
        super(progressPanel, threadId, repositoryProductListPanel, productsToCopy);

        this.localTargetFolder = localTargetFolder;
    }

    @Override
    protected boolean onTimerWakeUp(String message) {
        return super.onTimerWakeUp("Copy local products...");
    }

    @Override
    protected Void execute() throws Exception {
        for (int i = 0; i<this.productsToProcess.size(); i++) {
            LocalRepositoryProduct repositoryProduct = (LocalRepositoryProduct)this.productsToProcess.get(i);
            try {
                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.COPYING);

                FileIOUtils.copyFolderNew(repositoryProduct.getPath(), this.localTargetFolder);

                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.COPIED);
            } catch (Exception exception) {
                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.FAIL_COPIED);
                logger.log(Level.SEVERE, "Failed to copy the local product '" + repositoryProduct.getPath() + "'.", exception);
            }
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to copy the local products.";
    }
}
