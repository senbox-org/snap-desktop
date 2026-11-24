/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.worldwind;

import gov.nasa.worldwind.WorldWindow;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.worldwind.layers.DefaultProductLayer;
import org.esa.snap.worldwind.layers.ProductImageLayer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;


class ProductPanel extends JPanel {
    private final DefaultProductLayer defaultProductLayer;
    private final ProductImageLayer imageLayer;
    private JPanel layersPanel;
    private JPanel westPanel;
    private JScrollPane scrollPane;
    private Font defaultFont = null;

    /**
     * @param imageLayer the full-resolution images, or null. Products drawn by a layer of their own (e.g. S-1 OCN)
     *                   have no outline in the product layer but may have an image; they are listed too, and their
     *                   check box shows or hides that image.
     */
    public ProductPanel(WorldWindow wwd, DefaultProductLayer prodLayer, ProductImageLayer imageLayer) {
        super(new BorderLayout());
        defaultProductLayer = prodLayer;
        this.imageLayer = imageLayer;
        this.makePanel(wwd, new Dimension(100, 400));
    }

    private void makePanel(WorldWindow wwd, Dimension size) {
        // Make and fill the panel holding the layer titles.
        this.layersPanel = new JPanel(new GridLayout(0, 1, 0, 4));
        this.layersPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.fill(wwd);

        // Must put the layer grid in a container to prevent scroll panel from stretching their vertical spacing.
        final JPanel dummyPanel = new JPanel(new BorderLayout());

        dummyPanel.add(this.layersPanel, BorderLayout.NORTH);

        // Put the name panel in a scroll bar.
        this.scrollPane = new JScrollPane(dummyPanel);
        this.scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        if (size != null)
            this.scrollPane.setPreferredSize(size);

        // Add the scroll bar and name panel to a titled panel that will resize with the main window.

        westPanel = new JPanel(new GridLayout(0, 1, 0, 10));
        westPanel.setBorder(
                new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder("Products")));
        westPanel.setToolTipText("Products to Show");
        westPanel.add(scrollPane);
        this.add(westPanel, BorderLayout.CENTER);
    }

    private void fill(WorldWindow wwd) {
        for (String name : defaultProductLayer.getProductNames()) {
            addCheckBox(wwd, name, defaultProductLayer.isProductVisible(name),
                        visible -> defaultProductLayer.setProductVisible(name, visible));
        }
        if (imageLayer != null) {
            final List<Product> imageOnly = new ArrayList<>();
            for (Product product : imageLayer.getProducts()) {
                if (!defaultProductLayer.hasProduct(product)) {
                    imageOnly.add(product);
                }
            }
            imageOnly.sort(Comparator.comparing(ProductPanel::displayName));
            for (Product product : imageOnly) {
                addCheckBox(wwd, displayName(product), imageLayer.isProductVisible(product),
                            visible -> imageLayer.setProductVisible(product, visible));
            }
        }
    }

    // as the product layer names its products
    private static String displayName(final Product product) {
        return product.getProductRefString() + product.getName();
    }

    private void addCheckBox(final WorldWindow wwd, final String name, final boolean selected,
                             final Consumer<Boolean> setVisible) {
        final JCheckBox jcb = new JCheckBox(new LayerAction(wwd, name, setVisible));
        jcb.setSelected(selected);
        this.layersPanel.add(jcb);

        if (defaultFont == null) {
            this.defaultFont = jcb.getFont();
        }
    }

    /**
     * The check boxes, in order. For tests.
     */
    List<JCheckBox> getCheckBoxes() {
        final List<JCheckBox> list = new ArrayList<>();
        for (java.awt.Component component : layersPanel.getComponents()) {
            if (component instanceof JCheckBox) {
                list.add((JCheckBox) component);
            }
        }
        return list;
    }

    public void update(WorldWindow wwd) {
        // Replace all the layer names in the layers panel with the names of the current layers.
        this.layersPanel.removeAll();
        this.fill(wwd);
        this.westPanel.revalidate();
        this.westPanel.repaint();
    }

    @Override
    public void setToolTipText(String string) {
        this.scrollPane.setToolTipText(string);
    }

    private static class LayerAction extends AbstractAction {
        final WorldWindow wwd;
        private final Consumer<Boolean> setVisible;

        public LayerAction(WorldWindow wwd, String name, Consumer<Boolean> setVisible) {
            super(name);
            this.wwd = wwd;
            this.setVisible = setVisible;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            SystemUtils.LOG.fine("actionPerformed " + actionEvent);
            // show or hide this product only, not the whole layer
            setVisible.accept(((JCheckBox) actionEvent.getSource()).isSelected());
            if (wwd != null) {
                wwd.redraw();
            }
        }
    }
}
