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
package org.esa.snap.rcp.placemark;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ProductChooser extends ModalDialog {

    private static final Font SMALL_PLAIN_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font SMALL_ITALIC_FONT = SMALL_PLAIN_FONT.deriveFont(Font.ITALIC);

    private final Product[] allProducts;
    private Product[] selectedProducts;

    private int numSelected;

    private JCheckBox[] checkBoxes;
    private JCheckBox selectAllCheckBox;
    private JCheckBox selectNoneCheckBox;
    private final boolean selectAtLeastOneProduct;
    private boolean multipleProducts;

    public ProductChooser(Window parent, String title, String helpID,
                          Product[] allProducts, Product[] selectedProducts) {
        super(parent, title, ModalDialog.ID_OK_CANCEL, helpID);
        Guardian.assertNotNull("allProducts", allProducts);
        this.allProducts = allProducts;
        this.selectedProducts = selectedProducts;
        selectAtLeastOneProduct = true;
        if (this.selectedProducts == null) {
            this.selectedProducts = new Product[0];
        }
        multipleProducts = allProducts.length > 1;
        initUI();
    }

    @Override
    public int show() {
        updateUI();
        return super.show();
    }

    private void initUI() {
        JPanel checkersPane = createCheckersPane();

        selectAllCheckBox = new JCheckBox("Select all"); /*I18N*/
        selectAllCheckBox.setMnemonic('a');
        selectAllCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                select(true);
            }
        });

        selectNoneCheckBox = new JCheckBox("Select none"); /*I18N*/
        selectNoneCheckBox.setMnemonic('n');
        selectNoneCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                select(false);
            }
        });

        final JPanel checkPane = new JPanel(new BorderLayout());
        checkPane.add(selectAllCheckBox, BorderLayout.WEST);
        checkPane.add(selectNoneCheckBox, BorderLayout.CENTER);
        JScrollPane scrollPane = new JScrollPane(checkersPane);
        final Dimension preferredSize = checkersPane.getPreferredSize();
        scrollPane.setPreferredSize(new Dimension(Math.min(preferredSize.width + 20, 400),
                                                  Math.min(preferredSize.height + 40, 300)));
        final JLabel label = new JLabel("Target product(s):"); /*I18N*/

        final JPanel content = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0;
        content.add(label, gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        gbc.weightx = 1;
        content.add(scrollPane, gbc);
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.weightx = 0;
        content.add(checkPane, gbc);
        gbc.gridy++;
        gbc.insets.top = 20;
        setContent(content);
    }

    private JPanel createCheckersPane() {
        checkBoxes = new JCheckBox[allProducts.length];
        final JPanel checkersPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("insets.left=4,anchor=WEST,fill=HORIZONTAL");
        final StringBuffer description = new StringBuffer();
        addProductCheckers(description, checkersPane, gbc);
        return checkersPane;
    }

    private void addProductCheckers(final StringBuffer description, final JPanel checkersPane,
                                    final GridBagConstraints gbc) {
        final ActionListener checkListener = createActionListener();
        for (int i = 0; i < allProducts.length; i++) {
            Product product = allProducts[i];
            boolean checked = false;
            for (Product selectedProduct : selectedProducts) {
                if (product == selectedProduct) {
                    checked = true;
                    numSelected++;
                    break;
                }
            }

            description.setLength(0);
            description.append(product.getDescription() == null ? "" : product.getDescription());

            final JCheckBox check = new JCheckBox(getDisplayName(product), checked);
            check.setFont(SMALL_PLAIN_FONT);
            check.addActionListener(checkListener);

            final JLabel label = new JLabel(description.toString());
            label.setFont(SMALL_ITALIC_FONT);

            gbc.gridy++;
            GridBagUtils.addToPanel(checkersPane, check, gbc, "weightx=0,gridx=0");
            GridBagUtils.addToPanel(checkersPane, label, gbc, "weightx=1,gridx=1");

            checkBoxes[i] = check;
        }
    }

    private String getDisplayName(Product rasterDataNode) {
        return multipleProducts ? rasterDataNode.getDisplayName() : rasterDataNode.getName();
    }

    private ActionListener createActionListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final JCheckBox check = (JCheckBox) e.getSource();
                if (check.isSelected()) {
                    numSelected++;
                } else {
                    numSelected--;
                }
                updateUI();
            }
        };
    }

    private void select(boolean b) {
        for (JCheckBox checkBox : checkBoxes) {
            if (b && !checkBox.isSelected()) {
                numSelected++;
            }
            if (!b && checkBox.isSelected()) {
                numSelected--;
            }
            checkBox.setSelected(b);
        }
        updateUI();
    }

    private void updateUI() {
        selectAllCheckBox.setSelected(numSelected == checkBoxes.length);
        selectAllCheckBox.setEnabled(numSelected < checkBoxes.length);
        selectAllCheckBox.updateUI();
        selectNoneCheckBox.setSelected(numSelected == 0);
        selectNoneCheckBox.setEnabled(numSelected > 0);
        selectNoneCheckBox.updateUI();
    }

    @Override
    protected boolean verifyUserInput() {
        final List<Product> products = new ArrayList<>();
        for (int i = 0; i < checkBoxes.length; i++) {
            JCheckBox checkBox = checkBoxes[i];
            if (checkBox.isSelected()) {
                products.add(allProducts[i]);
            }
        }
        selectedProducts = products.toArray(new Product[products.size()]);
        if (selectAtLeastOneProduct) {
            boolean result = selectedProducts.length > 0;
            if (!result) {
                showInformationDialog("No products selected.\n" +
                                      "Please select at least one product."); /*I18N*/
            }
            return result;
        }
        return true;
    }

    public Product[] getSelectedProducts() {
        return selectedProducts;
    }

}
