package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.SaveProductData;
import org.esa.snap.product.library.v2.database.ScanLocalRepositoryFolderHelper;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 4/10/2019.
 */
public class ScanAllLocalRepositoryFoldersTimerRunnable extends AbstractProgressTimerRunnable<Void> {

    private static final Logger logger = Logger.getLogger(ScanAllLocalRepositoryFoldersTimerRunnable.class.getName());

    private final AllLocalFolderProductsRepository allLocalFolderProductsRepository;

    public ScanAllLocalRepositoryFoldersTimerRunnable(ProgressBarHelper progressPanel, int threadId, AllLocalFolderProductsRepository allLocalFolderProductsRepository) {
        super(progressPanel, threadId, 500);

        this.allLocalFolderProductsRepository = allLocalFolderProductsRepository;
    }

    @Override
    protected boolean onTimerWakeUp(String message) {
        return super.onTimerWakeUp("Scan all local repository folders...");
    }

    @Override
    protected Void execute() throws Exception {
        List<LocalRepositoryFolder> localRepositoryFolders = this.allLocalFolderProductsRepository.loadRepositoryFolders();
        if (!isFinished()) {
            ScanLocalRepositoryFolderHelper scanLocalProductsHelper = new ScanLocalRepositoryFolderHelper(this.allLocalFolderProductsRepository);
            for (int i = 0; i < localRepositoryFolders.size(); i++) {
                if (isFinished()) {
                    break;
                } else {
                    LocalRepositoryFolder localRepositoryFolder = localRepositoryFolders.get(i);
                    try {
                        List<SaveProductData> savedProducts = scanLocalProductsHelper.scanValidProductsFromFolder(localRepositoryFolder, this);
                        if (savedProducts == null) {
                            updateLocalRepositoryFolderDeletedLater(localRepositoryFolder);
                        }
                    } catch (java.lang.InterruptedException exception) {
                        logger.log(Level.FINE, "Stop scanning the local repository folders.");
                        break;
                    } catch (Exception exception) {
                        logger.log(Level.SEVERE, "Failed to save the local product from the path '" + localRepositoryFolder.getPath().toString() + "'.", exception);
                    }
                }
            }
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
