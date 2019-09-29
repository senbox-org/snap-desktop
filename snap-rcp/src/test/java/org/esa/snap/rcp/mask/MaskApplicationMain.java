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

package org.esa.snap.rcp.mask;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.ui.GridLayout2;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.util.Locale;

public class MaskApplicationMain {
    private final Product product;
    private final MaskManagerForm maskManagerForm;
    private final MaskViewerForm maskViewerForm;

    private Product selectedProduct;
    private RasterDataNode selectedBand;


    public MaskApplicationMain() {
        product = MaskFormTest.createTestProduct();
        product.addProductNodeListener(new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(ProductNodeEvent event) {
                System.out.println("event = " + event);
            }

            @Override
            public void nodeDataChanged(ProductNodeEvent event) {
                System.out.println("event = " + event);
            }

            @Override
            public void nodeAdded(ProductNodeEvent event) {
                System.out.println("event = " + event);
            }

            @Override
            public void nodeRemoved(ProductNodeEvent event) {
                System.out.println("event = " + event);
            }
        });

        selectedProduct = null;
        selectedBand = null;

        maskManagerForm = new MaskManagerForm(null, null);
        maskManagerForm.reconfigureMaskTable(selectedProduct, selectedBand);

        maskViewerForm = new MaskViewerForm(null);
        maskViewerForm.reconfigureMaskTable(selectedProduct, selectedBand);
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.UK);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // ignore
        }

        final MaskApplicationMain app = new MaskApplicationMain();
        final JFrame maskManagerFrame = createFrame("ROI/Mask Manager", app.maskManagerForm.createContentPanel());
        final JFrame maskViewerFrame = createFrame("Bitmask Overlay", app.maskViewerForm.createContentPanel());
        final JFrame productManagerFrame = createFrame("Product Manager", createProductManagerPanel(app));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                maskManagerFrame.setLocation(50, 50);
                maskViewerFrame.setLocation(maskManagerFrame.getX() + maskManagerFrame.getWidth(), 50);
                productManagerFrame.setLocation(maskViewerFrame.getX() + maskViewerFrame.getWidth(), 50);

                maskManagerFrame.setVisible(true);
                maskViewerFrame.setVisible(true);
                productManagerFrame.setVisible(true);
            }
        });
    }

    private static JFrame createFrame(String name, JPanel panel) {
        final JFrame frame = new JFrame(name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.setAlwaysOnTop(true);
        frame.pack();
        return frame;
    }

    private static JPanel createProductManagerPanel(final MaskApplicationMain app) {
        JPanel panel = new JPanel(new GridLayout2(-1, 1, 2, 2));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));

        panel.add(new JButton(new AbstractAction("Select product") {
            @Override
            public void actionPerformed(ActionEvent e) {
                app.selectProduct(app.product);
            }
        }));

        panel.add(new JButton(new AbstractAction("Unselect product") {
            @Override
            public void actionPerformed(ActionEvent e) {
                app.selectProduct(null);
            }
        }));

        String[] bandNames = app.product.getBandNames();
        for (String bandName : bandNames) {
            AbstractAction action = new AbstractAction("Select band '" + bandName + "'") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    app.selectBand(app.product.getBand((String) getValue("bandName")));
                }
            };
            action.putValue("bandName", bandName);
            panel.add(new JButton(action));
        }

        panel.add(new JButton(new AbstractAction("Unselect band") {
            @Override
            public void actionPerformed(ActionEvent e) {
                app.selectBand(null);
            }
        }));

        return panel;
    }

    void selectProduct(Product product) {
        selectedProduct = product;
        selectedBand = null;
        handleSelectionStateChange();
    }

    void selectBand(RasterDataNode band) {
        if (band != null) {
            selectedProduct = band.getProduct();
        }
        selectedBand = band;
        handleSelectionStateChange();
    }

    private void handleSelectionStateChange() {
        maskManagerForm.reconfigureMaskTable(selectedProduct, selectedBand);
        maskViewerForm.reconfigureMaskTable(selectedProduct, selectedBand);
    }
}
