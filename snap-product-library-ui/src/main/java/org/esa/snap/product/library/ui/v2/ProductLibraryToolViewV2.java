/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.product.library.ui.v2;

import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;

@TopComponent.Description(
        preferredID = "ProductLibraryTopComponentV2",
        iconBase = "org/esa/snap/productlibrary/icons/search.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "rightSlidingSide",
        openAtStartup = true,
        position = 0
)
@ActionID(category = "Window", id = "org.esa.snap.product.library.ui.v2.ProductLibraryToolViewV2")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows"),
        @ActionReference(path = "Menu/File", position = 17)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ProductLibraryTopComponentV2Name",
        preferredID = "ProductLibraryTopComponentV2"
)
@NbBundle.Messages({
        "CTL_ProductLibraryTopComponentV2Name=Product Library v2",
        "CTL_ProductLibraryTopComponentV2Description=Product Library v2",
})
public class ProductLibraryToolViewV2 extends ToolTopComponent {

    private boolean initialized;
    private JComboBox<AbstractProductsDataSource> repositoryListComboBox;
    private JComboBox<String> supportedMissionsComboBox;
    private JTextField productNameTextField;
    private JPanel dataSourceParametersPanel;

    public ProductLibraryToolViewV2() {
        this.initialized = false;

        setDisplayName("Product Library v2");
        setLayout(new BorderLayout());
    }

    @Override
    protected void componentShowing() {
        if (!this.initialized) {
            this.initialized = true;

            this.dataSourceParametersPanel = new JPanel(new GridBagLayout());

            Insets defaultTextFieldMargins = buildDefaultTextFieldMargins();
            Insets defaultListItemMargins = buildDefaultListItemMargins();

            this.productNameTextField = new JTextField();
            this.productNameTextField.setMargin(defaultTextFieldMargins);

            int textFieldPreferredHeight = this.productNameTextField.getPreferredSize().height;

            AbstractProductsDataSource[] availableDataSources = new AbstractProductsDataSource[2];
            availableDataSources[0] = new SciHubProductsDataSource(textFieldPreferredHeight, defaultListItemMargins);
            availableDataSources[1] = new LocalProductsDataSource();
            this.repositoryListComboBox = new JComboBox<AbstractProductsDataSource>(availableDataSources);
            Dimension comboBoxSize = this.repositoryListComboBox.getPreferredSize();
            comboBoxSize.height = textFieldPreferredHeight;
            this.repositoryListComboBox.setPreferredSize(comboBoxSize);
            this.repositoryListComboBox.setMinimumSize(comboBoxSize);
            LabelListCellRenderer<AbstractProductsDataSource> renderer = new LabelListCellRenderer<AbstractProductsDataSource>(defaultListItemMargins) {
                @Override
                protected String getItemDisplayText(AbstractProductsDataSource value) {
                    return (value == null) ? "" : value.getName();
                }
            };
            this.repositoryListComboBox.setMaximumRowCount(5);
            this.repositoryListComboBox.setRenderer(renderer);
            this.repositoryListComboBox.setBackground(new Color(0, 0, 0, 0)); // set the transparent color
            this.repositoryListComboBox.setOpaque(true);
            this.repositoryListComboBox.setSelectedItem(null);
            this.repositoryListComboBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        System.out.println("itemStateChanged e="+e);
                        newDataSourceSelected((AbstractProductsDataSource)e.getItem());
                    }
                }
            });

            createSupportedMissionsComboBox(textFieldPreferredHeight, defaultListItemMargins);

            Dimension buttonSize = new Dimension(textFieldPreferredHeight, textFieldPreferredHeight);

            ActionListener searchButtonListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                }
            };

            JButton searchButton = buildButton("/org/esa/snap/productlibrary/icons/search24.png", searchButtonListener, buttonSize);
            JButton helpButton = buildButton("/org/esa/snap/resources/images/icons/Help24.gif", searchButtonListener, buttonSize);

            JPanel headerPanel = new JPanel(new GridBagLayout());

            int gapBetweenRows = 5;
            int gapBetweenColumns = 5;
            GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
            headerPanel.add(new JLabel("Data source"), c);

            c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
            headerPanel.add(this.repositoryListComboBox, c);

            c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
            headerPanel.add(searchButton, c);

            c = SwingUtils.buildConstraints(3, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
            headerPanel.add(helpButton, c);

            add(headerPanel, BorderLayout.NORTH);
            add(this.dataSourceParametersPanel, BorderLayout.CENTER);
            setBorder(new EmptyBorder(5, 5, 5, 5));
        }
    }

    private void createSupportedMissionsComboBox(int textFieldPreferredHeight, Insets defaultListItemMargins) {
        this.supportedMissionsComboBox = new JComboBox<String>();

        Dimension comboBoxSize = this.supportedMissionsComboBox.getPreferredSize();
        comboBoxSize.height = textFieldPreferredHeight;
        this.supportedMissionsComboBox.setPreferredSize(comboBoxSize);
        this.supportedMissionsComboBox.setMinimumSize(comboBoxSize);
        LabelListCellRenderer<String> renderer = new LabelListCellRenderer<String>(defaultListItemMargins) {
            @Override
            protected String getItemDisplayText(String value) {
                return (value == null) ? "" : value;
            }
        };
        this.supportedMissionsComboBox.setMaximumRowCount(5);
        this.supportedMissionsComboBox.setRenderer(renderer);
        this.supportedMissionsComboBox.setBackground(new Color(0, 0, 0, 0)); // set the transparent color
        this.supportedMissionsComboBox.setOpaque(true);
        this.supportedMissionsComboBox.setSelectedItem(null);
        this.supportedMissionsComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    System.out.println("itemStateChanged e="+e);
                    newDataSourceSelected((AbstractProductsDataSource)e.getItem());
                }
            }
        });

    }

    private void newDataSourceSelected(AbstractProductsDataSource selectedDataSource) {
        this.dataSourceParametersPanel.removeAll();
        JPanel parametersPanel = selectedDataSource.buildParametersPanel();
        if (parametersPanel != null) {
            GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, 0);
            this.dataSourceParametersPanel.add(parametersPanel, c);
        }
        this.dataSourceParametersPanel.revalidate();
        this.dataSourceParametersPanel.repaint();
    }

    private Insets buildDefaultTextFieldMargins() {
        return new Insets(3, 2, 3, 2);
    }

    private Insets buildDefaultListItemMargins() {
        return new Insets(3, 2, 3, 2);
    }

    private static JButton buildButton(String resourceImagePath, ActionListener buttonListener, Dimension buttonSize) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL imageURL = classLoader.getResource(resourceImagePath);
        ImageIcon icon = new ImageIcon(imageURL);
        JButton button = new JButton(icon);
        button.setFocusable(false);
        button.addActionListener(buttonListener);
        button.setPreferredSize(buttonSize);
        button.setMinimumSize(buttonSize);
        button.setMaximumSize(buttonSize);
        return button;
    }
}
