package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The thread to delete all the local repository folders.
 *
 * Created by jcoravu on 2/10/2019.
 */
public class DeleteAllLocalRepositoriesTimerRunnable extends AbstractProgressTimerRunnable<Void> {

    private static final Logger logger = Logger.getLogger(DeleteAllLocalRepositoriesTimerRunnable.class.getName());

    private final LocalRepositoryFolder[] localRepositoryFolders;
    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;

    public DeleteAllLocalRepositoriesTimerRunnable(ProgressBarHelper progressPanel, int threadId,
                                                   AllLocalFolderProductsRepository allLocalFolderProductsRepository,
                                                   LocalRepositoryFolder... localRepositoryFolders) {
        super(progressPanel, threadId, 500);

        this.localRepositoryFolders = localRepositoryFolders;
        this.allLocalFolderProductsRepository = allLocalFolderProductsRepository;
    }

    @Override
    protected final boolean onTimerWakeUp(String message) {
        return super.onTimerWakeUp("Delete all local repositories...");
    }

    @Override
    protected final Void execute() throws Exception {
        if (this.localRepositoryFolders != null) {
            for (LocalRepositoryFolder folder : this.localRepositoryFolders) {
                if (isFinished()) {
                    break;
                } else {
                    boolean folderDeletedFromDatabase = false;
                    try {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "Delete the local repository folder '" + folder.getPath().toString() + "'.");
                        }

                        this.allLocalFolderProductsRepository.deleteRepositoryFolder(folder);
                        folderDeletedFromDatabase = true;
                    } catch (Exception exception) {
                        logger.log(Level.SEVERE, "Failed to delete the local repository folder '" + folder.getPath().toString() + "'.", exception);
                    } finally {
                        if (folderDeletedFromDatabase) {
                            updateLocalRepositoryFolderDeletedLater(folder);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected final String getExceptionLoggingMessage() {
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
