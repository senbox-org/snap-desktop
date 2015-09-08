package org.esa.snap.rcp.actions.file;

import org.esa.snap.framework.dataio.ProductIO;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.ui.GridBagUtils;
import org.esa.snap.framework.ui.SnapFileChooser;
import org.esa.snap.framework.ui.product.ProductSubsetDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.util.io.FileUtils;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Frame;
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

    public ProductFileChooser(File currentDirectory) {
        super(currentDirectory);
        setDialogType(OPEN_DIALOG);
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
        if (isSubsetEnabled()) {
            addSubsetAcessory();
            validate();
        }
        clearCurrentProduct();
        return super.showDialog(parent, approveButtonText);
    }

    private void addSubsetAcessory() {
        subsetButton = new JButton("Subset...");
        subsetButton.setMnemonic('S');
        subsetButton.addActionListener(e -> openProductSubsetDialog());
        subsetButton.setEnabled(false);


        sizeLabel = new JLabel("0 M");
        sizeLabel.setHorizontalAlignment(JLabel.RIGHT);
        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints(
                "fill=HORIZONTAL,weightx=1,anchor=NORTHWEST,insets.left=7,insets.right=7,insets.bottom=4");
        GridBagUtils.addToPanel(panel, subsetButton, gbc, "gridy=0");
        GridBagUtils.addToPanel(panel, sizeLabel, gbc, "gridy=1");
        GridBagUtils.addVerticalFiller(panel, gbc);

        setAccessory(panel);

        addPropertyChangeListener(e -> {
            String prop = e.getPropertyName();
            if (prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                clearCurrentProduct();
                subsetButton.setEnabled(true);
            } else if (prop.equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
                clearCurrentProduct();
                subsetButton.setEnabled(false);
            }
            updateState();
        });
    }

    private void updateState() {
        setApproveButtonText(Bundle.CTL_ImportProductActionName());
        setApproveButtonMnemonic('I');
        setApproveButtonToolTipText("Imports the entire product.");
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

    private void clearCurrentProduct() {
        subsetProduct = null;
    }

    private void openProductSubsetDialog() {

        File file = getSelectedFile();
        if (file == null) {
            // Should not come here...
            return;
        }

        Product product = null;
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            product = ProductIO.readProduct(file);
        } catch (IOException e) {
            SnapDialogs.showError("The product could not be read:\n" + e.getMessage());
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }

        if (product != null) {
            boolean approve = openProductSubsetDialog(product);
            if (approve) {
                approveSelection();
            }
        }

        updateState();
    }

    private boolean openProductSubsetDialog(Product product) {
        subsetProduct = null;
        boolean approve = false;
        if (product != null) {
            Frame mainFrame = SnapApp.getDefault().getMainFrame();
            ProductSubsetDialog productSubsetDialog = new ProductSubsetDialog(mainFrame, product);
            if (productSubsetDialog.show() == ProductSubsetDialog.ID_OK) {
                try {
                    final String newProductName = createNewProductName(product.getName(), numSubsetProducts++);
                    subsetProduct = product.createSubset(productSubsetDialog.getProductSubsetDef(),
                                                         newProductName,
                                                         null);
                    approve = true;
                } catch (IOException e) {
                    SnapDialogs.showError("Could not create subset:\n" + e.getMessage());
                }
            }
        }
        return approve;
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
