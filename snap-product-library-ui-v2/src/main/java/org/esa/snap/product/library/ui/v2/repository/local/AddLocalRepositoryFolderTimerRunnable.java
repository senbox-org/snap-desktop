package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.LocalRepositoryFolderHelper;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.SaveProductData;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The thread to add a local folder as a repository.
 *
 * Created by jcoravu on 3/10/2019.
 */
public class AddLocalRepositoryFolderTimerRunnable extends AbstractProgressTimerRunnable<Map<File, String>> {

    private static final Logger logger = Logger.getLogger(AddLocalRepositoryFolderTimerRunnable.class.getName());

    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;
    private final Path localRepositoryFolderPath;
    private final boolean scanRecursively;
    private final boolean generateQuickLookImages;
    private final boolean validateZips;

    public AddLocalRepositoryFolderTimerRunnable(ProgressBarHelper progressPanel, int threadId, Path localRepositoryFolderPath,
                                                 AllLocalFolderProductsRepository allLocalFolderProductsRepository, boolean scanRecursively,
                                                 boolean generateQuickLookImages, boolean validateZips) {

        super(progressPanel, threadId, 500);

        this.allLocalFolderProductsRepository = allLocalFolderProductsRepository;
        this.localRepositoryFolderPath = localRepositoryFolderPath;
        this.scanRecursively = scanRecursively;
        this.generateQuickLookImages = generateQuickLookImages;
        this.validateZips = validateZips;
    }

    @Override
    protected final boolean onTimerWakeUp(String message) {
        return super.onTimerWakeUp(null); // 'null' => do not reset the progress bar message
    }

    @Override
    protected final Map<File, String> execute() throws Exception {
        try {
            LocalRepositoryFolderHelper saveLocalProductsHelper = new LocalRepositoryFolderHelper(this.allLocalFolderProductsRepository,
                                                                                    this.scanRecursively, this.generateQuickLookImages, this.validateZips) {
                @Override
                protected void finishSavingProduct(SaveProductData saveProductData) {
                    updateFinishSavingProductDataLater(saveProductData);
                }
            };
            saveLocalProductsHelper.addRepository(this.localRepositoryFolderPath, this, this);
            return saveLocalProductsHelper.getErrorFiles();
        } catch (java.lang.InterruptedException exception) {
            logger.log(Level.FINE, "Stop adding products from the local repository folder '" + this.localRepositoryFolderPath+"'.");
            return null; // nothing to return
        }
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to add the local repository folder '"+this.localRepositoryFolderPath.toString()+"'.";
    }

    protected void onFinishSavingProduct(SaveProductData saveProductData) {
    }

    private void updateFinishSavingProductDataLater(SaveProductData saveProductData) {
        GenericRunnable<SaveProductData> runnable = new GenericRunnable<SaveProductData>(saveProductData) {
            @Override
            protected void execute(SaveProductData item) {
                onFinishSavingProduct(item);
            }
        };
        SwingUtilities.invokeLater(runnable);
    }
}
