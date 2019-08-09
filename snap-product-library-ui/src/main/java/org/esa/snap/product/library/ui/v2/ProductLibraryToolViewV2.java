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

import org.esa.snap.product.library.ui.v2.table.AbstractTableColumn;
import org.esa.snap.product.library.ui.v2.table.CustomTable;
import org.esa.snap.product.library.ui.v2.table.CustomTableModel;
import org.esa.snap.product.library.ui.v2.table.LabelTableCellRenderer;
import org.esa.snap.product.library.v2.SciHubDownloader;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.loading.IComponentsEnabled;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.LoadingIndicatorPanel;
import org.esa.snap.ui.loading.SwingUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.eodata.EOProduct;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private JComboBox<AbstractProductsDataSource> dataSourcesComboBox;
    private CustomTable<EOProduct> productsTable;
    private LoadingIndicatorPanel loadingIndicatorPanel;
    private JSplitPane verticalSplitPane;

    public ProductLibraryToolViewV2() {
        this.initialized = false;

        setDisplayName(Bundle.CTL_ProductLibraryTopComponentV2Name());
    }

    @Override
    protected void componentShowing() {
        if (!this.initialized) {
            this.initialized = true;
            initialize();
        }
    }

    private JPanel buildDataSourceHeaderPanel(Insets defaultListItemMargins, int textFieldPreferredHeight) {
        AbstractProductsDataSource[] availableDataSources = new AbstractProductsDataSource[2];
        availableDataSources[0] = new SciHubProductsDataSource(textFieldPreferredHeight, defaultListItemMargins) {
            @Override
            protected void newMissionSelected(String selectedMission) {
                super.newMissionSelected(selectedMission);

                productsTable.getModel().clearRecordsAndFireEvent();
            }
        };
        availableDataSources[1] = new LocalProductsDataSource();
        this.dataSourcesComboBox = new JComboBox<AbstractProductsDataSource>(availableDataSources);
        Dimension comboBoxSize = this.dataSourcesComboBox.getPreferredSize();
        comboBoxSize.height = textFieldPreferredHeight;
        this.dataSourcesComboBox.setPreferredSize(comboBoxSize);
        LabelListCellRenderer<AbstractProductsDataSource> renderer = new LabelListCellRenderer<AbstractProductsDataSource>(defaultListItemMargins) {
            @Override
            protected String getItemDisplayText(AbstractProductsDataSource value) {
                return (value == null) ? "" : value.getName();
            }
        };
        this.dataSourcesComboBox.setMaximumRowCount(5);
        this.dataSourcesComboBox.setRenderer(renderer);
        this.dataSourcesComboBox.setBackground(new Color(0, 0, 0, 0)); // set the transparent color
        this.dataSourcesComboBox.setOpaque(true);
        this.dataSourcesComboBox.setSelectedIndex(0);
        this.dataSourcesComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    newDataSourceSelected((AbstractProductsDataSource) e.getItem());
                }
            }
        });

        Dimension buttonSize = new Dimension(textFieldPreferredHeight, textFieldPreferredHeight);

        ActionListener searchButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchButtonPressed();
            }
        };

        JButton searchButton = buildButton("/org/esa/snap/productlibrary/icons/search24.png", searchButtonListener, buttonSize);
        JButton helpButton = buildButton("/org/esa/snap/resources/images/icons/Help24.gif", searchButtonListener, buttonSize);

        JPanel headerPanel = new JPanel(new GridBagLayout());

        int gapBetweenColumns = getGapBetweenColumns();

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        headerPanel.add(new JLabel("Data source"), c);

        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        headerPanel.add(this.dataSourcesComboBox, c);

        c = SwingUtils.buildConstraints(2, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        headerPanel.add(searchButton, c);

        c = SwingUtils.buildConstraints(3, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        headerPanel.add(helpButton, c);

        return headerPanel;
    }

    private int getGapBetweenRows() {
        return 5;
    }

    private int getGapBetweenColumns() {
        return 5;
    }

    private void initialize() {
        int gapBetweenRows = getGapBetweenRows();
        int gapBetweenColumns = getGapBetweenRows();

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(gapBetweenRows, gapBetweenColumns, gapBetweenRows, gapBetweenColumns));
        CustomLayeredPane layeredPane = new CustomLayeredPane(new BorderLayout(0, gapBetweenRows));
        add(layeredPane, BorderLayout.CENTER);

        Insets defaultTextFieldMargins = buildDefaultTextFieldMargins();
        Insets defaultListItemMargins = buildDefaultListItemMargins();
        JTextField productNameTextField = new JTextField();
        productNameTextField.setMargin(defaultTextFieldMargins);
        int textFieldPreferredHeight = productNameTextField.getPreferredSize().height;

        JPanel headerPanel = buildDataSourceHeaderPanel(defaultListItemMargins, textFieldPreferredHeight);
        createTableProducts();

        AbstractProductsDataSource selectedDataSource = (AbstractProductsDataSource)this.dataSourcesComboBox.getSelectedItem();

        JScrollPane tableScrollPane = new JScrollPane(this.productsTable);
        Border outsideBorder = new EmptyBorder(5, 0, 0, 0);
        Border border = new CompoundBorder(outsideBorder, tableScrollPane.getBorder());
        tableScrollPane.setBorder(border);

        verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT) {
            @Override
            public void doLayout() {
                super.doLayout();

                setDividerLocation(0.5d);
            }
        };
        verticalSplitPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        verticalSplitPane.setTopComponent((selectedDataSource == null) ? new JLabel() : selectedDataSource);
        verticalSplitPane.setBottomComponent(tableScrollPane);
        verticalSplitPane.setOneTouchExpandable(true);

        IComponentsEnabled componentsEnabled = new IComponentsEnabled() {
            @Override
            public void setComponentsEnabled(boolean enabled) {
            }
        };
        this.loadingIndicatorPanel = new LoadingIndicatorPanel(componentsEnabled);

        layeredPane.addToContentPanel(headerPanel, BorderLayout.NORTH);
        layeredPane.addToContentPanel(verticalSplitPane, BorderLayout.CENTER);
        layeredPane.addPanelToModalLayerAndPositionInCenter(this.loadingIndicatorPanel);
    }

    private void createTableProducts() {
        AbstractTableColumn<EOProduct> numberColumn = new AbstractTableColumn<EOProduct>("Number", Number.class) {
            @Override
            public Object getCellValue(EOProduct record, int rowIndex, int columnIndex) {
                return Integer.toString(rowIndex + 1);
            }
        };
        AbstractTableColumn<EOProduct> productNameColumn = new AbstractTableColumn<EOProduct>("Product name", EOProduct.class) {
            @Override
            public Object getCellValue(EOProduct record, int rowIndex, int columnIndex) {
                return record.getName();
            }
        };
        AbstractTableColumn<EOProduct> quickLookColumn = new AbstractTableColumn<EOProduct>("Quick look", Number.class) {
            @Override
            public Object getCellValue(EOProduct record, int rowIndex, int columnIndex) {
                return Integer.toString(rowIndex + 1);
            }
        };
        List<AbstractTableColumn<EOProduct>> columnNames = new ArrayList<AbstractTableColumn<EOProduct>>();
        columnNames.add(numberColumn);
        columnNames.add(productNameColumn);
        columnNames.add(quickLookColumn);

        CustomTableModel<EOProduct> tableModel = new CustomTableModel<EOProduct>(columnNames);
        this.productsTable = new CustomTable<EOProduct>(tableModel);
        this.productsTable.setVisibleRowCount(3);
        this.productsTable.setDefaultRenderer(Number.class, new LabelTableCellRenderer(JLabel.CENTER));
        this.productsTable.setDefaultRenderer(EOProduct.class, new LabelTableCellRenderer(JLabel.LEFT));
        this.productsTable.setBackground(Color.WHITE);
        this.productsTable.setFillsViewportHeight(true);
        this.productsTable.setOpaque(true);
    }

    private void searchButtonPressed() {
        AbstractProductsDataSource selectedDataSource = (AbstractProductsDataSource)this.dataSourcesComboBox.getSelectedItem();
        String selectedMission = selectedDataSource.getSelectedMission();
        Map<String, Object> parametersValues = selectedDataSource.getParameterValues();
        int threadId = this.loadingIndicatorPanel.getNewCurrentThreadId();
        DownloadProductListTimerRunnable thread = new DownloadProductListTimerRunnable(this.loadingIndicatorPanel, threadId, selectedMission, parametersValues) {
            @Override
            protected void onSuccessfullyFinish(List<EOProduct> results) {
                onSuccessfullyDownloadedProductList(results);
            }
        };
        thread.executeAsync();
    }

    private void onSuccessfullyDownloadedProductList(List<EOProduct> results) {
        this.productsTable.getModel().setRecordsAndFireEvent(results);
    }

    private void newDataSourceSelected(AbstractProductsDataSource selectedDataSource) {
        this.verticalSplitPane.setTopComponent(selectedDataSource);
        this.productsTable.getModel().clearRecordsAndFireEvent();
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
