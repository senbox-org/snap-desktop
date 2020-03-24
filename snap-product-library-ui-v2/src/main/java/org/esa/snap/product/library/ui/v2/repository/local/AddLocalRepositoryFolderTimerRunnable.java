package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.AddLocalRepositoryFolderHelper;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.SaveProductData;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.*;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 3/10/2019.
 */
public class AddLocalRepositoryFolderTimerRunnable extends AbstractProgressTimerRunnable<Void> {

    private static final Logger logger = Logger.getLogger(AddLocalRepositoryFolderTimerRunnable.class.getName());

    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;
    private final Path localRepositoryFolderPath;

    public AddLocalRepositoryFolderTimerRunnable(ProgressBarHelper progressPanel, int threadId, Path localRepositoryFolderPath,
                                                 AllLocalFolderProductsRepository allLocalFolderProductsRepository) {

        super(progressPanel, threadId, 500);

        this.allLocalFolderProductsRepository = allLocalFolderProductsRepository;
        this.localRepositoryFolderPath = localRepositoryFolderPath;
    }

    @Override
    protected final boolean onTimerWakeUp(String message) {
        return super.onTimerWakeUp("Add local repository folder...");
    }

    @Override
    protected final Void execute() throws Exception {
        try {
            AddLocalRepositoryFolderHelper saveLocalProductsHelper = new AddLocalRepositoryFolderHelper(this.allLocalFolderProductsRepository) {
                @Override
                protected void finishSavingProduct(SaveProductData saveProductData) {
                    updateFinishSavingProductDataLater(saveProductData);
                }
            };
            saveLocalProductsHelper.addValidProductsFromFolder(this.localRepositoryFolderPath, this);
        } catch (java.lang.InterruptedException exception) {
            logger.log(Level.FINE, "Stop adding products from the local repository folder '" + this.localRepositoryFolderPath+"'.");
        }
        return null; // nothing to return
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
