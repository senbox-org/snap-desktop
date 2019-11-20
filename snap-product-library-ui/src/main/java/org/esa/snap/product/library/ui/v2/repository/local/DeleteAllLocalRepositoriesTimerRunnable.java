package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.LocalRepositoryFolder;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 2/10/2019.
 */
public class DeleteAllLocalRepositoriesTimerRunnable extends AbstractProgressTimerRunnable<Void> {

    private static final Logger logger = Logger.getLogger(DeleteAllLocalRepositoriesTimerRunnable.class.getName());

    private final List<LocalRepositoryFolder> localRepositoryFolders;
    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;

    public DeleteAllLocalRepositoriesTimerRunnable(ProgressBarHelper progressPanel, int threadId, List<LocalRepositoryFolder> localRepositoryFolders,
                                                   AllLocalFolderProductsRepository allLocalFolderProductsRepository) {
        super(progressPanel, threadId, 500);

        this.localRepositoryFolders = localRepositoryFolders;
        this.allLocalFolderProductsRepository = allLocalFolderProductsRepository;
    }

    @Override
    protected Void execute() throws Exception {
        for (int i=0; i<this.localRepositoryFolders.size(); i++) {
            LocalRepositoryFolder localRepositoryFolder = this.localRepositoryFolders.get(i);
            boolean folderDeletedFromDatabase = false;
            try {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Delete the local repository folder '" + localRepositoryFolder.getPath().toString()+"'.");
                }

                this.allLocalFolderProductsRepository.deleteRepositoryFolder(localRepositoryFolder);
                folderDeletedFromDatabase = true;
                //if (Files.exists(localRepositoryFolder.getPath())) {
                //    FileIOUtils.deleteFolder(localRepositoryFolder.getPath());
                //}
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Failed to delete the local repository folder '" + localRepositoryFolder.getPath().toString() + "'.", exception);
            } finally {
                if (folderDeletedFromDatabase) {
                    updateLocalRepositoryFolderDeletedLater(localRepositoryFolder);
                }
            }
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to delete the local repository folders from the disk and the product from the database.";
    }

    protected void onLocalRepositoryFolderDeleted(LocalRepositoryFolder localRepositoryFolder) {
    }

    private void updateLocalRepositoryFolderDeletedLater(LocalRepositoryFolder localRepositoryFolder) {
        Runnable runnable = new GenericRunnable<LocalRepositoryFolder>(localRepositoryFolder) {
            @Override
            protected void execute(LocalRepositoryFolder item) {
                if (isCurrentProgressPanelThread()) {
                    onLocalRepositoryFolderDeleted(item);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }
}
