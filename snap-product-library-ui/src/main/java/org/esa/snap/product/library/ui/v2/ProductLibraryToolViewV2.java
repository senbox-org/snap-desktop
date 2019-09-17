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
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.local.AllLocalProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProductsTimerRunnable;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadRemoteProductsQueue;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductDownloader;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelperImpl;
import org.esa.snap.product.library.ui.v2.worldwind.PolygonMouseListener;
import org.esa.snap.product.library.ui.v2.worldwind.WorldWindowPanelWrapper;
import org.esa.snap.product.library.v2.database.SaveProductData;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.remote.products.repository.ProductRepositoryDownloader;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
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
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Path2D;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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

    private Path lastSelectedFolderPath;
    private RemoteRepositoryProductListPanel productResultsPanel;
    private RepositorySelectionPanel repositorySelectionPanel;
    private CustomSplitPane verticalSplitPane;

    private AbstractProgressTimerRunnable<?> searchProductListThread;
    private AbstractRunnable<?> downloadQuickLookImagesRunnable;
    private int textFieldPreferredHeight;
    private WorldWindowPanelWrapper worldWindowPanel;
    private DownloadRemoteProductsQueue downloadRemoteProductsQueue;

    public ProductLibraryToolViewV2() {
        super();

        setDisplayName(Bundle.CTL_ProductLibraryTopComponentV2Name());
    }

    @Override
    protected void componentOpened() {
        if (this.downloadRemoteProductsQueue == null) {
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

        ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
        ServiceRegistry<RemoteProductsRepositoryProvider> serviceRegistry = serviceRegistryManager.getServiceRegistry(RemoteProductsRepositoryProvider.class);
        Set<RemoteProductsRepositoryProvider> repositoryProductsProviders = serviceRegistry.getServices();

        RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders = new RemoteProductsRepositoryProvider[repositoryProductsProviders.size()];
        Iterator<RemoteProductsRepositoryProvider> it = repositoryProductsProviders.iterator();
        int index = 0;
        while (it.hasNext()) {
            RemoteProductsRepositoryProvider productsProvider = it.next();
            remoteRepositoryProductProviders[index++] = productsProvider;
        }

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

        PolygonMouseListener worldWindowMouseListener = new PolygonMouseListener() {
            @Override
            public void leftMouseButtonClicked(List<Path2D.Double> polygonPaths) {
                ProductLibraryToolViewV2.this.leftMouseButtonClicked(polygonPaths);
            }
        };
        this.worldWindowPanel = new WorldWindowPanelWrapper();
        this.worldWindowPanel.setPreferredSize(new Dimension(500, 500));
        this.worldWindowPanel.addWorldWindowPanelAsync(false, true, worldWindowMouseListener);

        ActionListener downloadRemoteProductListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadSelectedProductAsync();
            }
        };
        this.repositorySelectionPanel = new RepositorySelectionPanel(remoteRepositoryProductProviders, this, downloadRemoteProductListener, missionParameterListener, worldWindowPanel);
        this.repositorySelectionPanel.setRepositoriesItemListener(repositoriesItemListener);
        this.repositorySelectionPanel.setSearchButtonListener(searchButtonListener);
        this.repositorySelectionPanel.setStopButtonListener(stopDownloadingProductListButtonListener);
        this.repositorySelectionPanel.setDataSourcesBorder(new EmptyBorder(0, 0, 0, 1));

        ActionListener stopDownloadingProductsButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopDownloadingProducts();
            }
        };
        this.productResultsPanel = new RemoteRepositoryProductListPanel(this.repositorySelectionPanel, this, stopDownloadingProductsButtonListener);
        this.productResultsPanel.setBorder(new EmptyBorder(0, 1, 0, 0));
        this.productResultsPanel.setListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent listDataEvent) {
                productListChanged();
            }

            @Override
            public void intervalRemoved(ListDataEvent listDataEvent) {
                productListChanged();
            }

            @Override
            public void contentsChanged(ListDataEvent listDataEvent) {
                productListChanged();
            }
        });
        this.productResultsPanel.setProductListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (!listSelectionEvent.getValueIsAdjusting()) {
                    newSelectedRepositoryProducts();
                }
            }
        });

        this.verticalSplitPane = new CustomSplitPane(JSplitPane.HORIZONTAL_SPLIT, 1, 2);
        this.verticalSplitPane.setLeftComponent(this.repositorySelectionPanel.getSelectedRepository());
        this.verticalSplitPane.setRightComponent(this.productResultsPanel);

        int gapBetweenRows = getGapBetweenRows();
        int gapBetweenColumns = getGapBetweenRows();

        JPanel contentPanel = new JPanel(new BorderLayout(0, gapBetweenRows));
        contentPanel.add(this.repositorySelectionPanel, BorderLayout.NORTH);
        contentPanel.add(this.verticalSplitPane, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(gapBetweenRows, gapBetweenColumns, gapBetweenRows, gapBetweenColumns));
        add(contentPanel, BorderLayout.CENTER);

        this.repositorySelectionPanel.refreshRepositoryParameterComponents();

        this.downloadRemoteProductsQueue = new DownloadRemoteProductsQueue();
    }

    private void leftMouseButtonClicked(List<Path2D.Double> polygonPaths) {
        this.productResultsPanel.selectProductsByPolygonPath(polygonPaths);
    }

    private void productListChanged() {
        Path2D.Double[] polygonPaths = this.productResultsPanel.getPolygonPaths();
        this.worldWindowPanel.setPolygons(polygonPaths);
    }

    private void newSelectedRepositoryProducts() {
        RepositoryProduct[] selectedProducts = this.productResultsPanel.getSelectedProducts();
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
            this.searchProductListThread.stopRunning();
        }
    }

    private void refreshRepositoryParameterComponents() {
        stopSearchingProductList();
        stopDownloadingQuickLookImages();
        this.verticalSplitPane.setLeftComponent(this.repositorySelectionPanel.getSelectedRepository());
        this.verticalSplitPane.revalidate();
        this.verticalSplitPane.repaint();
        this.repositorySelectionPanel.refreshRepositoryParameterComponents();
        this.productResultsPanel.clearProducts();
    }

    private void refreshRepositoryMissionParameters() {
        stopSearchingProductList();
        stopDownloadingQuickLookImages();
        this.repositorySelectionPanel.refreshRepositoryParameterComponents();
        this.productResultsPanel.clearProducts();
    }

    private void stopDownloadingProducts() {
        synchronized (this.downloadRemoteProductsQueue) {
            this.downloadRemoteProductsQueue.clear();
        }
        this.productResultsPanel.getListModel().removePendingDownloadProducts();
        this.productResultsPanel.getProgressBarHelper().hideProgressPanel();
        if (this.downloadProductsThread != null) {
            this.downloadProductsThread.stopRunning();
        }
    }

    private void stopDownloadingQuickLookImages() {
        if (this.downloadQuickLookImagesRunnable != null) {
            this.downloadQuickLookImagesRunnable.stopRunning();
        }
    }

    private DownloadProductsTimerRunnable downloadProductsThread;

    private void downloadSelectedProductAsync() {
        CustomFileChooser fileChooser = buildFileChooser("Select folder to download the product", false, JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        if (this.lastSelectedFolderPath != null) {
            fileChooser.setCurrentDirectoryPath(this.lastSelectedFolderPath);
        }
        int result = fileChooser.showDialog(this, "Select");
        if (result == JFileChooser.APPROVE_OPTION) {
            this.lastSelectedFolderPath = fileChooser.getSelectedPath();
            AbstractProductsRepositoryPanel selectedRepository = this.repositorySelectionPanel.getSelectedRepository();
            if (selectedRepository instanceof RemoteProductsRepositoryPanel) {
                RemoteProductsRepositoryProvider productsRepositoryProvider = ((RemoteProductsRepositoryPanel) selectedRepository).getProductsRepositoryProvider();
                RepositoryProduct[] selectedProducts = this.productResultsPanel.getSelectedProducts();

                this.productResultsPanel.getListModel().addPendingDownloadProducts(selectedProducts);

                int queueSizeBeforeAddingProducts;
                synchronized (this.downloadRemoteProductsQueue) {
                    queueSizeBeforeAddingProducts = this.downloadRemoteProductsQueue.getSize();
                    for (int i=0; i<selectedProducts.length; i++) {
                        ProductRepositoryDownloader productRepositoryDownloader = productsRepositoryProvider.buidProductDownloader(selectedProducts[i].getMission());
                        RemoteProductDownloader remoteProductDownloader = new RemoteProductDownloader(selectedProducts[i], productRepositoryDownloader, this.lastSelectedFolderPath);
                        this.downloadRemoteProductsQueue.push(remoteProductDownloader);
                    }
                }

                boolean startProductsDownloadThread = false;
                if (queueSizeBeforeAddingProducts == 0 || this.downloadProductsThread == null) {
                    startProductsDownloadThread = true;
                }
                if (startProductsDownloadThread) {
                    ProgressBarHelperImpl progressBarHelper = this.productResultsPanel.getProgressBarHelper();
                    int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
                    this.downloadProductsThread = new DownloadProductsTimerRunnable(progressBarHelper, threadId, this.downloadRemoteProductsQueue, this.productResultsPanel, this) {
                        @Override
                        protected void onStopExecuting() {
                            ProductLibraryToolViewV2.this.downloadProductsThread = null; // reset
                        }

                        @Override
                        protected void onFinishSavingProduct(SaveProductData saveProductData) {
                            finishSavingProduct(saveProductData);
                        }
                    };
                    this.downloadProductsThread.executeAsync();
                } else {
                    this.downloadProductsThread.updateDownloadedProgressPercentLater();
                }
            } else {
                throw new IllegalStateException("The selected repository is not a remote repository.");
            }
        }
    }

    private void finishSavingProduct(SaveProductData saveProductData) {
        AllLocalProductsRepositoryPanel allLocalProductsRepositoryPanel = this.repositorySelectionPanel.getAllLocalProductsRepositoryPanel();
        allLocalProductsRepositoryPanel.addMissionIfMissing(saveProductData.getRemoteMission());
    }

    private void searchButtonPressed() {
        ThreadListener threadListener = new ThreadListener() {
            @Override
            public void onStopExecuting(AbstractProductsRepositoryPanel productsRepositoryPanel) {
                ProductLibraryToolViewV2.this.searchProductListThread = null; // reset
                if (productsRepositoryPanel instanceof RemoteProductsRepositoryPanel) {
                    displayQuickLookImagesAsync(productsRepositoryPanel);
                }
            }
        };
        ProgressBarHelperImpl progressBarHelper = this.repositorySelectionPanel.getProgressBarHelper();
        int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
        AbstractProductsRepositoryPanel selectedRepository = this.repositorySelectionPanel.getSelectedRepository();
        AbstractProgressTimerRunnable<?> thread = selectedRepository.buildThreadToSearchProducts(progressBarHelper, threadId, threadListener, this.productResultsPanel);
        if (thread != null) {
            this.productResultsPanel.clearProducts();
            this.searchProductListThread = thread;
            this.searchProductListThread.executeAsync(); // start the thread
        }
    }

    private void displayQuickLookImagesAsync(AbstractProductsRepositoryPanel productsRepositoryPanel) {
        AbstractProductsRepositoryPanel selectedDataSource = this.repositorySelectionPanel.getSelectedRepository();
        if (productsRepositoryPanel == selectedDataSource) {
            ThreadListener threadListener = new ThreadListener() {
                @Override
                public void onStopExecuting(AbstractProductsRepositoryPanel productsRepositoryPanel) {
                    ProductLibraryToolViewV2.this.downloadQuickLookImagesRunnable = null; // reset
                }
            };
            List<RepositoryProduct> productList = this.productResultsPanel.getListModel().getProducts();
            this.downloadQuickLookImagesRunnable = selectedDataSource.buildThreadToDisplayQuickLookImages(productList, threadListener, this.productResultsPanel);
            this.downloadQuickLookImagesRunnable.executeAsync(); // stgar the thread
        } else {
            throw new IllegalStateException("The repository providers do not match.");
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
}
