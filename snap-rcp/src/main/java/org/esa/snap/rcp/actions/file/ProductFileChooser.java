package org.esa.snap.rcp.actions.file;

import org.apache.commons.math3.util.Pair;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.MetadataInspector;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReaderExposedParams;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.product.ProductSubsetDialog;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Marco Peters
 *  modified 20191009 to support the advanced dialog for readers by Denisa Stefanescu
 */
public class ProductFileChooser extends SnapFileChooser {

    private static final Logger logger = Logger.getLogger(ProductFileChooser.class.getName());

    private static int numSubsetProducts = 0;

    private JButton subsetButton;
    private JButton advancedButton;
    private Product subsetProduct;
    private ProductSubsetDef productSubsetDef;
    private ProductReaderPlugIn plugin;

    private JLabel sizeLabel;
    private boolean useSubset;
    private Product productToExport;

    public ProductFileChooser(File currentDirectory) {
        super(currentDirectory);
        setDialogType(OPEN_DIALOG);
    }

    public void setProductToExport(Product product) {
        this.productToExport = product;
        if (productToExport != null) {
            String fileName = productToExport.getName();
            if (StringUtils.isNullOrEmpty(fileName) && productToExport.getFileLocation() != null) {
                fileName = productToExport.getFileLocation().getName();
            }
            setCurrentFilename(fileName);
        }
    }

    /**
     * File chooser only returns a product, if a product with advance options was created.
     *
     * @return the product with advance options or null
     */
    public Product getSubsetProduct() {
        return subsetProduct;
    }


    public boolean isSubsetEnabled() {
        return useSubset;
    }

    public void setSubsetEnabled(boolean useSubset) {
        this.useSubset = useSubset;
    }

    @Override
    public int showDialog(Component parent, String approveButtonText) {
        initUI();
        clearCurrentSubsetProduct();
        clearCurrentAdvancedProductOptions();
        updateState();
        return super.showDialog(parent, approveButtonText);
    }

    private void initUI() {
        if (getDialogType() == OPEN_DIALOG) {
            setDialogTitle(SnapApp.getDefault().getInstanceName() + " - Open Product");
            if (isSubsetEnabled()) {
                setDialogTitle(SnapApp.getDefault().getInstanceName() + " - Import Product");

                setApproveButtonText("Import Product");
                setApproveButtonMnemonic('I');
                setApproveButtonToolTipText("Imports the product.");
            }
        } else {
            setDialogTitle(SnapApp.getDefault().getInstanceName() + " - Save Product");
            if (isSubsetEnabled()) {
                setDialogTitle(SnapApp.getDefault().getInstanceName() + " - Export Product");

                setApproveButtonText("Export Product");
                setApproveButtonMnemonic('E');
                setApproveButtonToolTipText("Exports the product.");
            }
        }

        if (isSubsetEnabled()) {
            addSubsetAcessory();
        } else {
            addAdvancedAcessory();
        }
    }

    private void addSubsetAcessory() {
        subsetButton = new JButton("Subset...");
        subsetButton.setMnemonic('S');
        subsetButton.addActionListener(e -> openProductSubsetDialog());
        subsetButton.setEnabled(getSelectedFile() != null || productToExport != null);

        sizeLabel = new JLabel("0 M");
        sizeLabel.setHorizontalAlignment(JLabel.RIGHT);
        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints(
                "fill=HORIZONTAL,weightx=1,anchor=NORTHWEST,insets.left=7,insets.right=7,insets.bottom=4");
        GridBagUtils.addToPanel(panel, subsetButton, gbc, "gridy=0");
        GridBagUtils.addToPanel(panel, sizeLabel, gbc, "gridy=1");
        GridBagUtils.addVerticalFiller(panel, gbc);

        setAccessory(panel);

        addPropertyChangeListener(e -> updateState());
    }

    private void addAdvancedAcessory() {
        advancedButton = new JButton("Advanced");
        advancedButton.setMnemonic('A');
        advancedButton.addActionListener(e -> openAdvancedDialog());
        advancedButton.setEnabled(getSelectedFile() != null || productToExport != null);

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints(
                "fill=HORIZONTAL,weightx=1,anchor=NORTHWEST,insets.left=7,insets.right=7,insets.bottom=4");
        GridBagUtils.addToPanel(panel, advancedButton, gbc, "gridy=0");
        GridBagUtils.addVerticalFiller(panel, gbc);

        setAccessory(panel);
        addPropertyChangeListener(e -> updateState());
    }

    private void updateState() {
        if (isSubsetEnabled()) {
            subsetButton.setEnabled(getSelectedFile() != null || productToExport != null);

            File file = getSelectedFile();
            if (file != null && file.isFile()) {
                long fileSize = Math.round(file.length() / (1024.0 * 1024.0));
                if (fileSize >= 1) {
                    sizeLabel.setText("File size: " + fileSize + " M");
                } else {
                    sizeLabel.setText("File size: < 1 M");
                }
            } else {
                sizeLabel.setText("");
            }
        } else {
            advancedButton.setEnabled(getSelectedFile() != null || productToExport != null);
        }
    }

    private void clearCurrentSubsetProduct() {
        subsetProduct = null;
    }

    private void clearCurrentAdvancedProductOptions() {
        productSubsetDef = null;
        plugin = null;
    }

    public ProductSubsetDef getProductSubsetDef() {
        return productSubsetDef;
    }

    public ProductReaderPlugIn getProductReaderPlugin() {
        return plugin;
    }

    private void openProductSubsetDialog() {

        Product product = null;
        String newProductName = null;
        if (getDialogType() == OPEN_DIALOG) {
            File file = getSelectedFile();
            if (file == null) {
                // Should not come here...
                return;
            }
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                final FileFilter fileFilter = getFileFilter();
                String formatName = (fileFilter instanceof SnapFileFilter) ? ((SnapFileFilter) fileFilter).getFormatName() : null;
                product = ProductIO.readProduct(file, formatName);
                if (product == null) {
                    String msg = "The product could not be read.";
                    String optionalMsg = file.isDirectory() ? "\nSelection points to a directory." : "";
                    Dialogs.showError(msg + optionalMsg);
                    return;
                }
                newProductName = createNewProductName(product.getName(), numSubsetProducts++);
            } catch (IOException e) {
                Dialogs.showError("The product could not be read:\n" + e.getMessage());
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        } else {
            product = productToExport;
            if (StringUtils.isNotNullAndNotEmpty(getCurrentFilename())) {
                newProductName = getCurrentFilename();
            } else {
                newProductName = createNewProductName(product.getName(), numSubsetProducts++);
            }
        }

        if (product != null) {
            boolean approve = openProductSubsetDialog(product, newProductName);
            if (approve && getDialogType() == JFileChooser.OPEN_DIALOG) {
                approveSelection();
            }
        }

        updateState();
    }

    private boolean openProductSubsetDialog(Product product, String newProductName) {
        clearCurrentSubsetProduct();
        if (product != null) {
            if (product.isMultiSize()) {
                Dialogs.showError("No subset can be created of a multi-size products.");
                return false;
            }

            ProductSubsetDialog productSubsetDialog = new ProductSubsetDialog(SnapApp.getDefault().getMainFrame(), product);
            if (productSubsetDialog.show() == ProductSubsetDialog.ID_OK) {
                try {
                    subsetProduct = product.createSubset(productSubsetDialog.getProductSubsetDef(), newProductName, null);
                    if (getCurrentFilename() != null && !getCurrentFilename().startsWith("subset_")) {
                        setCurrentFilename("subset_" + getCurrentFilename());
                    }
                    return true;
                } catch (IOException e) {
                    Dialogs.showError("Could not create subset:\n" + e.getMessage());
                }
            }
        }
        return false;
    }

    private void openAdvancedDialog() {
        clearCurrentAdvancedProductOptions();
        File inputFile = getSelectedFile();
        boolean canceled = false;
        Pair<ProductReaderPlugIn, Boolean> foundPlugin = findPlugins(inputFile);
        if(foundPlugin != null){
            if(foundPlugin.getKey() == null) {
                canceled = foundPlugin.getValue();
            }else{
                plugin = foundPlugin.getKey();
            }

        }
        boolean addUIComponents = true;
        ProductReaderExposedParams readerExposedParams = null;
        MetadataInspector.Metadata readerInspectorExposeParameters = null;
        if (plugin != null) {
            readerExposedParams = plugin.getExposedParams();
            MetadataInspector metadatainsp = plugin.getMetadataInspector();
            if (metadatainsp != null) {
                Path input = convertInputToPath(inputFile);
                try {
                    readerInspectorExposeParameters = metadatainsp.getMetadata(input);
                } catch (Exception ex) {
                    addUIComponents = false;
                    logger.log(Level.SEVERE, "Failed to read the metadata file! ", ex);
                }
            }
        }else{
            addUIComponents = false;
        }
        //if the product does not support Advanced option action
        if (addUIComponents && readerExposedParams == null && readerInspectorExposeParameters == null) {
            int confirm = JOptionPane.showConfirmDialog(null, "The reader does not support Open with advanced options!\nDo you want to open the product normally?", null, JOptionPane.YES_NO_OPTION);
            //if the user want to open the product normally the Advanced Options window will not be displayed
            if (confirm == JOptionPane.YES_OPTION) {
                addUIComponents = false;
                approveSelection();
            } else {//if the user choose not to open the product normally the Advanced Option window components are removed
                addUIComponents = false;
            }
        }
        if (addUIComponents) {
            boolean approve = openAdvancedProduct(readerExposedParams, readerInspectorExposeParameters);
            if (approve && getDialogType() == JFileChooser.OPEN_DIALOG) {
                approveSelection();
            }
            updateState();
        }else if(plugin == null && !canceled){
            Dialogs.showError(Bundle.LBL_NoReaderFoundText() + String.format("%nFile '%s' can not be opened.", inputFile));
        }
    }

    private boolean openAdvancedProduct(ProductReaderExposedParams readerExposedParams, MetadataInspector.Metadata readerInspectorExposeParameters) {
        try {
            ProductAdvancedDialog advancedDialog = new ProductAdvancedDialog(SnapApp.getDefault().getMainFrame(), "Advanced Options", readerExposedParams,readerInspectorExposeParameters);
            if(advancedDialog.show() == advancedDialog.ID_OK) {
                advancedDialog.createSubsetDef();
                productSubsetDef = advancedDialog.getProductSubsetDef();
                return true;
                }
        }catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to open the advanced option dialog.", e);
            Dialogs.showError("The file " + getSelectedFile() + " could not be opened with advanced options!");
        }
        return false;
    }

    private String createNewProductName(String sourceProductName, int productIndex) {
        String newNameBase = "";
        if (sourceProductName != null && sourceProductName.length() > 0) {
            newNameBase = FileUtils.exchangeExtension(sourceProductName, "");
        }
        String newNamePrefix = "subset";
        String newProductName;
        if (newNameBase.length() > 0) {
            newProductName = newNamePrefix + "_" + productIndex + "_" + newNameBase;
        } else {
            newProductName = newNamePrefix + "_" + productIndex;
        }
        return newProductName;
    }

    public static Path convertInputToPath(Object input) {
        if (input == null) {
            throw new NullPointerException();
        } else if (input instanceof File) {
            return ((File) input).toPath();
        } else if (input instanceof Path) {
            return (Path) input;
        } else if (input instanceof String) {
            return Paths.get((String) input);
        } else {
            throw new IllegalArgumentException("Unknown input '" + input + "'.");
        }
    }

    private Pair<ProductReaderPlugIn, Boolean> findPlugins(File file){
        Pair<ProductReaderPlugIn, Boolean> result = null;
        final List<ProductOpener.PluginEntry> intendedPlugIns = ProductOpener.getPluginsForFile(file, DecodeQualification.INTENDED);
        List<ProductOpener.PluginEntry> suitablePlugIns = new ArrayList<>();
        if (intendedPlugIns.isEmpty()) { // check for suitable readers only if no intended reader was found
            suitablePlugIns.addAll(ProductOpener.getPluginsForFile(file, DecodeQualification.SUITABLE));
        }

        final String fileFormatName;
        if (intendedPlugIns.size() == 1) {
            ProductOpener.PluginEntry entry = intendedPlugIns.get(0);
            result = new Pair<>(entry.plugin, null);
        }else if (intendedPlugIns.isEmpty() && suitablePlugIns.size() == 1) {
            ProductOpener.PluginEntry entry = suitablePlugIns.get(0);
            result = new Pair<>(entry.plugin, null);
        }else if (!intendedPlugIns.isEmpty() || !suitablePlugIns.isEmpty()){
            Collections.sort(intendedPlugIns);
            Collections.sort(suitablePlugIns);
            // ask user to select a desired reader plugin
            fileFormatName = ProductOpener.getUserSelection(intendedPlugIns, suitablePlugIns);
            if (fileFormatName == null) { // User clicked cancel
                result = new Pair<>(null, true);
            } else {
                if (!suitablePlugIns.isEmpty() && suitablePlugIns.stream()
                        .anyMatch(entry -> entry.plugin.getFormatNames()[0].equals(fileFormatName))) {
                    ProductOpener.PluginEntry entry = suitablePlugIns.stream()
                            .filter(entry1 -> entry1.plugin.getFormatNames()[0].equals(fileFormatName))
                            .findAny()
                            .orElse(null);
                    result = new Pair<>(entry.plugin, false);
                } else {
                    ProductOpener.PluginEntry entry = intendedPlugIns.stream()
                            .filter(entry1 -> entry1.plugin.getFormatNames()[0].equals(fileFormatName))
                            .findAny()
                            .orElse(null);
                    result = new Pair<>(entry.plugin, false);
                }
            }
        }
        return result;
    }

}
