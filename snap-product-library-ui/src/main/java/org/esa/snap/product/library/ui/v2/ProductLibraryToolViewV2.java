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

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.graphbuilder.rcp.dialogs.BatchGraphDialog;
import org.esa.snap.product.library.ui.v2.preferences.RepositoriesCredentialsController;
import org.esa.snap.product.library.ui.v2.preferences.RepositoriesCredentialsControllerUI;
import org.esa.snap.product.library.ui.v2.preferences.model.RemoteRepositoryCredentials;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.repository.local.AddLocalRepositoryFolderTimerRunnable;
import org.esa.snap.product.library.ui.v2.repository.local.AllLocalProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.local.DeleteAllLocalRepositoriesTimerRunnable;
import org.esa.snap.product.library.ui.v2.repository.local.DeleteLocalProductsRunnable;
import org.esa.snap.product.library.ui.v2.repository.local.LocalParameterValues;
import org.esa.snap.product.library.ui.v2.repository.local.LocalProductsPopupListeners;
import org.esa.snap.product.library.ui.v2.repository.local.OpenLocalProductsRunnable;
import org.esa.snap.product.library.ui.v2.repository.local.ScanAllLocalRepositoryFoldersTimerRunnable;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProductListTimerRunnable;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadRemoteProductsHelper;
import org.esa.snap.product.library.ui.v2.repository.remote.OpenDownloadedProductsRunnable;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductDownloader;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelperImpl;
import org.esa.snap.product.library.ui.v2.worldwind.PolygonMouseListener;
import org.esa.snap.product.library.ui.v2.worldwind.WorldMapPanelWrapper;
import org.esa.snap.product.library.v2.database.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.LocalRepositoryProduct;
import org.esa.snap.product.library.v2.database.SaveDownloadedProductData;
import org.esa.snap.product.library.v2.database.SaveProductData;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RemoteRepositoriesManager;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.loading.CustomFileChooser;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Path2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger logger = Logger.getLogger(ProductLibraryToolViewV2.class.getName());

    public static final String PREFERENCES_KEY_LAST_LOCAL_REPOSITORY_FOLDER_PATH = "last_local_repository_folder_path";

    private Path lastLocalRepositoryFolderPath;
    private RepositoryProductListPanel repositoryProductListPanel;
    private RepositorySelectionPanel repositorySelectionPanel;
    private CustomSplitPane horizontalSplitPane;

    private AbstractProgressTimerRunnable<?> searchProductListThread;
    private AbstractProgressTimerRunnable<?> localRepositoryProductsThread;
    private DownloadRemoteProductsHelper downloadRemoteProductsHelper;
    private int textFieldPreferredHeight;
    private WorldMapPanelWrapper worldWindowPanel;
    private RemoteRepositoriesSemaphore remoteRepositoriesSemaphore;
    private boolean inputDataLoaded;
    private AppContext appContext;

    public ProductLibraryToolViewV2() {
        super();

        setDisplayName(Bundle.CTL_ProductLibraryTopComponentV2Name());
        this.inputDataLoaded = false;
    }

    @Override
    protected void componentOpened() {
        if (this.downloadRemoteProductsHelper == null) {
            initialize();
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();

        if (!this.inputDataLoaded) {
            this.inputDataLoaded = true;
            LoadInputDataRunnable thread = new LoadInputDataRunnable() {
                @Override
                protected void onSuccessfullyExecuting(LocalParameterValues parameterValues) {
                    repositorySelectionPanel.setInputData(parameterValues);
                }
            };
            thread.executeAsync();
        }
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

    private void refreshUserAccounts() {
        List<RemoteRepositoryCredentials> repositoriesCredentials = RepositoriesCredentialsController.getInstance().getRepositoriesCredentials();
        this.repositorySelectionPanel.refreshUserAccounts(repositoriesCredentials);
    }

    private void initialize() {
        this.appContext = SnapApp.getDefault().getAppContext();
        String lastFolderPath = this.appContext.getPreferences().getPropertyString(PREFERENCES_KEY_LAST_LOCAL_REPOSITORY_FOLDER_PATH, null);
        if (lastFolderPath != null) {
            this.lastLocalRepositoryFolderPath = Paths.get(lastFolderPath);
        }

        Insets defaultTextFieldMargins = new Insets(3, 2, 3, 2);
        JTextField productNameTextField = new JTextField();
        productNameTextField.setMargin(defaultTextFieldMargins);
        this.textFieldPreferredHeight = productNameTextField.getPreferredSize().height;

        createWorldWindowPanel();
        RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders = createRepositorySelectionPanel();
        createProductListPanel();

        int gapBetweenRows = getGapBetweenRows();
        int gapBetweenColumns = getGapBetweenRows();
        Color transparentDividerColor = new Color(255, 255, 255, 0);
        this.horizontalSplitPane = new CustomSplitPane(JSplitPane.HORIZONTAL_SPLIT, gapBetweenColumns-2, 0, transparentDividerColor);
        this.horizontalSplitPane.setLeftComponent(this.repositorySelectionPanel.getSelectedRepository());
        this.horizontalSplitPane.setRightComponent(this.repositoryProductListPanel);

        setLayout(new BorderLayout(0, gapBetweenRows));
        setBorder(new EmptyBorder(gapBetweenRows, gapBetweenColumns, gapBetweenRows, gapBetweenColumns));
        add(this.repositorySelectionPanel, BorderLayout.NORTH);
        add(this.horizontalSplitPane, BorderLayout.CENTER);

        this.repositorySelectionPanel.refreshRepositoryParameterComponents();

        this.appContext.getApplicationWindow().addPropertyChangeListener(RepositoriesCredentialsControllerUI.REMOTE_PRODUCTS_REPOSITORY_CREDENTIALS, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        refreshUserAccounts();
                    }
                });
            }
        });

        this.remoteRepositoriesSemaphore = new RemoteRepositoriesSemaphore(remoteRepositoryProductProviders);
        ProgressBarHelperImpl progressBarHelper = this.repositoryProductListPanel.getProgressBarHelper();
        this.downloadRemoteProductsHelper = new DownloadRemoteProductsHelper(progressBarHelper, this.remoteRepositoriesSemaphore, this.repositoryProductListPanel) {
            @Override
            protected void onFinishSavingProduct(SaveDownloadedProductData saveProductData) {
                ProductLibraryToolViewV2.this.repositorySelectionPanel.finishDownloadingProduct(saveProductData);
            }
        };
    }

    private void createProductListPanel() {
        ActionListener stopDownloadingProductsButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopDownloadingProducts();
            }
        };
        String text = DownloadRemoteProductsHelper.buildProgressBarDownloadingText(100, 100);
        JLabel label = new JLabel(text);
        int progressBarWidth = (int)(1.1f * label.getPreferredSize().width);
        this.repositoryProductListPanel = new RepositoryProductListPanel(this.repositorySelectionPanel, this, stopDownloadingProductsButtonListener, progressBarWidth);
        this.repositoryProductListPanel.setBorder(new EmptyBorder(0, 1, 0, 0));
        this.repositoryProductListPanel.getProductListPanel().addDataChangedListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                productListChanged();
            }
        });
        this.repositoryProductListPanel.getProductListPanel().addSelectionChangedListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                newSelectedRepositoryProducts();
            }
        });
        addListeners();
    }

    private RemoteProductsRepositoryProvider[] createRepositorySelectionPanel() {
        ItemListener repositoriesItemListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    refreshRepositoryParameterComponents();
                }
            }
        };
        MissionParameterListener missionParameterListener = new MissionParameterListener() {
            @Override
            public void newSelectedMission(String mission, AbstractProductsRepositoryPanel parentDataSource) {
                if (parentDataSource == repositorySelectionPanel.getSelectedRepository()) {
                    refreshRepositoryMissionParameters();
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
        ActionListener stopDownloadingProductListButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopSearchingProductList();
            }
        };

        RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders = RemoteRepositoriesManager.getRemoteProductsRepositoryProviders();
        if (remoteRepositoryProductProviders.length > 1) {
            // sort alphabetically by repository name
            Comparator<RemoteProductsRepositoryProvider> comparator = new Comparator<RemoteProductsRepositoryProvider>() {
                @Override
                public int compare(RemoteProductsRepositoryProvider o1, RemoteProductsRepositoryProvider o2) {
                    return o1.getRepositoryName().compareToIgnoreCase(o2.getRepositoryName());
                }
            };
            for (int i=0; i<remoteRepositoryProductProviders.length-1; i++) {
                for (int j=i+1; j<remoteRepositoryProductProviders.length; j++) {
                    int result = comparator.compare(remoteRepositoryProductProviders[i], remoteRepositoryProductProviders[j]);
                    if (result > 0) {
                        RemoteProductsRepositoryProvider aux = remoteRepositoryProductProviders[i];
                        remoteRepositoryProductProviders[i] = remoteRepositoryProductProviders[j];
                        remoteRepositoryProductProviders[j] = aux;
                    }
                }
            }
        }
        String text = DownloadProductListTimerRunnable.buildProgressBarDownloadingText(1000, 1000);
        JLabel label = new JLabel(text);
        int progressBarWidth = (int)(1.1f * label.getPreferredSize().width);

        this.repositorySelectionPanel = new RepositorySelectionPanel(remoteRepositoryProductProviders, this, missionParameterListener, this.worldWindowPanel, progressBarWidth);
        this.repositorySelectionPanel.setRepositoriesItemListener(repositoriesItemListener);
        this.repositorySelectionPanel.setSearchButtonListener(searchButtonListener);
        this.repositorySelectionPanel.setStopButtonListener(stopDownloadingProductListButtonListener);
        this.repositorySelectionPanel.setDataSourcesBorder(new EmptyBorder(0, 0, 0, 1));

        return remoteRepositoryProductProviders;
    }

    private void createWorldWindowPanel() {
        PolygonMouseListener worldWindowMouseListener = new PolygonMouseListener() {
            @Override
            public void leftMouseButtonClicked(List<Path2D.Double> polygonPaths) {
                ProductLibraryToolViewV2.this.leftMouseButtonClicked(polygonPaths);
            }
        };
        this.worldWindowPanel = new WorldMapPanelWrapper(worldWindowMouseListener);
        this.worldWindowPanel.setPreferredSize(new Dimension(400, 250));
        this.worldWindowPanel.addWorldMapPanelAsync(false, true);
    }

    private void addListeners() {
        ActionListener openLocalProductListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                openLocalSelectedProducts();
            }
        };
        ActionListener deleteLocalProductListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteLocalSelectedProducts();
            }
        };
        ActionListener batchProcessingListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                openBatchProcessingDialog();
            }
        };
        ActionListener showInExplorerListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showSelectedLocalProductInExplorer();
            }
        };
        LocalProductsPopupListeners localProductsPopupListeners = new LocalProductsPopupListeners(openLocalProductListener, deleteLocalProductListener, batchProcessingListener, showInExplorerListener);

        ActionListener scanLocalRepositoryFoldersListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                scanAllLocalRepositoriesButtonPressed();
            }
        };
        ActionListener addLocalRepositoryFolderListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                addLocalRepositoryButtonPressed();
            }
        };
        ActionListener deleteLocalRepositoryFolderListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteAllLocalRepositoriesButtonPressed();
            }
        };
        this.repositorySelectionPanel.setLocalRepositoriesListeners(localProductsPopupListeners, scanLocalRepositoryFoldersListener,
                                                                           addLocalRepositoryFolderListener, deleteLocalRepositoryFolderListener);

        ActionListener downloadRemoteProductListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadSelectedRemoteProductsButtonPressed();
            }
        };
        ActionListener openDownloadedRemoteProductListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openDownloadedRemoteProductsButtonPressed();
            }
        };
        this.repositorySelectionPanel.setDownloadRemoteProductListener(downloadRemoteProductListener, openDownloadedRemoteProductListener);
    }

    private void scanAllLocalRepositoriesButtonPressed() {
        if (this.downloadRemoteProductsHelper.isRunning() || this.localRepositoryProductsThread != null) {
            StringBuilder message = new StringBuilder();
            message.append("The local repository folders cannot be refreshed.")
                    .append("\n\n")
                    .append("There is a running action.");
            JOptionPane.showMessageDialog(ProductLibraryToolViewV2.this, message.toString(), "Scan all local repositories", JOptionPane.ERROR_MESSAGE);
        } else {
            ProgressBarHelperImpl progressBarHelper = this.repositoryProductListPanel.getProgressBarHelper();
            int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
            this.localRepositoryProductsThread = new ScanAllLocalRepositoryFoldersTimerRunnable(progressBarHelper, threadId) {
                @Override
                protected void onStopExecuting() {
                    ProductLibraryToolViewV2.this.localRepositoryProductsThread = null; // reset
                    searchProductListLater();
                }

                @Override
                protected void onLocalRepositoryFolderDeleted(LocalRepositoryFolder localRepositoryFolder) {
                    ProductLibraryToolViewV2.this.deleteLocalRepositoryFolder(localRepositoryFolder);
                }
            };
            this.localRepositoryProductsThread.executeAsync(); // start the thread
        }
    }

    private void searchProductListLater() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                searchButtonPressed();
            }
        });
    }

    private void addLocalRepositoryButtonPressed() {
        if (this.downloadRemoteProductsHelper.isRunning() || this.localRepositoryProductsThread != null) {
            StringBuilder message = new StringBuilder();
            message.append("A local repository folder cannot be added.")
                    .append("\n\n")
                    .append("There is a running action.");
            JOptionPane.showMessageDialog(ProductLibraryToolViewV2.this, message.toString(), "Add local repository folder", JOptionPane.ERROR_MESSAGE);
        } else {
            Path selectedLocalRepositoryFolder = showDialogToSelectLocalFolder("Select folder to add the products");
            if (selectedLocalRepositoryFolder != null) {
                ProgressBarHelperImpl progressBarHelper = this.repositoryProductListPanel.getProgressBarHelper();
                int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
                this.localRepositoryProductsThread = new AddLocalRepositoryFolderTimerRunnable(progressBarHelper, threadId, selectedLocalRepositoryFolder) {
                    @Override
                    protected void onStopExecuting() {
                        ProductLibraryToolViewV2.this.localRepositoryProductsThread = null; // reset
                        searchProductListLater();
                    }

                    @Override
                    protected void onFinishSavingProduct(SaveProductData saveProductData) {
                        ProductLibraryToolViewV2.this.repositorySelectionPanel.finishSavingProduct(saveProductData);
                    }
                };
                this.localRepositoryProductsThread.executeAsync(); // start the thread
            }
        }
    }

    private void deleteAllLocalRepositoriesButtonPressed() {
        AllLocalProductsRepositoryPanel allLocalProductsRepositoryPanel = this.repositorySelectionPanel.getAllLocalProductsRepositoryPanel();
        List<LocalRepositoryFolder> localRepositoryFoldersToDelete = allLocalProductsRepositoryPanel.getLocalRepositoryFolders();
        if (localRepositoryFoldersToDelete.size() > 0) {
            // there are local repositories into the application
            String dialogTitle = "Delete local products";
            if (this.localRepositoryProductsThread != null || this.downloadRemoteProductsHelper.isRunning()) {
                StringBuilder message = new StringBuilder();
                message.append("The local repositories cannot be deleted.")
                        .append("\n\n")
                        .append("There is a running action.");
                JOptionPane.showMessageDialog(this, message.toString(), dialogTitle, JOptionPane.ERROR_MESSAGE);
            } else {
                StringBuilder message = new StringBuilder();
                message.append("All the local repositories will be deleted.")
                        .append("\n\n")
                        .append("Are you sure you want to continue?");
                int answer = JOptionPane.showConfirmDialog(this, message.toString(), dialogTitle, JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION) {
                    allLocalProductsRepositoryPanel.clearParameterValues();
                    ProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
                    productListModel.clear();
                    ProgressBarHelperImpl progressBarHelper = this.repositorySelectionPanel.getProgressBarHelper();
                    int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
                    this.localRepositoryProductsThread = new DeleteAllLocalRepositoriesTimerRunnable(progressBarHelper, threadId, localRepositoryFoldersToDelete) {
                        @Override
                        protected void onStopExecuting() {
                            ProductLibraryToolViewV2.this.localRepositoryProductsThread = null; // reset
                            searchProductListLater();
                        }

                        @Override
                        protected void onLocalRepositoryFolderDeleted(LocalRepositoryFolder localRepositoryFolder) {
                            ProductLibraryToolViewV2.this.deleteLocalRepositoryFolder(localRepositoryFolder);
                        }
                    };
                    this.localRepositoryProductsThread.executeAsync(); // start the thread
                }
            }
        }
    }

    private void deleteLocalRepositoryFolder(LocalRepositoryFolder localRepositoryFolderToRemove) {
        AllLocalProductsRepositoryPanel allLocalProductsRepositoryPanel = this.repositorySelectionPanel.getAllLocalProductsRepositoryPanel();
        allLocalProductsRepositoryPanel.deleteLocalRepositoryFolder(localRepositoryFolderToRemove);
    }

    private void openLocalSelectedProducts() {
        RepositoryProduct[] unopenedSelectedProducts = processUnopenedSelectedProducts();
        ProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
        List<RepositoryProduct> productsToOpen = productListModel.addPendingOpenProducts(unopenedSelectedProducts);
        if (productsToOpen.size() > 0) {
            OpenLocalProductsRunnable runnable = new OpenLocalProductsRunnable(this.appContext, this.repositoryProductListPanel, productsToOpen);
            runnable.executeAsync(); // start the thread
        }
    }

    private void deleteLocalSelectedProducts() {
        RepositoryProduct[] unopenedSelectedProducts = processUnopenedSelectedProducts();
        if (unopenedSelectedProducts.length > 0) {
            // there are selected products into the output table
            StringBuilder message = new StringBuilder();
            if (unopenedSelectedProducts.length > 1) {
                message.append("The selected products");
            } else {
                message.append("The selected product");
            }
            message.append(" will be deleted.")
                    .append("\n\n")
                    .append("Are you sure you want to continue?");
            int answer = JOptionPane.showConfirmDialog(this, message.toString(), "Delete local products", JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                ProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
                List<RepositoryProduct> productsToDelete = productListModel.addPendingDeleteProducts(unopenedSelectedProducts);
                if (productsToDelete.size() > 0) {
                    DeleteLocalProductsRunnable runnable = new DeleteLocalProductsRunnable(this.appContext, this.repositoryProductListPanel, productsToDelete);
                    runnable.executeAsync(); // start the thread
                }
            }
        }
    }

    private RepositoryProduct[] processUnopenedSelectedProducts() {
        RepositoryProduct[] selectedProducts = this.repositoryProductListPanel.getProductListPanel().getSelectedProducts();
        List<RepositoryProduct> availableLocalProducts = new ArrayList<>(selectedProducts.length);
        for (int i=0; i<selectedProducts.length; i++) {
            Product product = this.appContext.getProductManager().getProduct(selectedProducts[i].getName());
            if (product == null) {
                // the local product is not opened in the application
                availableLocalProducts.add(selectedProducts[i]);
            }
        }
        selectedProducts = new RepositoryProduct[availableLocalProducts.size()];
        availableLocalProducts.toArray(selectedProducts);
        return selectedProducts;
    }

    private void openBatchProcessingDialog() {
        RepositoryProduct[] selectedProducts = this.repositoryProductListPanel.getProductListPanel().getSelectedProducts();
        File[] selectedProductsFiles = new File[selectedProducts.length];
        for (int i = 0; i < selectedProducts.length; i++) {
            selectedProductsFiles[i] = ((LocalRepositoryProduct) selectedProducts[i]).getPath().toFile();
        }
        BatchGraphDialog batchDialog = new BatchGraphDialog(this.appContext, "Batch Processing", "batchProcessing", true);
        batchDialog.setInputFiles(selectedProductsFiles);
        batchDialog.show();
    }

    private void showSelectedLocalProductInExplorer() {
        RepositoryProduct[] selectedProducts = this.repositoryProductListPanel.getProductListPanel().getSelectedProducts();
        Path selectedProductPath = ((LocalRepositoryProduct) selectedProducts[0]).getPath();
        try {
            Desktop.getDesktop().open(selectedProductPath.toFile());
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Failed to open the product in the explorer.", exception);
        }
    }

    private void leftMouseButtonClicked(List<Path2D.Double> polygonPaths) {
        this.repositoryProductListPanel.getProductListPanel().selectProductsByPolygonPath(polygonPaths);
    }

    private void productListChanged() {
        Path2D.Double[] polygonPaths = this.repositoryProductListPanel.getProductListPanel().getPolygonPaths();
        this.worldWindowPanel.setPolygons(polygonPaths);
    }

    private void newSelectedRepositoryProducts() {
        RepositoryProduct[] selectedProducts = this.repositoryProductListPanel.getProductListPanel().getSelectedProducts();
        Path2D.Double[] polygonPaths = new Path2D.Double[selectedProducts.length];
        for (int i = 0; i < selectedProducts.length; i++) {
            polygonPaths[i] = selectedProducts[i].getPolygon().getPath();
        }
        this.worldWindowPanel.highlightPolygons(polygonPaths);
        if (polygonPaths.length == 1) {
            this.worldWindowPanel.setEyePosition(polygonPaths[0]);
        }
    }

    private void stopSearchingProductList() {
        this.repositorySelectionPanel.getProgressBarHelper().hideProgressPanel();
        if (this.searchProductListThread != null) {
            this.searchProductListThread.stopRunning(); // stop the thread
        }
    }

    private void refreshRepositoryParameterComponents() {
        stopSearchingProductList();
        int dividerLocation = this.horizontalSplitPane.getDividerLocation();
        this.horizontalSplitPane.setLeftComponent(this.repositorySelectionPanel.getSelectedRepository());
        this.horizontalSplitPane.setDividerLocation(dividerLocation);
        this.horizontalSplitPane.revalidate();
        this.horizontalSplitPane.repaint();
        this.repositorySelectionPanel.refreshRepositoryParameterComponents();
        ProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
        productListModel.clear();
    }

    private void refreshRepositoryMissionParameters() {
        stopSearchingProductList();
        this.repositorySelectionPanel.refreshRepositoryParameterComponents();
        ProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
        productListModel.clear();
    }

    private void stopDownloadingProducts() {
        this.repositoryProductListPanel.getProductListPanel().removePendingDownloadProducts();
        this.repositoryProductListPanel.getProgressBarHelper().hideProgressPanel();
        this.downloadRemoteProductsHelper.stopRunning(); // stop the thread
    }

    private Path showDialogToSelectLocalFolder(String dialogTitle) {
        CustomFileChooser fileChooser = buildFileChooser(dialogTitle, false, JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        if (this.lastLocalRepositoryFolderPath != null) {
            fileChooser.setCurrentDirectoryPath(this.lastLocalRepositoryFolderPath);
        }
        int result = fileChooser.showDialog(this, "Select");
        if (result == JFileChooser.APPROVE_OPTION) {
            this.lastLocalRepositoryFolderPath = fileChooser.getSelectedPath();
            // save the folder path into the preferences
            this.appContext.getPreferences().setPropertyString(PREFERENCES_KEY_LAST_LOCAL_REPOSITORY_FOLDER_PATH, this.lastLocalRepositoryFolderPath.toString());
            return this.lastLocalRepositoryFolderPath;
        }
        return null;
    }

    private void openDownloadedRemoteProductsButtonPressed() {
        RepositoryProduct[] unopenedSelectedProducts = processUnopenedSelectedProducts();
        ProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
        Map<RepositoryProduct, Path> productsToOpen = productListModel.addPendingOpenDownloadedProducts(unopenedSelectedProducts);
        if (productsToOpen.size() > 0) {
            OpenDownloadedProductsRunnable runnable = new OpenDownloadedProductsRunnable(this.appContext, this.repositoryProductListPanel, productsToOpen);
            runnable.executeAsync(); // start the thread
        }
    }

    private void downloadSelectedRemoteProductsButtonPressed() {
        RepositoryProduct[] selectedProducts = this.repositoryProductListPanel.getProductListPanel().getSelectedProducts();
        if (selectedProducts.length > 0) {
            // there are selected products into the output table
            if (this.localRepositoryProductsThread == null) {
                Path selectedLocalRepositoryFolder = showDialogToSelectLocalFolder("Select folder to download the product");
                if (selectedLocalRepositoryFolder != null) {
                    AbstractProductsRepositoryPanel selectedRepository = this.repositorySelectionPanel.getSelectedRepository();
                    if (selectedRepository instanceof RemoteProductsRepositoryPanel) {
                        List<RepositoryProduct> productsToDownload = this.repositoryProductListPanel.getProductListPanel().addPendingDownloadProducts(selectedProducts);
                        if (productsToDownload.size() > 0) {
                            RemoteProductsRepositoryPanel remoteProductsRepositoryPanel = (RemoteProductsRepositoryPanel)selectedRepository;
                            RemoteProductDownloader[] remoteProductDownloaders = new RemoteProductDownloader[productsToDownload.size()];
                            for (int i=0; i<productsToDownload.size(); i++) {
                                remoteProductDownloaders[i] = remoteProductsRepositoryPanel.buildProductDownloader(productsToDownload.get(i), selectedLocalRepositoryFolder);
                            }
                            this.downloadRemoteProductsHelper.downloadProductsAsync(remoteProductDownloaders);
                        }
                    } else {
                        throw new IllegalStateException("The selected repository is not a remote repository.");
                    }
                }
            } else {
                StringBuilder message = new StringBuilder();
                if (selectedProducts.length > 1) {
                    message.append("The selected products");
                } else {
                    message.append("The selected product");
                }
                message.append(" cannot be downloaded.")
                        .append("\n\n")
                        .append("There is a running action .");
                JOptionPane.showMessageDialog(ProductLibraryToolViewV2.this, message.toString(), "Download products", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void searchButtonPressed() {
        ThreadListener threadListener = new ThreadListener() {
            @Override
            public void onStopExecuting() {
                ProductLibraryToolViewV2.this.searchProductListThread = null; // reset
            }
        };
        ProgressBarHelperImpl progressBarHelper = this.repositorySelectionPanel.getProgressBarHelper();
        int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
        AbstractProductsRepositoryPanel selectedRepository = this.repositorySelectionPanel.getSelectedRepository();
        AbstractProgressTimerRunnable<?> thread = selectedRepository.buildThreadToSearchProducts(progressBarHelper, threadId, threadListener, this.remoteRepositoriesSemaphore,
                                                                                                 this.repositoryProductListPanel);
        if (thread != null) {
            ProductListModel productListModel = this.repositoryProductListPanel.getProductListPanel().getProductListModel();
            productListModel.clear();
            progressBarHelper.updateProgressBarText(threadId, getSearchingProductListMessage() + "...");
            this.searchProductListThread = thread;
            this.searchProductListThread.executeAsync(); // start the thread
        }
    }

    public static String getSearchingProductListMessage() {
        return "Searching product list";
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
