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

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.core.util.ServiceLoader;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.repository.LocalFolderProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.v2.repository.ProductRepositoryDownloader;
import org.esa.snap.product.library.v2.repository.ProductsRepositoryProvider;
import org.esa.snap.product.library.v2.repository.ProductListRepositoryDownloader;
import org.esa.snap.product.library.v2.RepositoryProduct;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.loading.CustomFileChooser;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
public class ProductLibraryToolViewV2 extends ToolTopComponent implements ComponentDimension {

    private static final Insets LIST_ITEM_MARGINS = new Insets(3, 2, 3, 2);

    private boolean initialized;
    private Path lastSelectedFolderPath;
    private QueryProductResultsPanel productResultsPanel;
    private RepositorySelectionPanel repositoriesPanel;
    private CustomSplitPane verticalSplitPane;

    private AbstractProgressTimerRunnable<?> currentRunningThread;
    private DownloadQuickLookImagesRunnable downloadQuickLookImagesRunnable;
    private int textFieldPreferredHeight;
    private Credentials credentials;

    public ProductLibraryToolViewV2() {
        super();

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

    @Override
    public Insets getListItemMargins() {
        return LIST_ITEM_MARGINS;
    }

    @Override
    public int getGapBetweenRows() {
        return 5;
    }

    @Override
    public int getGapBetweenColumns() {
        return 5;
    }

    @Override
    public int getTextFieldPreferredHeight() {
        return this.textFieldPreferredHeight;
    }

    private void initialize() {
        Insets defaultTextFieldMargins = new Insets(3, 2, 3, 2);
        JTextField productNameTextField = new JTextField();
        productNameTextField.setMargin(defaultTextFieldMargins);
        this.textFieldPreferredHeight = productNameTextField.getPreferredSize().height;

        ItemListener dataSourceListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    refreshRepositoryParameters();
                }
            }
        };
        IMissionParameterListener missionParameterListener = new IMissionParameterListener() {
            @Override
            public void newSelectedMission(String mission, AbstractProductsRepositoryPanel parentDataSource) {
                if (parentDataSource == repositoriesPanel.getSelectedDataSource()) {
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
        ActionListener stopButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopProgressPanel();
            }
        };

        ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
        ServiceRegistry<ProductsRepositoryProvider> serviceRegistry = serviceRegistryManager.getServiceRegistry(ProductsRepositoryProvider.class);
        ServiceLoader.loadServices(serviceRegistry);
        Set<ProductsRepositoryProvider> repositoryProductsProviders = serviceRegistry.getServices();

        ProductsRepositoryProvider[] dataSourceProductProviders = new ProductsRepositoryProvider[repositoryProductsProviders.size()];
        Iterator<ProductsRepositoryProvider> it = repositoryProductsProviders.iterator();
        int index = 0;
        while (it.hasNext()) {
            ProductsRepositoryProvider productsProvider = it.next();
            dataSourceProductProviders[index++] = productsProvider;
        }

        this.repositoriesPanel = new RepositorySelectionPanel(dataSourceProductProviders, this, searchButtonListener, dataSourceListener, stopButtonListener, missionParameterListener);
        this.repositoriesPanel.setDataSourcesBorder(new EmptyBorder(0, 0, 0, 1));

        ActionListener downloadProductListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadSelectedProductAsync();
            }
        };
        this.productResultsPanel = new QueryProductResultsPanel(downloadProductListener);
        this.productResultsPanel.setBorder(new EmptyBorder(0, 1, 0, 0));

        this.verticalSplitPane = new CustomSplitPane(JSplitPane.HORIZONTAL_SPLIT, 1, 2);
        this.verticalSplitPane.setLeftComponent(this.repositoriesPanel.getSelectedDataSource());
        this.verticalSplitPane.setRightComponent(this.productResultsPanel);

        int gapBetweenRows = getGapBetweenRows();
        int gapBetweenColumns = getGapBetweenRows();

        JPanel contentPanel = new JPanel(new BorderLayout(0, gapBetweenRows));
        contentPanel.add(this.repositoriesPanel, BorderLayout.NORTH);
        contentPanel.add(this.verticalSplitPane, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(gapBetweenRows, gapBetweenColumns, gapBetweenRows, gapBetweenColumns));
        add(contentPanel, BorderLayout.CENTER);
    }

    private void stopProgressPanel() {
        this.repositoriesPanel.hideProgressPanel();
        if (this.currentRunningThread != null) {
            this.currentRunningThread.stopRunning();
        }
    }

    private void refreshRepositoryParameters() {
        stopProgressPanel();
        stopRunningDownloadQuickLookImagesThreads();
        this.verticalSplitPane.setLeftComponent(this.repositoriesPanel.getSelectedDataSource());
        this.verticalSplitPane.revalidate();
        this.verticalSplitPane.repaint();
        this.productResultsPanel.clearProducts();
    }

    private void refreshDataSourceMissionParameters() {
        stopProgressPanel();
        stopRunningDownloadQuickLookImagesThreads();
        this.repositoriesPanel.refreshDataSourceMissionParameters();
        this.productResultsPanel.clearProducts();
    }

    private void stopRunningDownloadQuickLookImagesThreads() {
        if (this.downloadQuickLookImagesRunnable != null) {
            this.downloadQuickLookImagesRunnable.stopRunning();
        }
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
            AbstractProductsRepositoryPanel selectedDataSource = this.repositoriesPanel.getSelectedDataSource();
            RepositoryProduct selectedProduct = this.productResultsPanel.getSelectedProduct();
            ProductRepositoryDownloader dataSourceProductDownloader = selectedDataSource.buidProductDownloader(selectedProduct.getMission());
            int threadId = this.repositoriesPanel.incrementAndGetCurrentThreadId();
            DownloadProductTimerRunnable thread = new DownloadProductTimerRunnable(this.repositoriesPanel, threadId, selectedDataSource.getName(), dataSourceProductDownloader,
                                                                                   selectedProduct, this.lastSelectedFolderPath, this.productResultsPanel, this) {

                @Override
                protected void onSuccessfullyFinish(Path targetFolderPath) {
                    super.onSuccessfullyFinish(targetFolderPath);

                    onSuccessfullyDownloadedProduct(targetFolderPath);
                }

                @Override
                protected void onStopExecuting() {
                    ProductLibraryToolViewV2.this.currentRunningThread = null; // reset
                }
            };
            startRunningThread(thread);
        }
    }

    private void onSuccessfullyDownloadedProduct(Path targetFolderPath) {
        LocalFolderProductsRepositoryPanel localFolderProducts = new LocalFolderProductsRepositoryPanel(targetFolderPath);
        this.repositoriesPanel.addNewLocalFolderProductsDataSource(localFolderProducts);
    }

    private Credentials getUserCredentials() {
        if (this.credentials == null) {
            LoginDialog loginDialog = new LoginDialog(SnapApp.getDefault().getMainFrame(), "User credentials");
            loginDialog.show();
            if (loginDialog.areCredentialsEntered()) {
                this.credentials = new UsernamePasswordCredentials(loginDialog.getUsername(), loginDialog.getPassword());
            }
        }
        return this.credentials;
    }

    private void searchButtonPressed() {
        AbstractProductsRepositoryPanel selectedDataSource = this.repositoriesPanel.getSelectedDataSource();
        Map<String, Object> parameterValues = selectedDataSource.getParameterValues();
        if (parameterValues != null) {
            Credentials credentials = getUserCredentials();
            if (credentials != null) {
                ProductListRepositoryDownloader dataSourceResults = selectedDataSource.buildResultsDownloader();
                String selectedMission = selectedDataSource.getSelectedMission();
                int threadId = this.repositoriesPanel.incrementAndGetCurrentThreadId();
                this.productResultsPanel.clearProducts();
                DownloadProductListTimerRunnable thread = new DownloadProductListTimerRunnable(this.repositoriesPanel, threadId, credentials, dataSourceResults,
                        this, this.productResultsPanel, selectedDataSource.getName(), selectedMission, parameterValues) {

                    @Override
                    protected void onStopExecuting() {
                        ProductLibraryToolViewV2.this.currentRunningThread = null; // reset
                        downloadQuickLookImagesAsync(getCredentials());
                    }
                };
                startRunningThread(thread);
            }
        }
    }

    private void startRunningThread(AbstractProgressTimerRunnable<?> newRunningThread) {
        this.currentRunningThread = newRunningThread;
        this.currentRunningThread.executeAsync(); // start the thread
    }

    private void downloadQuickLookImagesAsync(Credentials credentials) {
        List<RepositoryProduct> downloadedProductList = this.productResultsPanel.getListModel().getProducts();
        AbstractProductsRepositoryPanel selectedDataSource = this.repositoriesPanel.getSelectedDataSource();
        ProductListRepositoryDownloader dataSourceResults = selectedDataSource.buildResultsDownloader();
        this.downloadQuickLookImagesRunnable = new DownloadQuickLookImagesRunnable(downloadedProductList, credentials, dataSourceResults, this.productResultsPanel) {
            @Override
            protected void onStopExecuting() {
                ProductLibraryToolViewV2.this.downloadQuickLookImagesRunnable = null; // reset
            }
        };
        this.downloadQuickLookImagesRunnable.executeAsync();
    }

    private static CustomFileChooser buildFileChooser(String dialogTitle, boolean multiSelectionEnabled, int fileSelectionMode) {
        boolean previousReadOnlyFlag = UIManager.getDefaults().getBoolean(CustomFileChooser.FILE_CHOOSER_READ_ONLY_KEY);
        CustomFileChooser fileChooser = new CustomFileChooser(previousReadOnlyFlag);
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setMultiSelectionEnabled(multiSelectionEnabled);
        fileChooser.setFileSelectionMode(fileSelectionMode);
        return fileChooser;
    }
}
