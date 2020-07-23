package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.LocalRepositoryFolderHelper;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The thread class to scan all the local repository folders.
 *
 * Created by jcoravu on 4/10/2019.
 */
public class ScanAllLocalRepositoryFoldersTimerRunnable extends AbstractProgressTimerRunnable<Map<File, String>> {

    private static final Logger logger = Logger.getLogger(ScanAllLocalRepositoryFoldersTimerRunnable.class.getName());

    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;
    private final boolean scanRecursively;
    private final boolean generateQuickLookImages;
    private final boolean testZipFileForErrors;

    public ScanAllLocalRepositoryFoldersTimerRunnable(ProgressBarHelper progressPanel, int threadId, AllLocalFolderProductsRepository allLocalFolderProductsRepository,
                                                      boolean scanRecursively, boolean generateQuickLookImages, boolean testZipFileForErrors) {
        super(progressPanel, threadId, 500);

        this.allLocalFolderProductsRepository = allLocalFolderProductsRepository;
        this.scanRecursively = scanRecursively;
        this.generateQuickLookImages = generateQuickLookImages;
        this.testZipFileForErrors = testZipFileForErrors;
    }

    @Override
    protected boolean onTimerWakeUp(String message) {
        return super.onTimerWakeUp(null); // 'null' => do not reset the progress bar message
    }

    @Override
    protected Map<File, String> execute() throws Exception {
        List<LocalRepositoryFolder> localRepositoryFolders = this.allLocalFolderProductsRepository.loadRepositoryFolders();
        if (!isFinished()) {
            LocalRepositoryFolderHelper scanLocalProductsHelper = new LocalRepositoryFolderHelper(this.allLocalFolderProductsRepository, this.scanRecursively,
                                                                                    this.generateQuickLookImages, this.testZipFileForErrors);
            for (int i = 0; i < localRepositoryFolders.size(); i++) {
                if (isFinished()) {
                    break;
                } else {
                    LocalRepositoryFolder localRepositoryFolder = localRepositoryFolders.get(i);
                    try {
                        boolean deleteLocalFolderRepository = scanLocalProductsHelper.scanRepository(localRepositoryFolder, this, this);
                        if (deleteLocalFolderRepository) {
                            // no products saved and delete the local repository folder from the application
                            updateLocalRepositoryFolderDeletedLater(localRepositoryFolder);
                        }
                    } catch (java.lang.InterruptedException exception) {
                        logger.log(Level.FINE, "Stop scanning the local repository folders.");
                        break;
                    } catch (Exception exception) {
                        logger.log(Level.SEVERE, "Failed to scan the local repository folder '" + localRepositoryFolder.getPath().toString() + "'.", exception);
                    }
                }
            }
            return scanLocalProductsHelper.getErrorFiles();
        }
        return null;
    }

    @Override
    protected String getExceptionLoggingMessage() {
        return "Failed to scan the local repositories.";
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
