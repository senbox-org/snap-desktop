package org.esa.snap.product.library.ui.v2.repository.local;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.util.FileIOUtils;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelper;
import org.esa.snap.product.library.v2.database.H2DatabaseAccessor;
import org.esa.snap.product.library.v2.database.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.ProductLibraryDAL;
import org.esa.snap.product.library.v2.database.SaveProductData;
import org.esa.snap.ui.loading.GenericRunnable;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 4/10/2019.
 */
public class ScanAllLocalRepositoriesTimerRunnable extends AbstractProgressTimerRunnable<Void> {

    private static final Logger logger = Logger.getLogger(ScanAllLocalRepositoriesTimerRunnable.class.getName());

    public ScanAllLocalRepositoriesTimerRunnable(ProgressBarHelper progressPanel, int threadId) {
        super(progressPanel, threadId, 500);
    }

    @Override
    protected Void execute() throws Exception {
        updateProgressBarTextLater("");

        List<LocalRepositoryFolder> localRepositoryFolders;
        try (Connection connection = H2DatabaseAccessor.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                localRepositoryFolders = ProductLibraryDAL.loadLocalRepositoryFolders(statement);
            }
        }

        for (int i=0; i<localRepositoryFolders.size(); i++) {
            LocalRepositoryFolder localRepositoryFolder = localRepositoryFolders.get(i);
            try {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Scan the local repository folder '" + localRepositoryFolder.getPath().toString()+"'.");
                }

                SaveLocalProductsHelper saveLocalProductsHelper = new SaveLocalProductsHelper() {
                    @Override
                    protected void missingProductGeoCoding(Path path, Product product) throws IOException {
                        super.missingProductGeoCoding(path, product);

                        if (logger.isLoggable(Level.FINE)) {
                            if (Files.isDirectory(path)) {
                                logger.log(Level.FINE, "Delete the folder '"+path.toString()+"' because the product does not contain the geo-coding.");
                            } else {
                                logger.log(Level.FINE, "Delete the file '"+path.toString()+"' because the product does not contain the geo-coding.");
                            }
                        }
                        FileIOUtils.deleteFolder(path);
                    }

                    @Override
                    protected void invalidProduct(Path path) throws IOException {
                        super.invalidProduct(path);

                        if (logger.isLoggable(Level.FINE)) {
                            if (Files.isDirectory(path)) {
                                logger.log(Level.FINE, "Delete the folder '"+path.toString()+"' because it does not represent a valid product.");
                            } else {
                                logger.log(Level.FINE, "Delete the file '"+path.toString()+"' because it does not represent a valid product.");
                            }
                        }
                        FileIOUtils.deleteFolder(path);
                    }
                };

                List<SaveProductData> savedProducts = saveLocalProductsHelper.saveProductsFromFolder(localRepositoryFolder.getPath());

                if (savedProducts == null) {
                    ProductLibraryDAL.deleteLocalRepositoryFolder(localRepositoryFolder);
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Deleted the local repository folder folder '" + localRepositoryFolder.getPath().toString()+"' from the database.");
                    }

                    updateLocalRepositoryFolderDeletedLater(localRepositoryFolder);
                } else {
                    Set<Integer> deletedProductIds = ProductLibraryDAL.deleteMissingLocalRepositoryProducts(localRepositoryFolder.getId(), savedProducts);

                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Deleted "+deletedProductIds.size()+" products from the database corresponding to the local repository folder '" + localRepositoryFolder.getPath().toString()+"'.");
                    }
                }
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Failed to save the local product from the path '" + localRepositoryFolder.getPath().toString() + "'.", exception);
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
