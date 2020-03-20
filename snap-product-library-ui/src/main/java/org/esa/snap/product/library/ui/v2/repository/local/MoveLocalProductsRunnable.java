package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.engine_utilities.util.FileIOUtils;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductListModel;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryProduct;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.loading.PairRunnable;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 27/9/2019.
 */
public class MoveLocalProductsRunnable extends AbstractProcessLocalProductsRunnable {

    private static final Logger logger = Logger.getLogger(MoveLocalProductsRunnable.class.getName());

    private final Path localTargetFolder;
    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;

    public MoveLocalProductsRunnable(ProgressBarHelper progressPanel, int threadId, RepositoryOutputProductListPanel repositoryProductListPanel, Path localTargetFolder,
                                     List<RepositoryProduct> productsToMove, AllLocalFolderProductsRepository allLocalFolderProductsRepository) {

        super(progressPanel, threadId, repositoryProductListPanel, productsToMove);

        this.localTargetFolder = localTargetFolder;
        this.allLocalFolderProductsRepository = allLocalFolderProductsRepository;
    }

    @Override
    protected boolean onTimerWakeUp(String message) {
        return super.onTimerWakeUp("Move local products...");
    }

    @Override
    protected Void execute() throws Exception {
        for (int i = 0; i<this.productsToProcess.size(); i++) {
            LocalRepositoryProduct repositoryProduct = (LocalRepositoryProduct)this.productsToProcess.get(i);
            try {
                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.MOVING);

                Path newProductPath = FileIOUtils.moveFolderNew(repositoryProduct.getPath(), this.localTargetFolder);

                this.allLocalFolderProductsRepository.updateProductPath(repositoryProduct, newProductPath, this.localTargetFolder);

                Runnable runnable = new PairRunnable<LocalRepositoryProduct, Path>(repositoryProduct, newProductPath) {
                    @Override
                    protected void execute(LocalRepositoryProduct localRepositoryProduct, Path path) {
                        onFinishMoveLocalProduct(localRepositoryProduct, path);
                    }
                };
                SwingUtilities.invokeLater(runnable);
            } catch (Exception exception) {
                updateProductProgressStatusLater(repositoryProduct, LocalProgressStatus.FAIL_MOVED);
                logger.log(Level.SEVERE, "Failed to move the local product '" + repositoryProduct.getPath() + "'.", exception);
            }
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to move the local products.";
    }

    private void onFinishMoveLocalProduct(LocalRepositoryProduct localRepositoryProduct, Path path) {
        localRepositoryProduct.setPath(path);
        OutputProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
        productListModel.setLocalProductStatus(localRepositoryProduct, LocalProgressStatus.MOVED);
    }
}
