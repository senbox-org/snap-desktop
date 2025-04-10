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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.Credentials;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.util.Pair;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIRegistry;
import org.esa.snap.graphbuilder.rcp.dialogs.BatchGraphDialog;
import org.esa.snap.graphbuilder.rcp.utils.ClipboardUtils;
import org.esa.snap.product.library.ui.v2.preferences.RepositoriesCredentialsControllerUI;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.RepositorySelectionPanel;
import org.esa.snap.product.library.ui.v2.repository.local.AddLocalRepositoryFolderTimerRunnable;
import org.esa.snap.product.library.ui.v2.repository.local.AllLocalProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.local.AttributesParameterComponent;
import org.esa.snap.product.library.ui.v2.repository.local.CopyLocalProductsRunnable;
import org.esa.snap.product.library.ui.v2.repository.local.DeleteAllLocalRepositoriesTimerRunnable;
import org.esa.snap.product.library.ui.v2.repository.local.DeleteLocalProductsRunnable;
import org.esa.snap.product.library.ui.v2.repository.local.ExportLocalProductListPathsRunnable;
import org.esa.snap.product.library.ui.v2.repository.local.LocalParameterValues;
import org.esa.snap.product.library.ui.v2.repository.local.MoveLocalProductsRunnable;
import org.esa.snap.product.library.ui.v2.repository.local.OpenLocalProductsRunnable;
import org.esa.snap.product.library.ui.v2.repository.local.ReadLocalProductsTimerRunnable;
import org.esa.snap.product.library.ui.v2.repository.local.ScanAllLocalRepositoryFoldersTimerRunnable;
import org.esa.snap.product.library.ui.v2.repository.local.ScanLocalRepositoryOptionsDialog;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductListModel;
import org.esa.snap.product.library.ui.v2.repository.output.OutputProductListPanel;
import org.esa.snap.product.library.ui.v2.repository.output.RepositoryOutputProductListPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.DownloadProgressStatus;
import org.esa.snap.product.library.ui.v2.repository.remote.OpenDownloadedProductsRunnable;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.repository.remote.RemoteRepositoriesSemaphore;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductListTimerRunnable;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductListener;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadProductRunnable;
import org.esa.snap.product.library.ui.v2.repository.remote.download.DownloadRemoteProductsHelper;
import org.esa.snap.product.library.ui.v2.repository.remote.download.popup.DownloadingProductsPopupMenu;
import org.esa.snap.product.library.ui.v2.thread.AbstractProgressTimerRunnable;
import org.esa.snap.product.library.ui.v2.thread.ProgressBarHelperImpl;
import org.esa.snap.product.library.ui.v2.thread.ThreadCallback;
import org.esa.snap.product.library.ui.v2.thread.ThreadListener;
import org.esa.snap.product.library.ui.v2.worldwind.WorldMapPanelWrapperImpl;
import org.esa.snap.worldwind.productlibrary.PolygonMouseListener;
import org.esa.snap.worldwind.productlibrary.WorldMapPanelWrapper;
import org.esa.snap.product.library.v2.database.AllLocalFolderProductsRepository;
import org.esa.snap.product.library.v2.database.AttributeFilter;
import org.esa.snap.product.library.v2.database.DataAccess;
import org.esa.snap.product.library.v2.database.SaveProductData;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryFolder;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryProduct;
import org.esa.snap.product.library.v2.preferences.RepositoriesCredentialsController;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RemoteMission;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.geometry.AbstractGeometry2D;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.help.HelpDisplayer;
import org.esa.snap.ui.loading.CustomFileChooser;
import org.esa.snap.ui.loading.CustomSplitPane;
import org.esa.snap.ui.loading.SwingUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * The Product Library Tool view representing the main panel.
 */
@TopComponent.Description(
        preferredID = "ProductLibraryTopComponentV2",
        iconBase = "org/esa/snap/product/library/ui/v2/icons/search.png"
)
@TopComponent.Registration(
        mode = "rightSlidingSide",
        openAtStartup = true,
        position = 0
)
@ActionID(category = "Window", id = "org.esa.snap.product.library.ui.v2.ProductLibraryToolViewV2")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows", position = 12),
        @ActionReference(path = "Menu/File", position = 17)
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ProductLibraryTopComponentV2Name",
        preferredID = "ProductLibraryTopComponentV2"
)
@NbBundle.Messages({
        "CTL_ProductLibraryTopComponentV2Name=Product Library",
        "CTL_ProductLibraryTopComponentV2Description=Product Library",
})
public class ProductLibraryToolViewV2 extends ToolTopComponent implements ComponentDimension, DownloadProductListener {

    private static final Logger logger = Logger.getLogger(ProductLibraryToolViewV2.class.getName());

    private static final String HELP_ID = "productLibraryToolV2";

    private static final String PREFERENCES_KEY_LAST_LOCAL_REPOSITORY_FOLDER_PATH = "last_local_repository_folder_path";

    private final static String LAST_ERROR_OUTPUT_DIR_KEY = "snap.lastErrorOutputDir";

    private Path lastLocalRepositoryFolderPath;
    private RepositoryOutputProductListPanel repositoryOutputProductListPanel;
    private RepositorySelectionPanel repositorySelectionPanel;
    private CustomSplitPane horizontalSplitPane;
    private DownloadingProductsPopupMenu downloadingProductsPopupMenu;

    private AbstractProgressTimerRunnable<?> searchProductListThread;
    private AbstractProgressTimerRunnable<?> localRepositoryProductsThread;
    private DownloadRemoteProductsHelper downloadRemoteProductsHelper;
    private int textFieldPreferredHeight;
    private WorldMapPanelWrapper worldWindowPanel;
    private boolean inputDataLoaded;
    private AppContext appContext;

    public ProductLibraryToolViewV2() {
        super();

        setDisplayName(Bundle.CTL_ProductLibraryTopComponentV2Name());
        this.inputDataLoaded = false;
    }

    @Override
    public void addNotify() {
        if (this.downloadRemoteProductsHelper == null) {
            initialize();
        }

        super.addNotify();

        if (!this.inputDataLoaded) {
            this.inputDataLoaded = true;
            AllLocalFolderProductsRepository allLocalFolderProductsRepository = this.repositorySelectionPanel.getAllLocalProductsRepositoryPanel().getAllLocalFolderProductsRepository();
            LoadInputDataRunnable thread = new LoadInputDataRunnable(allLocalFolderProductsRepository) {
                @Override
                protected void onSuccessfullyExecuting(LocalParameterValues parameterValues) {
                    onFinishLoadingInputData(parameterValues);
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

    @Override
    public Color getTextFieldBackgroundColor() {
        return Color.WHITE;
    }

    @Override
    public void onFinishDownloadingProduct(DownloadProductRunnable downloadProductRunnable, DownloadProgressStatus downloadProgressStatus,
                                           SaveProductData saveProductData, boolean hasProductsToDownload) {

        if (this.downloadingProductsPopupMenu != null) {
            this.downloadingProductsPopupMenu.onStopDownloadingProduct(downloadProductRunnable);
        }
        RepositoryProduct repositoryProduct = downloadProductRunnable.getProductToDownload();
        if (downloadProgressStatus != null) {
            this.repositorySelectionPanel.finishDownloadingProduct(repositoryProduct, downloadProgressStatus, saveProductData);
        }
        OutputProductListModel productListModel = this.repositoryOutputProductListPanel.getProductListPanel().getProductListModel();
        productListModel.refreshProduct(repositoryProduct);
    }

    @Override
    public void onUpdateProductDownloadProgress(RepositoryProduct repositoryProduct) {
        if (this.downloadingProductsPopupMenu != null) {
            this.downloadingProductsPopupMenu.onUpdateProductDownloadProgress(repositoryProduct);
        }
        OutputProductListModel productListModel = this.repositoryOutputProductListPanel.getProductListPanel().getProductListModel();
        productListModel.refreshProduct(repositoryProduct);
    }

    @Override
    public void onRefreshProduct(RepositoryProduct repositoryProduct) {
        OutputProductListModel productListModel = this.repositoryOutputProductListPanel.getProductListPanel().getProductListModel();
        productListModel.refreshProduct(repositoryProduct);
    }

    private void onFinishLoadingInputData(LocalParameterValues parameterValues) {
        this.repositorySelectionPanel.setInputData(parameterValues);
        this.repositoryOutputProductListPanel.setVisibleProductsPerPage(parameterValues.getVisibleProductsPerPage());
        this.downloadRemoteProductsHelper.setUncompressedDownloadedProducts(parameterValues.isUncompressedDownloadedProducts());
    }

    private void refreshUserAccounts() {
        RepositoriesCredentialsController controller = RepositoriesCredentialsController.getInstance();
        this.repositorySelectionPanel.refreshUserAccounts(controller.getRepositoriesCredentials());
        this.repositoryOutputProductListPanel.setVisibleProductsPerPage(controller.getNrRecordsOnPage());
        this.downloadRemoteProductsHelper.setUncompressedDownloadedProducts(controller.isAutoUncompress());
    }

    public static Integer findLocalAttributeAsInt(String attributeName, RepositoryProduct repositoryProduct) {
        List<Attribute> localAttributes = repositoryProduct.getLocalAttributes();
        for (Attribute attribute : localAttributes) {
            if (attribute.getName().equals(attributeName)) {
                return Integer.parseInt(attribute.getValue());
            }
        }
        return null;
    }

    private void onHideDownloadingProgressBar() {
        if (this.downloadingProductsPopupMenu != null) {
            this.downloadingProductsPopupMenu.setVisible(false);
            this.downloadingProductsPopupMenu = null;
        }
    }

    private void initialize() {
        this.appContext = SnapApp.getDefault().getAppContext();
        PropertyMap persistencePreferences = this.appContext.getPreferences();

        String lastFolderPath = persistencePreferences.getPropertyString(PREFERENCES_KEY_LAST_LOCAL_REPOSITORY_FOLDER_PATH, null);
        if (lastFolderPath != null) {
            this.lastLocalRepositoryFolderPath = Paths.get(lastFolderPath);
        }

        Insets defaultTextFieldMargins = new Insets(3, 2, 3, 2);
        JTextField productNameTextField = new JTextField();
        productNameTextField.setMargin(defaultTextFieldMargins);
        this.textFieldPreferredHeight = productNameTextField.getPreferredSize().height;

        RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders = RemoteProductsRepositoryProvider.getRemoteProductsRepositoryProviders();
        for (RemoteProductsRepositoryProvider provider : remoteRepositoryProductProviders) {
            DataAccess.saveRemoteRepositoryName(provider.getRepositoryName());
        }

        createWorldWindowPanel(persistencePreferences);
        createRepositorySelectionPanel(remoteRepositoryProductProviders);
        createProductListPanel();

        this.repositorySelectionPanel.addComponents(this.repositoryOutputProductListPanel.getProductListPaginationPanel());
        this.repositoryOutputProductListPanel.addPageProductsChangedListener(event -> outputProductsPageChanged());

        int gapBetweenRows = getGapBetweenRows();
        int gapBetweenColumns = getGapBetweenRows();
        int visibleDividerSize = gapBetweenColumns - 2;
        int dividerMargins = 0;
        float initialDividerLocationPercent = 0.5f;
        this.horizontalSplitPane = new CustomSplitPane(JSplitPane.HORIZONTAL_SPLIT, visibleDividerSize, dividerMargins, initialDividerLocationPercent, SwingUtils.TRANSPARENT_COLOR);
        this.horizontalSplitPane.setLeftComponent(this.repositorySelectionPanel.getSelectedProductsRepositoryPanel());
        this.horizontalSplitPane.setRightComponent(this.repositoryOutputProductListPanel);

        setLayout(new BorderLayout(0, gapBetweenRows));
        setBorder(new EmptyBorder(gapBetweenRows, gapBetweenColumns, gapBetweenRows, gapBetweenColumns));
        add(this.repositorySelectionPanel, BorderLayout.NORTH);
        add(this.horizontalSplitPane, BorderLayout.CENTER);

        this.repositorySelectionPanel.getSelectedProductsRepositoryPanel().addInputParameterComponents();

        RemoteRepositoriesSemaphore remoteRepositoriesSemaphore = new RemoteRepositoriesSemaphore(remoteRepositoryProductProviders);
        ProgressBarHelperImpl progressBarHelper = this.repositoryOutputProductListPanel.getProgressBarHelper();
        this.downloadRemoteProductsHelper = new DownloadRemoteProductsHelper(progressBarHelper, remoteRepositoriesSemaphore, this);

        this.repositorySelectionPanel.setDownloadingProductProgressCallback(this.downloadRemoteProductsHelper);
        this.repositoryOutputProductListPanel.getProductListPanel().getProductListModel().setDownloadingProductProgressCallback(this.downloadRemoteProductsHelper);

        this.appContext.getApplicationWindow().addPropertyChangeListener(RepositoriesCredentialsControllerUI.REMOTE_PRODUCTS_REPOSITORY_CREDENTIALS,
                event -> SwingUtilities.invokeLater(this::refreshUserAccounts));

        progressBarHelper.addVisiblePropertyChangeListener(event -> {
            if (!(Boolean) event.getNewValue()) {
                onHideDownloadingProgressBar();
            }
        });
        progressBarHelper.getProgressBar().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                showDownloadingProductsPopup((JComponent) mouseEvent.getSource());
            }
        });
    }

    private void showDownloadingProductsPopup(JComponent invoker) {
        List<Pair<DownloadProductRunnable, DownloadProgressStatus>> downloadingProductRunnables = this.downloadRemoteProductsHelper.findDownloadingProducts();
        if (downloadingProductRunnables.size() > 0) {
            Color backgroundColor = getTextFieldBackgroundColor();
            int gapBetweenRows = getGapBetweenRows() / 2;
            int gapBetweenColumns = getGapBetweenColumns() / 2;
            DownloadingProductsPopupMenu popupMenu = new DownloadingProductsPopupMenu(downloadingProductRunnables, gapBetweenRows, gapBetweenColumns, backgroundColor);
            popupMenu.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                    downloadingProductsPopupMenu = (DownloadingProductsPopupMenu) popupMenuEvent.getSource();
                    downloadingProductsPopupMenu.refresh(); // refresh the texts in the panels after registering the listener
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                    ProductLibraryToolViewV2.this.downloadingProductsPopupMenu = null; // reset the listener
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                    // do nothing
                }
            });
            int x = invoker.getWidth() - popupMenu.getPreferredSize().width;
            int y = invoker.getHeight();
            popupMenu.show(invoker, x, y);
        }
    }

    private void createProductListPanel() {
        ActionListener stopDownloadingProductsButtonListener = e -> cancelDownloadingProducts();
        String text = DownloadRemoteProductsHelper.buildProgressBarDownloadingText(100, 100);
        JLabel label = new JLabel(text);
        int progressBarWidth = (int) (1.1f * label.getPreferredSize().width);
        this.repositoryOutputProductListPanel = new RepositoryOutputProductListPanel(this.repositorySelectionPanel, this, stopDownloadingProductsButtonListener, progressBarWidth, false);
        this.repositoryOutputProductListPanel.setBorder(new EmptyBorder(0, 1, 0, 0));
        this.repositoryOutputProductListPanel.getProductListPanel().addDataChangedListener(evt -> productListChanged());
        this.repositoryOutputProductListPanel.getProductListPanel().addSelectionChangedListener(evt -> newSelectedRepositoryProducts());
        addListeners();
    }

    private void createRepositorySelectionPanel(RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders) {
        ItemListener productsRepositoryListener = itemEvent -> onSelectedNewProductsRepository();
        MissionParameterListener missionParameterListener = (mission, parentProductsRepositoryPanel) -> {
            if (parentProductsRepositoryPanel == repositorySelectionPanel.getSelectedProductsRepositoryPanel()) {
                onSelectedNewProductsRepositoryMission();
            } else {
                throw new IllegalStateException("The selected mission '" + mission + "' does not belong to the visible products repository.");
            }
        };
        ActionListener searchButtonListener = e -> searchButtonPressed();
        ActionListener stopDownloadingProductListButtonListener = e -> cancelSearchingProductList();
        ActionListener helpButtonListener = e -> {
            HelpDisplayer.show(HELP_ID);
        };

        String text = DownloadProductListTimerRunnable.buildProgressBarDownloadingText(1000, 1000);
        JLabel label = new JLabel(text);
        int progressBarWidth = (int) (1.1f * label.getPreferredSize().width);

        this.repositorySelectionPanel = new RepositorySelectionPanel(remoteRepositoryProductProviders, this, missionParameterListener, this.worldWindowPanel, progressBarWidth);
        this.repositorySelectionPanel.setRepositoriesItemListener(productsRepositoryListener);
        this.repositorySelectionPanel.setSearchButtonListener(searchButtonListener);
        this.repositorySelectionPanel.setHelpButtonListener(helpButtonListener);
        this.repositorySelectionPanel.setStopButtonListener(stopDownloadingProductListButtonListener);
        this.repositorySelectionPanel.setAllProductsRepositoryPanelBorder(new EmptyBorder(0, 0, 0, 1));
    }

    private void createWorldWindowPanel(PropertyMap persistencePreferences) {
        PolygonMouseListener worldWindowMouseListener = ProductLibraryToolViewV2.this::leftMouseButtonClicked;
        this.worldWindowPanel = new WorldMapPanelWrapperImpl(worldWindowMouseListener, getTextFieldBackgroundColor(), persistencePreferences);
        this.worldWindowPanel.setPreferredSize(new Dimension(400, 250));
        this.worldWindowPanel.addWorldMapPanelAsync(false, true);
    }

    private List<ProductLibraryV2Action> readLocalRepositoryActions() {
        FileObject fileObj = FileUtil.getConfigFile("ProductLibraryV2LocalRepositoryActions");
        List<ProductLibraryV2Action> localActions = new ArrayList<>();
        if (fileObj == null) {
            logger.log(Level.WARNING, "No ProductLibrary Action found.");
        } else {
            FileObject[] files = fileObj.getChildren();
            List<FileObject> orderedFiles = FileUtil.getOrder(Arrays.asList(files), true);
            for (FileObject fileObject : orderedFiles) {
                Class<? extends ProductLibraryV2Action> actionExtClass = OperatorUIRegistry.getClassAttribute(fileObject, "actionClass", ProductLibraryV2Action.class, false);
                try {
                    ProductLibraryV2Action action = Objects.requireNonNull(actionExtClass).getDeclaredConstructor().newInstance();
                    action.setProductLibraryToolView(this);
                    localActions.add(action);
                } catch (ReflectiveOperationException e) {
                    logger.log(Level.SEVERE, "Failed to instantiate the product library action using class '" + actionExtClass + "'.", e);
                }
            }
        }
        return localActions;
    }

    private int showConfirmDialog(String title, String message, int buttonsOptionType) {
        return JOptionPane.showConfirmDialog(this, message, title, buttonsOptionType);
    }

    public void showMessageDialog(String title, String message, int iconMessageType) {
        JOptionPane.showMessageDialog(this, message, title, iconMessageType);
    }

    private void addListeners() {
        List<ProductLibraryV2Action> localActions = readLocalRepositoryActions();

        ProductLibraryV2Action jointSearchCriteriaListener = new ProductLibraryV2Action("Joint Search Criteria") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                jointSearchCriteriaOptionClicked();
            }

            @Override
            public boolean canAddItemToPopupMenu(AbstractProductsRepositoryPanel visibleProductsRepositoryPanel, RepositoryProduct[] selectedProducts) {
                return (selectedProducts.length == 1 && selectedProducts[0].getRemoteMission() != null);
            }
        };
        ProductLibraryV2Action openLocalProductListener = new ProductLibraryV2Action("Open") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                openLocalSelectedProductsOptionClicked();
            }
        };
        ProductLibraryV2Action deleteLocalProductListener = new ProductLibraryV2Action("Delete...") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteLocalSelectedProductsOptionClicked();
            }
        };
        ProductLibraryV2Action batchProcessingListener = new ProductLibraryV2Action("Batch Processing...") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                openBatchProcessingDialog();
            }
        };
        ProductLibraryV2Action showInExplorerListener = new ProductLibraryV2Action("Show in Explorer...") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showSelectedLocalProductInExplorer();
            }

            @Override
            public boolean canAddItemToPopupMenu(AbstractProductsRepositoryPanel visibleProductsRepositoryPanel, RepositoryProduct[] selectedProducts) {
                return (selectedProducts.length == 1);
            }
        };
        ProductLibraryV2Action selectAllListener = new ProductLibraryV2Action("Select All") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                repositoryOutputProductListPanel.getProductListPanel().selectAllProducts();
            }
        };
        ProductLibraryV2Action selectNoneListener = new ProductLibraryV2Action("Select None") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                repositoryOutputProductListPanel.getProductListPanel().clearSelection();
            }
        };
        ProductLibraryV2Action copyListener = new ProductLibraryV2Action("Copy") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                copySelectedProductsOptionClicked();
            }
        };
        ProductLibraryV2Action copyToListener = new ProductLibraryV2Action("Copy To...") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                copyToSelectedProductsOptionClicked();
            }
        };
        ProductLibraryV2Action moveToListener = new ProductLibraryV2Action("Move To...") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveToSelectedProductsOptionClicked();
            }
        };
        ProductLibraryV2Action exportListListener = new ProductLibraryV2Action("Export List...") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                exportSelectedProductsListOptionClicked();
            }
        };
        localActions.add(openLocalProductListener);
        localActions.add(deleteLocalProductListener);
        localActions.add(selectAllListener);
        localActions.add(selectNoneListener);
        localActions.add(copyListener);
        localActions.add(copyToListener);
        localActions.add(moveToListener);
        localActions.add(exportListListener);
        localActions.add(jointSearchCriteriaListener);
        localActions.add(batchProcessingListener);
        localActions.add(showInExplorerListener);

        ActionListener scanLocalRepositoryFoldersListener = actionEvent -> scanAllLocalRepositoriesButtonPressed();
        ActionListener addLocalRepositoryFolderListener = actionEvent -> addLocalRepositoryButtonPressed();
        ActionListener deleteLocalRepositoryFolderListener = actionEvent -> deleteAllLocalRepositoriesButtonPressed();
        this.repositorySelectionPanel.setLocalRepositoriesListeners(scanLocalRepositoryFoldersListener, addLocalRepositoryFolderListener,
                deleteLocalRepositoryFolderListener, localActions);

        ProductLibraryV2Action downloadRemoteProductListener = new ProductLibraryV2Action("Download") {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadSelectedRemoteProductsButtonPressed();
            }

            @Override
            public boolean canAddItemToPopupMenu(AbstractProductsRepositoryPanel visibleProductsRepositoryPanel, RepositoryProduct[] selectedProducts) {
                return visibleProductsRepositoryPanel.getOutputProductResults().canDownloadProducts(selectedProducts);
            }
        };
        ProductLibraryV2Action openDownloadedRemoteProductListener = new ProductLibraryV2Action("Open") {
            @Override
            public void actionPerformed(ActionEvent e) {
                openDownloadedRemoteProductsButtonPressed();
            }

            @Override
            public boolean canAddItemToPopupMenu(AbstractProductsRepositoryPanel visibleProductsRepositoryPanel, RepositoryProduct[] selectedProducts) {
                return visibleProductsRepositoryPanel.getOutputProductResults().canOpenDownloadedProducts(selectedProducts);
            }
        };

        List<ProductLibraryV2Action> remoteActions = new ArrayList<>();
        remoteActions.add(openDownloadedRemoteProductListener);
        remoteActions.add(downloadRemoteProductListener);
        remoteActions.add(jointSearchCriteriaListener);

        this.repositorySelectionPanel.setDownloadRemoteProductListener(remoteActions);
    }

    private void scanAllLocalRepositories(boolean scanRecursively, boolean generateQuickLookImages, boolean testZipFileForErrors) {
        ProgressBarHelperImpl progressBarHelper = this.repositorySelectionPanel.getProgressBarHelper();
        int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
        AllLocalFolderProductsRepository allLocalFolderProductsRepository = this.repositorySelectionPanel.getAllLocalProductsRepositoryPanel().getAllLocalFolderProductsRepository();
        this.localRepositoryProductsThread = new ScanAllLocalRepositoryFoldersTimerRunnable(progressBarHelper, threadId, allLocalFolderProductsRepository, scanRecursively, generateQuickLookImages, testZipFileForErrors) {
            @Override
            protected void onFinishRunning() {
                onFinishProcessingLocalRepositoryThread(this, null); // 'null' => no local repository folder to select
            }

            @Override
            protected void onLocalRepositoryFolderDeleted(LocalRepositoryFolder localRepositoryFolder) {
                ProductLibraryToolViewV2.this.deleteLocalRepositoryFolder(localRepositoryFolder);
            }

            @Override
            protected void onFinishSavingProduct(SaveProductData saveProductData) {
                ProductLibraryToolViewV2.this.repositorySelectionPanel.finishSavingLocalProduct(saveProductData);
            }

            @Override
            protected void onSuccessfullyFinish(Map<File, String> errorFiles) {
                processErrorFiles(errorFiles);
            }
        };
        this.localRepositoryProductsThread.executeAsync(); // start the thread
    }

    private void scanAllLocalRepositoriesButtonPressed() {
        if (this.downloadRemoteProductsHelper.isRunning() || this.localRepositoryProductsThread != null) {
            showMessageDialog("Scan all local repositories",
                    "The local repository folders cannot be refreshed.\n\nThere is a running action.",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            ScanLocalRepositoryOptionsDialog dialog = new ScanLocalRepositoryOptionsDialog(null) {
                @Override
                protected void okButtonPressed(boolean scanRecursively, boolean generateQuickLookImages, boolean testZipFileForErrors) {
                    super.okButtonPressed(scanRecursively, generateQuickLookImages, testZipFileForErrors);
                    scanAllLocalRepositories(scanRecursively, generateQuickLookImages, testZipFileForErrors);
                }
            };
            dialog.show();
        }
    }

    private void searchProductListLater() {
        SwingUtilities.invokeLater(this::searchButtonPressed);
    }

    private void addLocalRepositoryButtonPressed() {
        if (this.localRepositoryProductsThread != null) {
            showMessageDialog("Add local repository folder",
                    "A local repository folder cannot be added./n/nThere is a running action.", JOptionPane.ERROR_MESSAGE);
        } else {
            Path selectedLocalRepositoryFolder = showDialogToSelectLocalFolder("Select folder to add the products", true);
            if (selectedLocalRepositoryFolder != null) {
                // a local folder has been selected
                ScanLocalRepositoryOptionsDialog dialog = new ScanLocalRepositoryOptionsDialog(null) {
                    @Override
                    protected void okButtonPressed(boolean scanRecursively, boolean generateQuickLookImages, boolean testZipFileForErrors) {
                        super.okButtonPressed(scanRecursively, generateQuickLookImages, testZipFileForErrors);
                        addLocalRepository(selectedLocalRepositoryFolder, scanRecursively, generateQuickLookImages, testZipFileForErrors);
                    }
                };
                dialog.show();
            }
        }
    }

    private void addLocalRepository(Path selectedLocalRepositoryFolder, boolean scanRecursively, boolean generateQuickLookImages, boolean testZipFileForErrors) {
        ProgressBarHelperImpl progressBarHelper = this.repositorySelectionPanel.getProgressBarHelper();
        int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
        AllLocalFolderProductsRepository allLocalFolderProductsRepository = this.repositorySelectionPanel.getAllLocalProductsRepositoryPanel().getAllLocalFolderProductsRepository();
        this.localRepositoryProductsThread = new AddLocalRepositoryFolderTimerRunnable(progressBarHelper, threadId, selectedLocalRepositoryFolder, allLocalFolderProductsRepository,
                scanRecursively, generateQuickLookImages, testZipFileForErrors) {
            @Override
            protected void onFinishRunning() {
                onFinishProcessingLocalRepositoryThread(this, getLocalRepositoryFolderPath());
            }

            @Override
            protected void onFinishSavingProduct(SaveProductData saveProductData) {
                ProductLibraryToolViewV2.this.repositorySelectionPanel.finishSavingLocalProduct(saveProductData);
            }

            @Override
            protected void onSuccessfullyFinish(Map<File, String> errorFiles) {
                processErrorFiles(errorFiles);
            }
        };
        this.localRepositoryProductsThread.executeAsync(); // start the thread
    }

    private void processErrorFiles(Map<File, String> errorFiles) {
        if (errorFiles != null && errorFiles.size() > 0) {
            final StringBuilder str = new StringBuilder();
            int cnt = 1;
            for (Map.Entry<File, String> entry : errorFiles.entrySet()) {
                str.append(entry.getValue()); // the message
                str.append("   ");
                str.append(entry.getKey().getAbsolutePath());
                str.append('\n');
                if (cnt >= 20) {
                    str.append("plus ").append(errorFiles.size() - 20).append(" other errors...\n");
                    break;
                }
                ++cnt;
            }
            final String question = "\nWould you like to save the list to a text file?";
            Dialogs.Answer answer = Dialogs.requestDecision("Product Errors",
                    "The follow files have errors:\n" + str + question,
                    false, null);
            if (answer == Dialogs.Answer.YES) {
                final File file = Dialogs.requestFileForSave("Save as...", false,
                        new SnapFileFilter("Text File", new String[]{".txt"}, "Text File"),
                        ".txt", "ProductErrorList", null, LAST_ERROR_OUTPUT_DIR_KEY);
                if (file != null) {
                    try {
                        writeErrors(errorFiles, file);
                    } catch (Exception e) {
                        Dialogs.showError("Unable to save to " + file.getAbsolutePath());
                    }
                    if (Desktop.isDesktopSupported() && file.exists()) {
                        try {
                            Desktop.getDesktop().open(file);
                        } catch (Exception e) {
                            SystemUtils.LOG.warning("Unable to open error file: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private boolean resetLocalRepositoryProductsThread(Runnable invokerThread) {
        if (invokerThread == this.localRepositoryProductsThread) {
            this.localRepositoryProductsThread = null; // reset
            return true;
        }
        return false;
    }

    private void deleteAllLocalRepositoriesButtonPressed() {
        AllLocalProductsRepositoryPanel allLocalProductsRepositoryPanel = this.repositorySelectionPanel.getAllLocalProductsRepositoryPanel();
        LocalRepositoryFolder[] folders;
        final LocalRepositoryFolder selectedFolder = allLocalProductsRepositoryPanel.getSelectedFolder();
        String messageToDisplay;
        if (selectedFolder != null) {
            folders = new LocalRepositoryFolder[]{selectedFolder};
            messageToDisplay = "Selected local repository will be deleted.";
        } else {
            List<LocalRepositoryFolder> localRepositoryFoldersToDelete = allLocalProductsRepositoryPanel.getLocalRepositoryFolders();
            folders = localRepositoryFoldersToDelete.toArray(new LocalRepositoryFolder[0]);
            messageToDisplay = "All the local repositories will be deleted.";
        }
        if (folders.length > 0) {
            // there are local repositories into the application
            String dialogTitle = "Delete local repositories";
            if (this.localRepositoryProductsThread != null || this.downloadRemoteProductsHelper.isRunning()) {
                // there is a running action
                String message = String.format("The local repositories cannot be deleted.%n%nThere is a running action.");
                showMessageDialog(dialogTitle, message, JOptionPane.ERROR_MESSAGE);
            } else {
                // the local repository folders can be deleted
                String message = String.format("%s%n%nAre you sure you want to continue?", messageToDisplay);
                int answer = showConfirmDialog(dialogTitle, message, JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION) {
                    allLocalProductsRepositoryPanel.clearInputParameterComponentValues();
                    this.repositoryOutputProductListPanel.clearOutputList(true);
                    ProgressBarHelperImpl progressBarHelper = this.repositorySelectionPanel.getProgressBarHelper();
                    int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
                    AllLocalFolderProductsRepository allLocalFolderProductsRepository = this.repositorySelectionPanel.getAllLocalProductsRepositoryPanel().getAllLocalFolderProductsRepository();
                    this.localRepositoryProductsThread = new DeleteAllLocalRepositoriesTimerRunnable(progressBarHelper, threadId, allLocalFolderProductsRepository, folders) {
                        @Override
                        protected void onFinishRunning() {
                            onFinishRunningLocalProductsThread(this, true);
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

    private void onFinishRunningLocalProductsThread(Runnable invokerThread, boolean startSearchProducts) {
        if (resetLocalRepositoryProductsThread(invokerThread)) {
            if (this.repositorySelectionPanel.getSelectedProductsRepositoryPanel() instanceof AllLocalProductsRepositoryPanel) {
                this.repositoryOutputProductListPanel.updateProductListCountTitle();
                // refresh the products in the output panel
                OutputProductListPanel productListPanel = this.repositoryOutputProductListPanel.getProductListPanel();
                productListPanel.getProductListModel().refreshProducts();
                if (startSearchProducts) {
                    searchProductListLater();
                }
            }
        }
    }

    private void deleteLocalRepositoryFolder(LocalRepositoryFolder localRepositoryFolderToRemove) {
        AllLocalProductsRepositoryPanel allLocalProductsRepositoryPanel = this.repositorySelectionPanel.getAllLocalProductsRepositoryPanel();
        allLocalProductsRepositoryPanel.deleteLocalRepositoryFolder(localRepositoryFolderToRemove);
    }

    private void onFinishProcessingLocalRepositoryThread(Runnable invokerThread, Path localRepositoryFolderPathToSelect) {
        if (resetLocalRepositoryProductsThread(invokerThread)) {
            if (this.repositorySelectionPanel.getSelectedProductsRepositoryPanel() instanceof AllLocalProductsRepositoryPanel) {
                // the local repository is selected
                AllLocalProductsRepositoryPanel allLocalProductsRepositoryPanel = (AllLocalProductsRepositoryPanel) this.repositorySelectionPanel.getSelectedProductsRepositoryPanel();
                allLocalProductsRepositoryPanel.updateInputParameterValues(localRepositoryFolderPathToSelect, null, null, null, null);
                this.repositoryOutputProductListPanel.clearOutputList(true);
                searchProductListLater();
            }
        }
    }

    private void copySelectedProductsOptionClicked() {
        RepositoryProduct[] selectedProducts = this.repositoryOutputProductListPanel.getProductListPanel().getSelectedProducts();
        if (selectedProducts.length > 0) {
            File[] fileList = new File[selectedProducts.length];
            for (int i = 0; i < selectedProducts.length; i++) {
                fileList[i] = new File(selectedProducts[i].getURL());
            }
            ClipboardUtils.copyToClipboard(fileList);
        }
    }

    private void copyToSelectedProductsOptionClicked() {
        RepositoryProduct[] selectedProducts = this.repositoryOutputProductListPanel.getProductListPanel().getSelectedProducts();
        if (selectedProducts.length > 0) {
            // there are selected products
            if (this.localRepositoryProductsThread != null) {
                String message = "The local products cannot be copied.\n\nThere is a running action.";
                showMessageDialog("Copy local products", message, JOptionPane.ERROR_MESSAGE);
            } else {
                Path selectedLocalTargetFolder = showDialogToSelectLocalFolder("Select folder to copy the products", false);
                if (selectedLocalTargetFolder != null) {
                    // a local folder has been selected
                    OutputProductListModel productListModel = this.repositoryOutputProductListPanel.getProductListPanel().getProductListModel();
                    List<RepositoryProduct> productsToCopy = productListModel.addPendingCopyLocalProducts(selectedProducts);
                    if (productsToCopy.size() > 0) {
                        // there are local products to copy
                        ProgressBarHelperImpl progressBarHelper = this.repositorySelectionPanel.getProgressBarHelper();
                        int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
                        this.localRepositoryProductsThread = new CopyLocalProductsRunnable(progressBarHelper, threadId, this.repositoryOutputProductListPanel,
                                selectedLocalTargetFolder, productsToCopy) {

                            @Override
                            protected void onFinishRunning() {
                                onFinishRunningLocalProductsThread(this, false);
                            }
                        };
                        this.localRepositoryProductsThread.executeAsync(); // start the thread
                    }
                }
            }
        }
    }

    private void moveToSelectedProductsOptionClicked() {
        RepositoryProduct[] unopenedSelectedProducts = processUnopenedSelectedProducts();
        if (unopenedSelectedProducts.length > 0) {
            if (this.localRepositoryProductsThread != null) {
                String message = "The local products cannot be moved.\n\nThere is a running action.";
                showMessageDialog("Move local products", message, JOptionPane.ERROR_MESSAGE);
            } else {
                Path selectedLocalTargetFolder = showDialogToSelectLocalFolder("Select folder to move the products", false);
                if (selectedLocalTargetFolder != null) {
                    // a local folder has been selected
                    OutputProductListModel productListModel = this.repositoryOutputProductListPanel.getProductListPanel().getProductListModel();
                    List<RepositoryProduct> productsToMove = productListModel.addPendingMoveLocalProducts(unopenedSelectedProducts);
                    if (productsToMove.size() > 0) {
                        // there are local products to move
                        ProgressBarHelperImpl progressBarHelper = this.repositorySelectionPanel.getProgressBarHelper();
                        int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
                        AllLocalFolderProductsRepository allLocalFolderProductsRepository = this.repositorySelectionPanel.getAllLocalProductsRepositoryPanel().getAllLocalFolderProductsRepository();
                        this.localRepositoryProductsThread = new MoveLocalProductsRunnable(progressBarHelper, threadId, this.repositoryOutputProductListPanel, selectedLocalTargetFolder,
                                productsToMove, allLocalFolderProductsRepository) {

                            @Override
                            protected void onFinishRunning() {
                                onFinishRunningLocalProductsThread(this, true);
                            }
                        };
                        this.localRepositoryProductsThread.executeAsync(); // start the thread
                    }
                }
            }
        }
    }

    private void openLocalSelectedProductsOptionClicked() {
        RepositoryProduct[] unopenedSelectedProducts = processUnopenedSelectedProducts();
        if (unopenedSelectedProducts.length > 0) {
            // there are selected products into the output table
            if (this.localRepositoryProductsThread != null) {
                String message = "The local products cannot be opened.\n\nThere is a running action.";
                showMessageDialog("Open local products", message, JOptionPane.ERROR_MESSAGE);
            } else {
                OutputProductListModel productListModel = this.repositoryOutputProductListPanel.getProductListPanel().getProductListModel();
                List<RepositoryProduct> productsToOpen = productListModel.addPendingOpenLocalProducts(unopenedSelectedProducts);
                if (productsToOpen.size() > 0) {
                    ProgressBarHelperImpl progressBarHelper = this.repositorySelectionPanel.getProgressBarHelper();
                    int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
                    this.localRepositoryProductsThread = new OpenLocalProductsRunnable(progressBarHelper, threadId, this.repositoryOutputProductListPanel, this.appContext, productsToOpen) {
                        @Override
                        protected void onFinishRunning() {
                            onFinishRunningLocalProductsThread(this, false);
                        }
                    };
                    this.localRepositoryProductsThread.executeAsync(); // start the thread
                }
            }
        }
    }

    private void exportSelectedProductsListOptionClicked() {
        RepositoryProduct[] selectedProducts = this.repositoryOutputProductListPanel.getProductListPanel().getSelectedProducts();
        if (selectedProducts.length > 0) {
            // there are selected products
            String extension = ".txt";
            FileFilter fileFilter = CustomFileChooser.buildFileFilter(extension, "*.txt");
            Path selectedFilePath = showDialogToSaveLocalFile("Export selected product list", fileFilter);
            if (selectedFilePath != null) {
                boolean canContinue = true;
                String fileName = selectedFilePath.getFileName().toString();
                if (!StringUtils.endsWithIgnoreCase(fileName, extension)) {
                    fileName += extension;
                    selectedFilePath = selectedFilePath.getParent().resolve(fileName); // add the '.txt' extension
                }
                if (Files.exists(selectedFilePath)) {
                    String message = String.format("The selected file '%s' already exists.%n%nAre you sure you want to overwrite?", selectedFilePath);
                    int answer = showConfirmDialog("Delete local products", message, JOptionPane.YES_NO_OPTION);
                    if (answer != JOptionPane.YES_OPTION) {
                        canContinue = false;
                    }
                }
                if (canContinue) {
                    Path[] localProductPaths = new Path[selectedProducts.length];
                    for (int i = 0; i < selectedProducts.length; i++) {
                        localProductPaths[i] = ((LocalRepositoryProduct) selectedProducts[i]).getPath();
                    }
                    ExportLocalProductListPathsRunnable runnable = new ExportLocalProductListPathsRunnable(this, selectedFilePath, localProductPaths);
                    runnable.executeAsync(); // start the thread
                }
            }
        }
    }

    public void readLocalProductsAsync(RepositoryProduct[] productsToRead, ThreadCallback<Product[]> threadCallback) {
        if (this.localRepositoryProductsThread == null) {
            ProgressBarHelperImpl progressBarHelper = this.repositorySelectionPanel.getProgressBarHelper();
            int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
            this.localRepositoryProductsThread = new ReadLocalProductsTimerRunnable(progressBarHelper, threadId, productsToRead, threadCallback) {
                @Override
                protected void onFinishRunning() {
                    resetLocalRepositoryProductsThread(this);
                }
            };
            this.localRepositoryProductsThread.executeAsync(); // start the thread
        } else {
            String message = "The local products cannot be read.\n\nThere is a running action.";
            showMessageDialog("Read local products", message, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteLocalSelectedProductsOptionClicked() {
        RepositoryProduct[] unopenedSelectedProducts = processUnopenedSelectedProducts();
        if (unopenedSelectedProducts.length > 0) {
            // there are selected products into the output table
            if (this.localRepositoryProductsThread != null) {
                String message = "The local products cannot be deleted.\n\nThere is a running action.";
                showMessageDialog("Delete local products", message, JOptionPane.ERROR_MESSAGE);
            } else {
                StringBuilder message = new StringBuilder();
                if (unopenedSelectedProducts.length > 1) {
                    message.append("The selected products");
                } else {
                    message.append("The selected product");
                }
                message.append(" will be deleted.")
                        .append("\n\n")
                        .append("Are you sure you want to continue?");
                int answer = showConfirmDialog("Delete local products", message.toString(), JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION) {
                    OutputProductListModel productListModel = this.repositoryOutputProductListPanel.getProductListPanel().getProductListModel();
                    List<RepositoryProduct> productsToDelete = productListModel.addPendingDeleteLocalProducts(unopenedSelectedProducts);
                    if (productsToDelete.size() > 0) {
                        ProgressBarHelperImpl progressBarHelper = this.repositorySelectionPanel.getProgressBarHelper();
                        int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
                        AllLocalFolderProductsRepository allLocalFolderProductsRepository = this.repositorySelectionPanel.getAllLocalProductsRepositoryPanel().getAllLocalFolderProductsRepository();
                        this.localRepositoryProductsThread = new DeleteLocalProductsRunnable(progressBarHelper, threadId, this.repositoryOutputProductListPanel,
                                productsToDelete, allLocalFolderProductsRepository) {

                            @Override
                            protected void onFinishRunning() {
                                onFinishRunningLocalProductsThread(this, true);
                            }
                        };
                        this.localRepositoryProductsThread.executeAsync(); // start the thread
                    }
                }
            }
        }
    }

    private void openBatchProcessingDialog() {
        RepositoryProduct[] selectedProducts = this.repositoryOutputProductListPanel.getProductListPanel().getSelectedProducts();
        File[] selectedProductsFiles = new File[selectedProducts.length];
        for (int i = 0; i < selectedProducts.length; i++) {
            selectedProductsFiles[i] = ((LocalRepositoryProduct) selectedProducts[i]).getPath().toFile();
        }
        BatchGraphDialog batchDialog = new BatchGraphDialog(this.appContext, "Batch Processing", "batchProcessing", true);
        batchDialog.setInputFiles(selectedProductsFiles);
        batchDialog.show();
    }

    private RepositoryProduct[] processUnopenedSelectedProducts() {
        RepositoryProduct[] selectedProducts = this.repositoryOutputProductListPanel.getProductListPanel().getSelectedProducts();
        List<RepositoryProduct> availableLocalProducts = new ArrayList<>(selectedProducts.length);
        for (RepositoryProduct selectedProduct : selectedProducts) {
            Product product = this.appContext.getProductManager().getProduct(selectedProduct.getName());
            if (product == null) {
                // the local product is not opened in the application
                availableLocalProducts.add(selectedProduct);
            }
        }
        selectedProducts = new RepositoryProduct[availableLocalProducts.size()];
        availableLocalProducts.toArray(selectedProducts);
        return selectedProducts;
    }

    private void leftMouseButtonClicked(List<Path2D.Double> polygonPaths) {
        this.repositoryOutputProductListPanel.getProductListPanel().selectProductsByPolygonPath(polygonPaths);
    }

    private void productListChanged() {
        Path2D.Double[] polygonPaths = this.repositoryOutputProductListPanel.getProductListPanel().getPolygonPaths();
        this.worldWindowPanel.setPolygons(polygonPaths);
    }

    private void showSelectedLocalProductInExplorer() {
        RepositoryProduct[] selectedProducts = this.repositoryOutputProductListPanel.getProductListPanel().getSelectedProducts();
        Path selectedProductPath = ((LocalRepositoryProduct) selectedProducts[0]).getPath();
        String dialogTitle = "Show local product in explorer";
        if (Files.exists(selectedProductPath)) {
            try {
                Desktop.getDesktop().open(selectedProductPath.toFile());
            } catch (IOException exception) {
                String message = String.format("The local product path '%s' can not be opened in the explorer.", selectedProductPath);
                showMessageDialog(dialogTitle, message, JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // the product path does not exist
            String message = String.format("The local product path '%s' does not exist.", selectedProductPath);
            showMessageDialog(dialogTitle, message, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelSearchingProductList() {
        if (this.searchProductListThread != null) {
            this.searchProductListThread.cancelRunning(); // stop the thread
        }
        if (this.localRepositoryProductsThread != null) {
            this.localRepositoryProductsThread.cancelRunning(); // stop the thread
        }
        this.repositoryOutputProductListPanel.updateProductListCountTitle();
        this.downloadRemoteProductsHelper.cancelDownloadingProductsQuickLookImage();
        this.repositorySelectionPanel.getProgressBarHelper().stopRequested();
    }

    private void setHorizontalSplitPaneLeftComponent(AbstractProductsRepositoryPanel selectedProductsRepositoryPanel) {
        int dividerLocation = this.horizontalSplitPane.getDividerLocation();
        this.horizontalSplitPane.setLeftComponent(selectedProductsRepositoryPanel);
        this.horizontalSplitPane.setDividerLocation(dividerLocation);
        this.horizontalSplitPane.revalidate();
        this.horizontalSplitPane.repaint();
    }

    private void onSelectedNewProductsRepository() {
        cancelSearchingProductList();
        AbstractProductsRepositoryPanel selectedProductsRepositoryPanel = this.repositorySelectionPanel.getSelectedProductsRepositoryPanel();
        setHorizontalSplitPaneLeftComponent(selectedProductsRepositoryPanel);
        selectedProductsRepositoryPanel.addInputParameterComponents();
        boolean refreshed = selectedProductsRepositoryPanel.refreshInputParameterComponentValues();
        if (refreshed) {
            this.repositoryOutputProductListPanel.refreshOutputList();
        } else {
            this.repositoryOutputProductListPanel.clearOutputList(true);
        }
    }

    private void onSelectedNewProductsRepositoryMission() {
        cancelSearchingProductList();
        AbstractProductsRepositoryPanel selectedProductsRepositoryPanel = this.repositorySelectionPanel.getSelectedProductsRepositoryPanel();
        selectedProductsRepositoryPanel.addInputParameterComponents();
        selectedProductsRepositoryPanel.resetInputParameterValues();
        this.repositoryOutputProductListPanel.clearOutputList(true);
    }

    private void cancelDownloadingProducts() {
        this.downloadRemoteProductsHelper.cancelDownloadingProducts(); // stop the thread
        this.repositoryOutputProductListPanel.getProgressBarHelper().hideProgressPanel();
    }

    private Path showDialogToSaveLocalFile(String dialogTitle, FileFilter fileFilter) {
        CustomFileChooser fileChooser = CustomFileChooser.buildFileChooser(dialogTitle, false, JFileChooser.FILES_ONLY, false);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(fileFilter);
        if (this.lastLocalRepositoryFolderPath != null) {
            fileChooser.setCurrentDirectoryPath(this.lastLocalRepositoryFolderPath);
        }
        int result = fileChooser.showDialog(this, "Save");
        if (result == JFileChooser.APPROVE_OPTION) {
            Path selectedFilePath = fileChooser.getSelectedPath();
            if (selectedFilePath == null) {
                throw new NullPointerException("The selected file path is null.");
            }
            saveLastSelectedFolderPath(selectedFilePath.getParent());
            return selectedFilePath;
        }
        return null;
    }

    private Path showDialogToSelectLocalFolder(String dialogTitle, boolean readOnly) {
        CustomFileChooser fileChooser = CustomFileChooser.buildFileChooser(dialogTitle, false, JFileChooser.DIRECTORIES_ONLY, readOnly);
        fileChooser.setAcceptAllFileFilterUsed(false);
        if (this.lastLocalRepositoryFolderPath != null) {
            fileChooser.setCurrentDirectoryPath(this.lastLocalRepositoryFolderPath);
        }
        int result = fileChooser.showDialog(this, "Select");
        if (result == JFileChooser.APPROVE_OPTION) {
            Path selectedFolderPath = fileChooser.getSelectedPath();
            if (selectedFolderPath == null) {
                throw new NullPointerException("The selected folder path is null.");
            }
            saveLastSelectedFolderPath(selectedFolderPath);
            return selectedFolderPath;
        }
        return null;
    }

    private void saveLastSelectedFolderPath(Path selectedFolderPath) {
        // save the folder path into the preferences
        this.lastLocalRepositoryFolderPath = selectedFolderPath;
        this.appContext.getPreferences().setPropertyString(PREFERENCES_KEY_LAST_LOCAL_REPOSITORY_FOLDER_PATH, this.lastLocalRepositoryFolderPath.toString());
    }

    private void openDownloadedRemoteProductsButtonPressed() {
        RepositoryProduct[] unopenedSelectedProducts = processUnopenedSelectedProducts();
        if (unopenedSelectedProducts.length > 0) {
            OutputProductListModel productListModel = this.repositoryOutputProductListPanel.getProductListPanel().getProductListModel();
            Map<RepositoryProduct, Path> productsToOpen = productListModel.addPendingOpenDownloadedProducts(unopenedSelectedProducts);
            if (productsToOpen.size() > 0) {
                OpenDownloadedProductsRunnable runnable = new OpenDownloadedProductsRunnable(this.appContext, this.repositoryOutputProductListPanel, productsToOpen);
                runnable.executeAsync(); // start the thread
            }
        }
    }

    private void newSelectedRepositoryProducts() {
        RepositoryProduct[] selectedProducts = this.repositoryOutputProductListPanel.getProductListPanel().getSelectedProducts();
        int totalPathCount = 0;
        for (RepositoryProduct selectedProduct : selectedProducts) {
            if (selectedProduct.getPolygon() != null) {
                totalPathCount += selectedProduct.getPolygon().getPathCount();
            }
        }
        Path2D.Double[] polygonPaths = new Path2D.Double[totalPathCount];
        for (int i = 0, index = 0; i < selectedProducts.length; i++) {
            if (selectedProducts[i].getPolygon() != null) {
                AbstractGeometry2D productGeometry = selectedProducts[i].getPolygon();
                for (int p = 0; p < productGeometry.getPathCount(); p++) {
                    polygonPaths[index++] = productGeometry.getPathAt(p);
                }
            }
        }
        this.worldWindowPanel.highlightPolygons(polygonPaths);
        if (polygonPaths.length == 1) {
            // the repository product has only one path
            this.worldWindowPanel.setEyePosition(polygonPaths[0]);
        }
    }

    public RepositoryProduct[] getSelectedProducts() {
        return this.repositoryOutputProductListPanel.getProductListPanel().getSelectedProducts();
    }

    private void outputProductsPageChanged() {
        this.downloadRemoteProductsHelper.cancelDownloadingProductsQuickLookImage();
        if (this.localRepositoryProductsThread == null) {
            AbstractProductsRepositoryPanel selectedProductsRepositoryPanel = this.repositorySelectionPanel.getSelectedProductsRepositoryPanel();
            if (selectedProductsRepositoryPanel instanceof RemoteProductsRepositoryPanel) {
                OutputProductListModel productListModel = this.repositoryOutputProductListPanel.getProductListPanel().getProductListModel();
                List<RepositoryProduct> productsWithoutQuickLookImage = productListModel.findProductsWithoutQuickLookImage();
                if (productsWithoutQuickLookImage.size() > 0) {
                    if (logger.isLoggable(Level.FINE)) {
                        int currentPageNumber = selectedProductsRepositoryPanel.getOutputProductResults().getCurrentPageNumber();
                        String repositoryName = selectedProductsRepositoryPanel.getRepositoryName();
                        logger.log(Level.FINE, "Start downloading the quick look images for " + productsWithoutQuickLookImage.size() + " products from page number " + currentPageNumber + " using the '" + repositoryName + "' remote repository.");
                    }

                    RemoteProductsRepositoryPanel remoteProductsRepositoryPanel = (RemoteProductsRepositoryPanel) selectedProductsRepositoryPanel;
                    Credentials selectedCredentials = remoteProductsRepositoryPanel.getSelectedAccount();
                    RemoteProductsRepositoryProvider productsRepositoryProvider = remoteProductsRepositoryPanel.getProductsRepositoryProvider();
                    this.downloadRemoteProductsHelper.downloadProductsQuickLookImageAsync(productsWithoutQuickLookImage, productsRepositoryProvider, selectedCredentials, this.repositoryOutputProductListPanel);
                }
            }
        }
    }

    public void findRelatedSlices() {
        RepositoryProduct[] selectedProducts = getSelectedProducts();
        if (selectedProducts.length > 0) {
            // there are selected products into the output table
            if (selectedProducts.length > 1) {
                throw new IllegalStateException("Only one selected product is allowed.");
            } else {
                Integer dataTakeId = findLocalAttributeAsInt(AbstractMetadata.data_take_id, selectedProducts[0]);
                if (dataTakeId != null && dataTakeId != AbstractMetadata.NO_METADATA) {
                    cancelSearchingProductList();
                    AbstractProductsRepositoryPanel selectedProductsRepositoryPanel = this.repositorySelectionPanel.getSelectedProductsRepositoryPanel();
                    if (selectedProductsRepositoryPanel instanceof AllLocalProductsRepositoryPanel) {
                        // the local repository is selected
                        AllLocalProductsRepositoryPanel allLocalProductsRepositoryPanel = (AllLocalProductsRepositoryPanel) selectedProductsRepositoryPanel;

                        AttributeFilter attributeFilter = new AttributeFilter(AbstractMetadata.data_take_id, dataTakeId.toString(), AttributesParameterComponent.EQUAL_VALUE_FILTER);
                        List<AttributeFilter> attributes = new ArrayList<>(1);
                        attributes.add(attributeFilter);
                        allLocalProductsRepositoryPanel.updateInputParameterValues(null, null, null, null, null, attributes);

                        this.repositoryOutputProductListPanel.clearOutputList(true);

                        searchProductListLater();
                    } else {
                        throw new IllegalStateException("The local products repository is not the selected repository.");
                    }
                }
            }
        }
    }

    private void jointSearchCriteriaOptionClicked() {
        OutputProductListPanel productListPanel = this.repositoryOutputProductListPanel.getProductListPanel();
        RepositoryProduct[] selectedProducts = productListPanel.getSelectedProducts();
        if (selectedProducts.length > 0) {
            // there are selected products into the output table
            try {
                if (selectedProducts.length > 1) {
                    throw new IllegalStateException("Only one selected product is allowed.");
                } else {
                    RemoteMission remoteMission = selectedProducts[0].getRemoteMission();
                    if (remoteMission == null) {
                        throw new NullPointerException("The remote mission is missing.");
                    }
                    LocalDateTime acquisitionDate = selectedProducts[0].getAcquisitionDate();
                    if (acquisitionDate == null) {
                        throw new NullPointerException("The product acquisition date is missing.");
                    }

                    cancelSearchingProductList();

                    RemoteProductsRepositoryPanel selectedProductsRepositoryPanel = this.repositorySelectionPanel.selectRemoteProductsRepositoryPanelByName(remoteMission);
                    if (selectedProductsRepositoryPanel == null) {
                        throw new IllegalStateException("The remote products repository '" + remoteMission.getRepositoryName() + "' is missing.");
                    } else {
                        // the remote products repository exists and it is selected
                        LocalDateTime startDate = acquisitionDate.minusDays(7); // one week ago
                        LocalDateTime endDate = acquisitionDate.plusDays(14);

                        Rectangle2D.Double areaOfInterestToSelect = null;
                        if (selectedProducts[0].getPolygon() != null) {
                            Path2D.Double productAreaPath = selectedProducts[0].getPolygon().getPathAt(0);
                            areaOfInterestToSelect = convertProductAreaPathToRectangle(productAreaPath);
                        }

                        setHorizontalSplitPaneLeftComponent(selectedProductsRepositoryPanel);
                        selectedProductsRepositoryPanel.addInputParameterComponents();
                        selectedProductsRepositoryPanel.updateInputParameterValues(remoteMission.getName(), startDate, endDate, areaOfInterestToSelect);
                        this.repositoryOutputProductListPanel.clearOutputList(true);

                        searchProductListLater();
                    }
                }
            } catch (NullPointerException ex) {
                showMessageDialog("Joint search", "Cannot perform joint search!\nReason: " + ex.getMessage(), JOptionPane.WARNING_MESSAGE);
            } catch (IllegalStateException ex) {
                showMessageDialog("Joint search", "Cannot perform joint search!\nReason: " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onFinishSearchingProducts(Runnable invokerThread) {
        if (invokerThread == ProductLibraryToolViewV2.this.searchProductListThread) {
            this.searchProductListThread = null; // reset
            this.repositoryOutputProductListPanel.updateProductListCountTitle();
        }
    }

    private void downloadSelectedRemoteProductsButtonPressed() {
        OutputProductListPanel productListPanel = this.repositoryOutputProductListPanel.getProductListPanel();
        RepositoryProduct[] selectedProducts = productListPanel.getSelectedProducts();
        if (selectedProducts.length > 0) {
            // there are selected products into the output table
            if (this.localRepositoryProductsThread == null) {
                // there is no running thread for the local repository products
                Path selectedLocalRepositoryFolder = showDialogToSelectLocalFolder("Select folder to download the product", false);
                if (selectedLocalRepositoryFolder != null) {
                    AbstractProductsRepositoryPanel selectedRepository = this.repositorySelectionPanel.getSelectedProductsRepositoryPanel();
                    if (selectedRepository instanceof RemoteProductsRepositoryPanel) {
                        RemoteProductsRepositoryPanel remoteProductsRepositoryPanel = (RemoteProductsRepositoryPanel) selectedRepository;
                        Credentials selectedCredentials = remoteProductsRepositoryPanel.getSelectedAccount();
                        RemoteProductsRepositoryProvider remoteProductsRepositoryProvider = remoteProductsRepositoryPanel.getProductsRepositoryProvider();
                        AllLocalFolderProductsRepository allLocalFolderProductsRepository = this.repositorySelectionPanel.getAllLocalProductsRepositoryPanel().getAllLocalFolderProductsRepository();
                        this.downloadRemoteProductsHelper.downloadProductsAsync(selectedProducts, remoteProductsRepositoryProvider, selectedLocalRepositoryFolder,
                                selectedCredentials, allLocalFolderProductsRepository);
                        // refresh the products in the output panel
                        productListPanel.getProductListModel().refreshProducts();
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
                showMessageDialog("Download products", message.toString(), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static Rectangle2D.Double convertProductAreaPathToRectangle(Path2D.Double productAreaPath) {
        double[] coordinates = new double[2];

        PathIterator pathIterator = productAreaPath.getPathIterator(null);

        pathIterator.currentSegment(coordinates);
        double x1 = coordinates[0];
        double x2 = coordinates[0];
        double y1 = coordinates[1];
        double y2 = coordinates[1];
        pathIterator.next();

        while (!pathIterator.isDone()) {
            pathIterator.currentSegment(coordinates);
            x1 = min(x1, coordinates[0]);
            x2 = max(x2, coordinates[0]);
            y1 = min(y1, coordinates[1]);
            y2 = max(y2, coordinates[1]);
            pathIterator.next();
        }
        return new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);
    }

    private static void writeErrors(final Map<File, String> errorFiles, final File file) throws Exception {
        PrintStream p = null; // declare a print stream object
        try {
            final FileOutputStream out = new FileOutputStream(file.getAbsolutePath());
            // Connect print stream to the output stream
            p = new PrintStream(out);
            for (Map.Entry<File, String> entry : errorFiles.entrySet()) {
                p.println(entry.getValue() + "   " + entry.getKey().getAbsolutePath());
            }
        } finally {
            if (p != null) {
                p.close();
            }
        }
    }

    private void searchButtonPressed() {
        cancelSearchingProductList();
        ThreadListener threadListener = this::onFinishSearchingProducts;
        ProgressBarHelperImpl progressBarHelper = this.repositorySelectionPanel.getProgressBarHelper();
        int threadId = progressBarHelper.incrementAndGetCurrentThreadId();
        AbstractProductsRepositoryPanel selectedProductsRepositoryPanel = this.repositorySelectionPanel.getSelectedProductsRepositoryPanel();
        RemoteRepositoriesSemaphore remoteRepositoriesSemaphore = this.downloadRemoteProductsHelper.getRemoteRepositoriesSemaphore();
        AbstractProgressTimerRunnable<?> thread = selectedProductsRepositoryPanel.buildSearchProductListThread(progressBarHelper, threadId, threadListener,
                remoteRepositoriesSemaphore, this.repositoryOutputProductListPanel);
        if (thread != null) {
            this.repositoryOutputProductListPanel.clearOutputList(true);
            this.searchProductListThread = thread;
            this.searchProductListThread.executeAsync(); // start the thread
        }
    }
}
