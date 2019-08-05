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
package org.esa.snap.product.library.v2;

import org.esa.snap.productlibrary.rcp.toolviews.ProductLibraryToolView;
import org.esa.snap.productlibrary.rcp.toolviews.model.repositories.RepositoryInterface;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.BoxLayout;
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
@ActionID(category = "Window", id = "org.esa.snap.product.library.v2.ProductLibraryToolViewV2")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows"),
        @ActionReference(path = "Menu/File", position = 17)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ProductLibraryTopComponentV2Name",
        preferredID = "ProductLibraryTopComponentV2"
)
@NbBundle.Messages({
        "CTL_ProductLibraryTopComponentV2Name=Product Library V2",
        "CTL_ProductLibraryTopComponentV2Description=Product Library V2",
})
public class ProductLibraryToolViewV2 extends ToolTopComponent {

    private boolean initialized;
    private JComboBox<String> repositoryListComboBox;
    private JTextField productNameTextField;

    public ProductLibraryToolViewV2() {
        this.initialized = false;

        setLayout(new BorderLayout());
    }

    @Override
    protected void componentShowing() {
        if (!this.initialized) {
            this.initialized = true;

            Insets defaultTextFieldMargins = buildDefaultTextFieldMargins();
            Insets defaultListItemMargins = buildDefaultListItemMargins();

            this.productNameTextField = new JTextField();
            this.productNameTextField.setMargin(defaultTextFieldMargins);

            int textFieldPreferredHeight = this.productNameTextField.getPreferredSize().height;

            String[] availableDataSources = new String[] {"Scientific Hub", "Local data folder"};
            this.repositoryListComboBox = new JComboBox<String>(availableDataSources);
//            Dimension formatNameComboBoxSize = productFormatNameComboBox.getPreferredSize();
//            formatNameComboBoxSize.height = textFieldPreferredHeight;
//            productFormatNameComboBox.setPreferredSize(formatNameComboBoxSize);
//            productFormatNameComboBox.setMinimumSize(formatNameComboBoxSize);
//            LabelListCellRenderer<String> renderer = new LabelListCellRenderer<String>(defaultListItemMargins) {
//                @Override
//                protected String getItemDisplayText(String value) {
//                    return value;
//                }
//            };
            this.repositoryListComboBox.setMaximumRowCount(5);
//            this.repositoryListComboBox.setRenderer(renderer);
            this.repositoryListComboBox.setBackground(new Color(0, 0, 0, 0)); // set the transparent color
            this.repositoryListComboBox.setOpaque(true);

            Dimension buttonSize = new Dimension(textFieldPreferredHeight, textFieldPreferredHeight);

            JPanel verticalButtonsPanel = new JPanel();
            verticalButtonsPanel.setLayout(new BoxLayout(verticalButtonsPanel, BoxLayout.Y_AXIS));

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
            setBorder(new EmptyBorder(5, 5, 5, 5));
        }
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
