package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.engine_utilities.util.FileIOUtils;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.ProductLibraryDAL;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 2/10/2019.
 */
public class DeleteAllLocalProductsTimerRunnable extends AbstractProgressTimerRunnable<Void> {

    private static final Logger logger = Logger.getLogger(DeleteAllLocalProductsTimerRunnable.class.getName());

    private final List<LocalRepositoryFolder> localRepositoryFolders;

    public DeleteAllLocalProductsTimerRunnable(ProgressBarHelper progressPanel, int threadId, List<LocalRepositoryFolder> localRepositoryFolders) {
        super(progressPanel, threadId, 500);

        this.localRepositoryFolders = localRepositoryFolders;
    }

    @Override
    protected Void execute() throws Exception {
        for (int i=0; i<this.localRepositoryFolders.size(); i++) {
            LocalRepositoryFolder localRepositoryFolder = this.localRepositoryFolders.get(i);
            try {
                FileIOUtils.deleteFolder(localRepositoryFolder.getPath());
                ProductLibraryDAL.deleteLocalRepositoryFolder(localRepositoryFolder);
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Failed to delete the local repository folder '" + localRepositoryFolder.getPath().toString() + "'.", exception);
            }
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to delete the local repository folders from the disk and the product from the database.";
    }
}
