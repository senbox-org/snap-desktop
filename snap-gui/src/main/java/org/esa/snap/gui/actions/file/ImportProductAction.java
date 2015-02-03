/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.gui.actions.file;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeList;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.NewProductDialog;
import org.esa.beam.framework.ui.product.ProductSubsetDialog;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.SnapDialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Map;

/**
 * Action for importing a product.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 */
@ActionID(
        category = "File",
        id = "org.esa.snap.gui.actions.file.ImportProductAction"
)
@ActionRegistration(
        displayName = "#CTL_ImportProductActionName",
        menuText = "#CTL_ImportProductActionMenuText"
)
@ActionReference(path = "Menu/File", position = 100, separatorBefore = 99)
@NbBundle.Messages({
        "CTL_ImportProductActionName=Import Product",
        "CTL_ImportProductActionMenuText=Import Product..."
})
public class ImportProductAction extends AbstractAction {

    /**
     * Action factory method used in NetBeans {@code layer.xml} file, e.g.
     *
     * <pre>
     * &lt;file name="org-esa-beam-dataio-ceos-ImportAvnir2Product.instance"&gt;
     *     &lt;attr name="instanceCreate"
     *         methodvalue="org.openide.awt.Actions.alwaysEnabled"/&gt;
     *     &lt;attr name="delegate"
     *         methodvalue="org.esa.snap.gui.actions.file.ImportProductAction.create"/&gt;
     *     &lt;attr name="displayName"
     *         stringvalue="ALOS/AVNIR-2 Product"/&gt;
     *     &lt;attr name="formatName"
     *         stringvalue="AVNIR-2"/&gt;
     *     &lt;attr name="useAllFileFilter"
     *         boolvalue="true"/&gt;
     *     &lt;attr name="helpId"
     *         stringvalue="importAvnir2Product"/&gt;
     *     &lt;attr name="ShortDescription"
     *         stringvalue="Import an ALOS/AVNIR-2 data product."/&gt;
     * &lt;/file&gt;
     * </pre>
     *
     * @param configuration Configuration attributes from layer.xml.
     * @return The action.
     * @since SNAP 2
     */
    public static ImportProductAction create(Map<String, Object> configuration) {
        ImportProductAction importProductAction = new ImportProductAction();
        importProductAction.setFormatName((String) configuration.get("formatName"));
        importProductAction.setHelpId((String) configuration.get("helpId"));
        importProductAction.setUseAllFileFilter((Boolean) configuration.get("useAllFileFilter"));
        return importProductAction;
    }

    public void setFormatName(String formatName) {
        putValue("formatName", formatName);
    }

    public void setHelpId(String helpId) {
        putValue("helpId", helpId);
    }

    public void setUseAllFileFilter(Boolean useAllFileFilter) {
        putValue("useAllFileFilter", useAllFileFilter);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OpenProductAction openProductAction = new OpenProductAction();
        ProductFileChooser fileChooser = new ProductFileChooser();
        //openProductAction.setFileChooser(fileChooser);
        openProductAction.actionPerformed(e);
    }

    protected class ProductFileChooser extends BeamFileChooser {

        private static final long serialVersionUID = -8122437634943074658L;

        private JButton subsetButton;
        private Product subsetProduct;

        private JLabel sizeLabel;

        public ProductFileChooser() {
            createUI();
        }

        /**
         * File chooser only returns a product, if a product subset was created.
         *
         * @return the product subset or null
         */
        public Product getSubsetProduct() {
            return subsetProduct;
        }

        @Override
        public int showDialog(Component parent, String approveButtonText) {
            clearCurrentProduct();
            return super.showDialog(parent, approveButtonText);
        }

        protected void createUI() {

            setDialogType(OPEN_DIALOG);
            setDialogTitle(SnapApp.getDefault().getInstanceName() + " - " + Bundle.CTL_ImportProductActionName()); /*I18N*/

            subsetButton = new JButton("Subset...");  /*I18N*/
            subsetButton.setMnemonic('S'); /*I18N*/
            subsetButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {

                    openProductSubsetDialog();
                }
            });
            subsetButton.setEnabled(false);


            sizeLabel = new JLabel("0 M");
            sizeLabel.setHorizontalAlignment(JLabel.RIGHT);
            JPanel panel = GridBagUtils.createPanel();
            GridBagConstraints gbc = GridBagUtils.createConstraints(
                    "fill=HORIZONTAL,weightx=1,anchor=NORTHWEST,insets.left=7,insets.right=7,insets.bottom=4");
            GridBagUtils.addToPanel(panel, subsetButton, gbc, "gridy=0");
            //GridBagUtils.addToPanel(panel, _historyButton, gbc, "gridy=1");
            GridBagUtils.addToPanel(panel, sizeLabel, gbc, "gridy=1");
            GridBagUtils.addVerticalFiller(panel, gbc);

            setAccessory(panel);

            addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent e) {
                    String prop = e.getPropertyName();
                    if (prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                        clearCurrentProduct();
                        subsetButton.setEnabled(true);
                    } else if (prop.equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
                        clearCurrentProduct();
                        subsetButton.setEnabled(false);
                    }
                    updateState();
                }
            });

            ProductFileChooser.this.setPreferredSize(new Dimension(640, 400));
            clearCurrentProduct();
            updateState();
        }

        private void updateState() {
            setApproveButtonText(Bundle.CTL_ImportProductActionName());/*I18N*/
            setApproveButtonMnemonic('I');/*I18N*/
            setApproveButtonToolTipText("Imports the entire product.");/*I18N*/
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

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            // todo
            //Product product = readProductNodes(file);
            setCursor(Cursor.getDefaultCursor());
            // todo
            /*
            boolean approve = openProductSubsetDialog(product);
            if (approve) {
                approveSelection();
            }
            */
            updateState();
        }

        private boolean openProductSubsetDialog(Product product) {
            subsetProduct = null;
            boolean approve = false;
            if (product != null) {
                Frame mainFrame = SnapApp.getDefault().getMainFrame();
                ProductSubsetDialog productSubsetDialog = new ProductSubsetDialog(mainFrame, product);
                if (productSubsetDialog.show() == ProductSubsetDialog.ID_OK) {
                    ProductNodeList<Product> products = new ProductNodeList<Product>();
                    products.add(product);
                    NewProductDialog newProductDialog = new NewProductDialog(SnapApp.getDefault().getMainFrame(), products, 0,
                                                                             true);
                    newProductDialog.setSubsetDef(productSubsetDialog.getProductSubsetDef());
                    if (newProductDialog.show() == NewProductDialog.ID_OK) {
                        subsetProduct = newProductDialog.getResultProduct();
                        approve = subsetProduct != null;
                        if (!approve && newProductDialog.getException() != null) {
                            SnapDialogs.showError("The product subset could not be created:\n" +
                                                          newProductDialog.getException().getMessage());
                        }
                    }
                }
            }
            return approve;
        }
    }


}
