package org.esa.snap.product.library.ui.v2.preferences;

import org.esa.snap.product.library.ui.v2.preferences.model.RemoteRepositoryCredentials;
import org.esa.snap.runtime.EngineConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A controller for Product Library Remote Repositories Credentials.
 * Used for establish a strategy with storing remote repositories credentials data.
 *
 * @author Adrian Draghici
 */
public class RepositoriesCredentialsController {

    private static RepositoriesCredentialsController instance = new RepositoriesCredentialsController(getDefaultConfigFilePath());

    private static Logger logger = Logger.getLogger(RepositoriesCredentialsController.class.getName());

    private final Path plConfigFile;
    private List<RemoteRepositoryCredentials> repositoriesCredentials;


    /**
     * Creates the new VFS Remote File Repositories Controller with given config file.
     */
    private RepositoriesCredentialsController(Path plConfigFile) {
        this.plConfigFile = plConfigFile;
        this.repositoriesCredentials = new ArrayList<>();
        try {
            this.repositoriesCredentials = RepositoriesCredentialsPersistence.load(this.plConfigFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read the credentials from the application preferences.", e);
        }
    }

    private static Path getDefaultConfigFilePath() {
        return Paths.get(EngineConfig.instance().userDir().toString() + "/config/Preferences/product-library.properties");
    }

    public static RepositoriesCredentialsController getInstance() {
        return instance;
    }

    public List<RemoteRepositoryCredentials> getRepositoriesCredentials() {
        return this.repositoriesCredentials;
    }

    /**
     * Writes the provided Remote Repositories Credentials on SNAP configuration file.
     */
    void saveCredentials(List<RemoteRepositoryCredentials> repositoriesCredentialsForSave) throws IOException {
        this.repositoriesCredentials = repositoriesCredentialsForSave;
        RepositoriesCredentialsPersistence.save(this.plConfigFile, this.repositoriesCredentials);
    }
}
