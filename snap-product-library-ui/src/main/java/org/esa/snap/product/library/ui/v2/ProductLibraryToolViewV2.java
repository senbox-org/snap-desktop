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

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.product.library.ui.v2.table.CustomLayeredPane;
import org.esa.snap.product.library.ui.v2.table.CustomSplitPane;
import org.esa.snap.product.library.v2.ProductLibraryItem;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.loading.CustomFileChooser;
import org.esa.snap.ui.loading.IComponentsEnabled;
import org.esa.snap.ui.loading.LoadingIndicatorPanel;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.JFileChooser;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.nio.file.Path;
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
    private LoadingIndicatorPanel loadingIndicatorPanel;
    private Path lastSelectedFolderPath;
    private QueryParametersPanel parameretersPanel;
    private QueryProductResultsPanel productResultsPanel;

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

    private int getGapBetweenRows() {
        return 5;
    }

    private void initialize() {
        int gapBetweenRows = getGapBetweenRows();
        int gapBetweenColumns = getGapBetweenRows();

        Insets defaultTextFieldMargins = buildDefaultTextFieldMargins();
        Insets defaultListItemMargins = buildDefaultListItemMargins();
        JTextField productNameTextField = new JTextField();
        productNameTextField.setMargin(defaultTextFieldMargins);
        int textFieldPreferredHeight = productNameTextField.getPreferredSize().height;

        ItemListener dataSourceListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    refreshDataSourceParameters();
                }
            }
        };
        IMissionParameterListener missionParameterListener = new IMissionParameterListener() {
            @Override
            public void newSelectedMission(String mission, AbstractProductsDataSource parentDataSource) {
                if (parentDataSource == parameretersPanel.getSelectedDataSource()) {
                    refreshDataSourceMissionParameters();
                } else {
                    throw new IllegalStateException("The selected mission '"+mission+"' does not belong to the visible data source.");
                }
            }
        };
        ActionListener searchButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchButtonPressed();
            }
        };
        this.parameretersPanel = new QueryParametersPanel(defaultListItemMargins, textFieldPreferredHeight, gapBetweenRows, gapBetweenColumns, searchButtonListener,
                                         dataSourceListener, missionParameterListener);
        this.parameretersPanel.setBorder(new EmptyBorder(0, 0, 0, 1));

        ActionListener downloadProductListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadSelectedProductAsync();
            }
        };
        this.productResultsPanel = new QueryProductResultsPanel(downloadProductListener);
        this.productResultsPanel.setBorder(new EmptyBorder(0, 1, 0, 0));

        CustomSplitPane verticalSplitPane = new CustomSplitPane(JSplitPane.HORIZONTAL_SPLIT, 1, 2);
        verticalSplitPane.setLeftComponent(this.parameretersPanel);
        verticalSplitPane.setRightComponent(this.productResultsPanel);

        IComponentsEnabled componentsEnabled = new IComponentsEnabled() {
            @Override
            public void setComponentsEnabled(boolean enabled) {
                parameretersPanel.setParametersEnabledWhileDownloading(enabled);
            }
        };
        this.loadingIndicatorPanel = new LoadingIndicatorPanel(componentsEnabled);

        CustomLayeredPane layeredPane = new CustomLayeredPane(new BorderLayout(0, gapBetweenRows));
        layeredPane.addToContentPanel(verticalSplitPane, BorderLayout.CENTER);
        layeredPane.addPanelToModalLayerAndPositionInCenter(this.loadingIndicatorPanel);

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(gapBetweenRows, gapBetweenColumns, gapBetweenRows, gapBetweenColumns));
        add(layeredPane, BorderLayout.CENTER);
    }

    private void refreshDataSourceParameters() {
        hideLoadingIndicatorPanel();
        this.parameretersPanel.refreshDataSourceParameters();
        this.productResultsPanel.clearProducts();
    }

    private void refreshDataSourceMissionParameters() {
        hideLoadingIndicatorPanel();
        this.parameretersPanel.refreshDataSourceMissionParameters();
        this.productResultsPanel.clearProducts();
    }

    private void downloadSelectedProductAsync() {
        CustomFileChooser fileChooser = buildFileChooser("Select folder to download the product", false, JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        if (this.lastSelectedFolderPath != null) {
            fileChooser.setCurrentDirectoryPath(this.lastSelectedFolderPath);
        }
        int result = fileChooser.showDialog(this, "Select");
        if (result == JFileChooser.APPROVE_OPTION) {
            this.lastSelectedFolderPath = fileChooser.getSelectedPath();
            AbstractProductsDataSource selectedDataSource = this.parameretersPanel.getSelectedDataSource();
            ProductLibraryItem selectedProduct = this.productResultsPanel.getSelectedProduct();
            int threadId = this.loadingIndicatorPanel.getNewCurrentThreadId();
            DownloadProductTimerRunnable thread = new DownloadProductTimerRunnable(this.loadingIndicatorPanel, threadId, selectedDataSource.getName(),
                                                                                   selectedProduct, this.lastSelectedFolderPath, this);
            thread.executeAsync(); // start the thread
        }
    }

    private static CustomFileChooser buildFileChooser(String dialogTitle, boolean multiSelectionEnabled, int fileSelectionMode) {
        boolean previousReadOnlyFlag = UIManager.getDefaults().getBoolean(CustomFileChooser.FILE_CHOOSER_READ_ONLY_KEY);
        CustomFileChooser fileChooser = new CustomFileChooser(previousReadOnlyFlag);
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setMultiSelectionEnabled(multiSelectionEnabled);
        fileChooser.setFileSelectionMode(fileSelectionMode);
        return fileChooser;
    }

    private void searchButtonPressed() {
        AbstractProductsDataSource selectedDataSource = this.parameretersPanel.getSelectedDataSource();
        String selectedMission = selectedDataSource.getSelectedMission();
        Map<String, Object> parametersValues = selectedDataSource.getParameterValues();
        int threadId = this.loadingIndicatorPanel.getNewCurrentThreadId();
        Credentials credentials = new UsernamePasswordCredentials("jcoravu", "jcoravu@yahoo.com");
        this.productResultsPanel.clearProducts();
        DownloadProductListTimerRunnable thread = new DownloadProductListTimerRunnable(this.loadingIndicatorPanel, threadId, credentials, this, this.productResultsPanel,
                                                                                       selectedDataSource.getName(), selectedMission, parametersValues) {
            @Override
            protected void onSuccessfullyFinish(List<ProductLibraryItem> downloadedProductList) {
                super.onSuccessfullyFinish(downloadedProductList);

                downloadQuickLookImagesAsync(downloadedProductList, getCredentials());
            }
        };
        thread.executeAsync(); // start the thread
    }

    private void downloadQuickLookImagesAsync(List<ProductLibraryItem> downloadedProductList, Credentials credentials) {
        int threadId = this.loadingIndicatorPanel.getNewCurrentThreadId();
        Runnable runnable = new DownloadQuickLookImagesRunnable(this.loadingIndicatorPanel, threadId, downloadedProductList, credentials, this.productResultsPanel);
        Thread thread = new Thread(runnable);
        thread.start(); // start the thread
    }

    private void hideLoadingIndicatorPanel() {
        this.loadingIndicatorPanel.stopRunningAndHide();
    }

    private Insets buildDefaultTextFieldMargins() {
        return new Insets(3, 2, 3, 2);
    }

    private Insets buildDefaultListItemMargins() {
        return new Insets(3, 2, 3, 2);
    }
}
