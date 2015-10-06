package org.esa.snap.rcp.actions.file;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.product.ProductSubsetDialog;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.io.File;
import java.io.IOException;

/**
 * @author Marco Peters
 */
class ProductFileChooser extends SnapFileChooser {

    private static int numSubsetProducts = 0;

    private JButton subsetButton;
    private Product subsetProduct;

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
            String fileName;
            if (productToExport.getFileLocation() != null) {
                fileName = productToExport.getFileLocation().getName();
            } else {
                fileName = productToExport.getName();
            }
            setCurrentFilename(fileName);
        }
    }

    /**
     * File chooser only returns a product, if a product subset was created.
     *
     * @return the product subset or null
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
        }
    }

    private void clearCurrentSubsetProduct() {
        subsetProduct = null;
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
                product = ProductIO.readProduct(file);
                newProductName = createNewProductName(product.getName(), numSubsetProducts++);
            } catch (IOException e) {
                SnapDialogs.showError("The product could not be read:\n" + e.getMessage());
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
            ProductSubsetDialog productSubsetDialog = new ProductSubsetDialog(SnapApp.getDefault().getMainFrame(), product);
            if (productSubsetDialog.show() == ProductSubsetDialog.ID_OK) {
                try {
                    subsetProduct = product.createSubset(productSubsetDialog.getProductSubsetDef(), newProductName, null);
                    if (getCurrentFilename() != null && !getCurrentFilename().startsWith("subset_")) {
                        setCurrentFilename("subset_" + getCurrentFilename());
                    }
                    return true;
                } catch (IOException e) {
                    SnapDialogs.showError("Could not create subset:\n" + e.getMessage());
                }
            }
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

}
