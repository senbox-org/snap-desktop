package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.AddLocalRepositoryFolderHelper;
import org.esa.snap.product.library.v2.database.SaveProductData;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.SwingUtilities;
import java.nio.file.Path;

/**
 * Created by jcoravu on 3/10/2019.
 */
public class AddLocalRepositoryFolderTimerRunnable extends AbstractProgressTimerRunnable<Void> {

    private final Path localRepositoryFolderPath;

    public AddLocalRepositoryFolderTimerRunnable(ProgressBarHelper progressPanel, int threadId, Path localRepositoryFolderPath) {
        super(progressPanel, threadId, 500);

        this.localRepositoryFolderPath = localRepositoryFolderPath;
    }

    @Override
    protected Void execute() throws Exception {
        updateProgressBarTextLater("");

        AddLocalRepositoryFolderHelper saveLocalProductsHelper = new AddLocalRepositoryFolderHelper() {
            @Override
            protected void finishSavingProduct(SaveProductData saveProductData) {
                updateFinishSavingProductDataLater(saveProductData);
            }
        };
        saveLocalProductsHelper.addValidProductsFromFolder(this.localRepositoryFolderPath);

        return null;
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
