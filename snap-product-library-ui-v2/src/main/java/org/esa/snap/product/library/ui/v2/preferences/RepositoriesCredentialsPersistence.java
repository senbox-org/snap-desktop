package org.esa.snap.product.library.ui.v2.preferences;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.util.CryptoUtils;
import org.esa.snap.product.library.ui.v2.preferences.model.RemoteRepositoryCredentials;
import org.esa.snap.product.library.ui.v2.preferences.model.RepositoriesCredentialsConfigurations;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadRemoteProductsHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class RepositoriesCredentialsPersistence {

    /**
     * The pattern for remote repository.
     */
    private static final String REPO_ID_KEY = "%repo_id%";
    /**
     * The pattern for remote repository credential.
     */
    private static final String CRED_ID_KEY = "%cred_id%";
    /**
     * The regex for extract remote repositories IDs.
     */
    private static final String REPOSITORIES_IDS_REGEX = "repository_(.*?)\\.credential_.*?\\.username";
    /**
     * The preference key for remote repositories auto-uncompress setting.
     */
    private static final String PREFERENCE_KEY_AUTO_UNCOMPRESS = "auto_uncompress";
    /**
     * The preference key for remote repositories records on page setting.
     */
    private static final String PREFERENCE_KEY_RECORDS_ON_PAGE = "records_on_page";
    /**
     * The preference key for remote repository item.
     */
    private static final String PREFERENCE_KEY_REPOSITORY = "repository_" + REPO_ID_KEY;
    /**
     * The regex for extract remote repository credentials IDs.
     */
    private static final String REPOSITORY_CREDENTIALS_IDS_REGEX = PREFERENCE_KEY_REPOSITORY + "\\.credential_(.*?)\\.username";
    /**
     * The preference key for remote repository credential item.
     */
    private static final String PREFERENCE_KEY_REPOSITORY_CREDENTIAL = PREFERENCE_KEY_REPOSITORY + ".credential_" + CRED_ID_KEY;
    /**
     * The preference key for remote repository credential item username.
     */
    private static final String PREFERENCE_KEY_REPOSITORY_CREDENTIAL_USERNAME = PREFERENCE_KEY_REPOSITORY_CREDENTIAL + ".username";
    /**
     * The preference key for remote repository credential item password.
     */
    private static final String PREFERENCE_KEY_REPOSITORY_CREDENTIAL_SECRET = PREFERENCE_KEY_REPOSITORY_CREDENTIAL + ".password";

    private static String buildUsernameKey(String repositoryId, String credentialId) {
        String usernameKey = PREFERENCE_KEY_REPOSITORY_CREDENTIAL_USERNAME;
        usernameKey = usernameKey.replace(REPO_ID_KEY, repositoryId);
        usernameKey = usernameKey.replace(CRED_ID_KEY, credentialId);
        return usernameKey;
    }

    private static String buildPasswordKey(String repositoryId, String credentialId) {
        String passwordKey = PREFERENCE_KEY_REPOSITORY_CREDENTIAL_SECRET;
        passwordKey = passwordKey.replace(REPO_ID_KEY, repositoryId);
        passwordKey = passwordKey.replace(CRED_ID_KEY, credentialId);
        return passwordKey;
    }

    private static String buildCredentialsIdsRegex(String repositoryId) {
        String credentialsIdsRegex = REPOSITORY_CREDENTIALS_IDS_REGEX;
        credentialsIdsRegex = credentialsIdsRegex.replace(REPO_ID_KEY, repositoryId);
        return credentialsIdsRegex;
    }

    static boolean validCredentials(List<RemoteRepositoryCredentials> repositoriesCredentials) {
        for (RemoteRepositoryCredentials repositoryCredentials : repositoriesCredentials) {
            for (Credentials credentials : repositoryCredentials.getCredentialsList()) {
                if (repositoryCredentials.credentialInvalid(credentials)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void saveCredentials(Properties properties, List<RemoteRepositoryCredentials> repositoriesCredentials) {
        if (validCredentials(repositoriesCredentials)) {
            for (RemoteRepositoryCredentials repositoryCredentials : repositoriesCredentials) {
                String repositoryId = repositoryCredentials.getRepositoryName();
                int id = 1;
                for (Credentials credential : repositoryCredentials.getCredentialsList()) {
                    String credentialId = "" + id++;
                    String username = credential.getUserPrincipal().getName();
                    if (StringUtils.isNotNullAndNotEmpty(username)) {
                        String usernameKey = buildUsernameKey(repositoryId, credentialId);
                        properties.setProperty(usernameKey, username);
                    } else {
                        throw new IllegalArgumentException("empty username");
                    }
                    String password = credential.getPassword();
                    if (StringUtils.isNotNullAndNotEmpty(password)) {
                        String encryptedPassword;
                        try {
                            encryptedPassword = CryptoUtils.encrypt(password, repositoryId);
                        } catch (Exception e) {
                            throw new IllegalStateException("Failed to encrypt the password.", e);
                        }
                        String passwordKey = buildPasswordKey(repositoryId, credentialId);
                        properties.setProperty(passwordKey, encryptedPassword);
                    } else {
                        throw new IllegalArgumentException("empty password");
                    }
                }
            }

        } else {
            throw new IllegalArgumentException("invalid credentials (empty or duplicates)");
        }
    }

    private static void saveAutoUncompress(Properties properties, boolean autoUncompress) {
        String autoUncompressVal = "false";
        if (autoUncompress) {
            autoUncompressVal = "true";
        }
        properties.setProperty(PREFERENCE_KEY_AUTO_UNCOMPRESS, autoUncompressVal);
    }

    private static void saveRecordsOnPage(Properties properties, int recordsOnPageToSave) {
        String recordsOnPageVal = "" + recordsOnPageToSave;
        properties.setProperty(PREFERENCE_KEY_RECORDS_ON_PAGE, recordsOnPageVal);
    }

    static void save(Path destFile, RepositoriesCredentialsConfigurations repositoriesCredentialsConfigurations) throws IOException {
        if (destFile == null) {
            return;
        }
        Properties properties = new Properties();
        saveCredentials(properties, repositoriesCredentialsConfigurations.getRepositoriesCredentials());
        saveAutoUncompress(properties, repositoriesCredentialsConfigurations.isAutoUncompress());
        saveRecordsOnPage(properties, repositoriesCredentialsConfigurations.getNrRecordsOnPage());
        if (!Files.exists(destFile)) {
            Files.createDirectories(destFile.getParent());
            Files.createFile(destFile);
        }
        try (OutputStream outputStream = Files.newOutputStream(destFile)) {
            properties.store(outputStream, "");
        }
    }

    /**
     * Reads the Remote Repositories Credentials list from SNAP configuration file.
     */
    private static List<RemoteRepositoryCredentials> loadCredentials(Properties properties) {
        List<RemoteRepositoryCredentials> repositoriesCredentials = new ArrayList<>();
        Set<String> propertyNames = properties.stringPropertyNames();
        List<String> repositoriesIdsList = new ArrayList<>();
        for (String propertyName : propertyNames) {
            String repositoryId = propertyName.replaceAll(REPOSITORIES_IDS_REGEX, "$1");
            if (!repositoriesIdsList.contains(repositoryId)) {
                repositoriesIdsList.add(repositoryId);
            }
        }
        for (String repositoryId : repositoriesIdsList) {
            List<Credentials> repositoryCredentials = new ArrayList<>();
            List<String> repositoryCredentialsIds = new ArrayList<>();
            for (String propertyName : propertyNames) {
                String credentialId = propertyName.replaceAll(buildCredentialsIdsRegex(repositoryId), "$1");
                repositoryCredentialsIds.add(credentialId);
            }
            for (String credentialId : repositoryCredentialsIds) {
                String usernameKey = buildUsernameKey(repositoryId, credentialId);
                String username = properties.getProperty(usernameKey);
                String passwordKey = buildPasswordKey(repositoryId, credentialId);
                String password = properties.getProperty(passwordKey);
                try {
                    password = CryptoUtils.decrypt(password, repositoryId);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to decrypt the password.", e);
                }
                if (StringUtils.isNotNullAndNotEmpty(username) && StringUtils.isNotNullAndNotEmpty(password)) {
                    repositoryCredentials.add(new UsernamePasswordCredentials(username, password));
                }
            }
            if (!repositoryCredentials.isEmpty()) {
                repositoriesCredentials.add(new RemoteRepositoryCredentials(repositoryId, repositoryCredentials));
            }
        }
        return repositoriesCredentials;
    }

    /**
     * Reads the Remote Repositories Credentials auto-uncompress setting from SNAP configuration file.
     */
    private static boolean loadAutoUncompress(Properties properties) {
        String autoUncompressVal = properties.getProperty(PREFERENCE_KEY_AUTO_UNCOMPRESS);
        if (autoUncompressVal != null && !autoUncompressVal.isEmpty()) {
            return autoUncompressVal.contentEquals("true");
        }
        return DownloadRemoteProductsHelper.UNCOMPRESSED_DOWNLOADED_PRODUCTS;
    }

    /**
     * Reads the Remote Repositories Credentials nr repositories on page setting from SNAP configuration file.
     */
    private static int loadRecordsOnPage(Properties properties) {
        String recordsOnPageVal = properties.getProperty(PREFERENCE_KEY_RECORDS_ON_PAGE);
        if (recordsOnPageVal != null && !recordsOnPageVal.isEmpty()) {
            return Integer.parseInt(recordsOnPageVal);
        }
        return RepositoryOutputProductListPanel.VISIBLE_PRODUCTS_PER_PAGE;
    }

    /**
     * Reads the Remote Repositories Credentials configurations from SNAP configuration file.
     */
    static RepositoriesCredentialsConfigurations load(Path destFile) throws IOException {
        if (destFile == null || !Files.exists(destFile)) {
            return new RepositoriesCredentialsConfigurations(new ArrayList<>(), false, 20);
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(destFile)) {
            properties.load(inputStream);
        }
        List<RemoteRepositoryCredentials> repositoriesCredentials = loadCredentials(properties);
        boolean autoUncompress = loadAutoUncompress(properties);
        int recordsOnPage = loadRecordsOnPage(properties);
        return new RepositoriesCredentialsConfigurations(repositoriesCredentials, autoUncompress, recordsOnPage);
    }


    public static void main(String[] args) throws IOException {
        List<Credentials> repositoryCredentialsList = new ArrayList<>();
        repositoryCredentialsList.add(new UsernamePasswordCredentials("u1", "p1"));
        repositoryCredentialsList.add(new UsernamePasswordCredentials("u2", "p2"));
        RemoteRepositoryCredentials repositoryCredentials = new RemoteRepositoryCredentials("b1", repositoryCredentialsList);

        List<Credentials> repositoryCredentialsList2 = new ArrayList<>();
        repositoryCredentialsList2.add(new UsernamePasswordCredentials("as1", "pw1"));
        repositoryCredentialsList2.add(new UsernamePasswordCredentials("us2", "pw2"));
        RemoteRepositoryCredentials repositoryCredentials2 = new RemoteRepositoryCredentials("b2", repositoryCredentialsList2);
        List<RemoteRepositoryCredentials> itemsToSave = new ArrayList<>();
        itemsToSave.add(repositoryCredentials);
        itemsToSave.add(repositoryCredentials2);

        boolean autoUncompressToSave = false;
        int recordsOnPageToSave = 20;

        Path credsFile = Paths.get("D:/Temp/test_pl.properties");

        save(credsFile, new RepositoriesCredentialsConfigurations(itemsToSave, autoUncompressToSave, recordsOnPageToSave));

        RepositoriesCredentialsConfigurations repositoriesCredentialsConfigurations = load(credsFile);
        List<RemoteRepositoryCredentials> itemsLoaded = repositoriesCredentialsConfigurations.getRepositoriesCredentials();
        boolean autoUncompressLoaded = repositoriesCredentialsConfigurations.isAutoUncompress();
        int recordsOnPageLoaded = repositoriesCredentialsConfigurations.getNrRecordsOnPage();

        if (itemsToSave != itemsLoaded) {
            throw new IllegalStateException("items mismatch");
        }
        if (autoUncompressToSave != autoUncompressLoaded) {
            throw new IllegalStateException("auto uncompress mismatch");
        }
        if (recordsOnPageToSave != recordsOnPageLoaded) {
            throw new IllegalStateException("records on page mismatch");
        }
    }
}
