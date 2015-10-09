package org.esa.snap.rcp.ctxhelp;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.util.SystemUtils;

import java.awt.Desktop;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Adds contextual search support to Snap.
 * <p>
 * A contextual search is performed if a product node is selected and CTRL+F1 is pressed.
 *
 * @author Norman Fomferra
 */
public class ContextWebSearch {

    private static final String DEFAULT_SEARCH = "http://www.google.com/search?q=";
    private static final String DEFAULT_QUERY = "ESA Sentinel Toolbox";
    private static final String CONFIG_FILENAME = "context-search.properties";

    private static ContextWebSearch instance;
    private final Properties config;

    public static ContextWebSearch getDefault() {
        if (instance == null) {
            instance = new ContextWebSearch();
        }
        return instance;
    }

    public void searchForNode(ProductNode node) {

        String searchString = getSearch();
        String queryString = getQueryString(node);

        try {
            String search = searchString + URLEncoder.encode(queryString, "UTF-8");
            URI uri = URI.create(search);
            Desktop.getDesktop().browse(uri);
        } catch (IOException e) {
            SystemUtils.LOG.log(Level.WARNING, "Failed to perform context search");
        }
    }

    public ContextWebSearch() {

        this.config = new Properties();

        try {
            loadConfig();
        } catch (IOException e) {
            SystemUtils.LOG.log(Level.SEVERE, "Failed to load context search configuration", e);
        }
    }

    private String getSearch() {
        return config.getProperty("search", DEFAULT_SEARCH);
    }

    private String getQuery() {
        return config.getProperty("query", DEFAULT_QUERY);
    }

    private String getQuery(String productType, String def) {
        return config.getProperty(String.format("products.%s.query", productType.replace(" ", "_")), def);
    }

    private String getQueryString(ProductNode node) {
        String contextTerms = getQuery();

        if (node == null) {
            return contextTerms;
        }

        Product product = node.getProduct();
        if (product != null) {
            String productType = product.getProductType();
            if (productType != null) {
                contextTerms = getQuery(productType, contextTerms);
            }
        }

        String nodeName = node.getName();
        String[] nodeNameSplits = nodeName.split("[\\.\\_\\ \\-]");

        StringBuilder nodeNameTerms = new StringBuilder();
        for (String nodeNameSplit : nodeNameSplits) {
            if (!nodeNameSplit.isEmpty() && Character.isAlphabetic(nodeNameSplit.charAt(0))) {
                if (nodeNameTerms.length() > 0) {
                    nodeNameTerms.append(" OR ");
                }
                nodeNameTerms.append(nodeNameSplit);
            }
        }

        return contextTerms + " " + nodeNameTerms;
    }

    private void loadConfig() throws IOException {
        Path file = getConfigPath();
        config.load(new FileReader(file.toFile()));
    }

    private Path getConfigPath() throws IOException {
        FileSystem fs = FileSystems.getDefault();
        // todo: fix SystemUtils.getApplicationDataDir() (nf, 201502059)
        Path dir = SystemUtils.getAuxDataPath();
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }

        Path file = fs.getPath(dir.toString(), CONFIG_FILENAME);
        if (Files.notExists(file)) {
            InputStream resourceAsStream = getClass().getResourceAsStream(CONFIG_FILENAME);
            Files.copy(resourceAsStream, file);
        }
        return file;
    }

}
