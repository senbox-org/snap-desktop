package org.esa.snap.rcp;

import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Version;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.formdev.flatlaf.FlatLightLaf;
import eu.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.util.PreferencesPropertyMap;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.actions.file.OpenProductAction;
import org.esa.snap.rcp.actions.file.SaveProductAction;
import org.esa.snap.rcp.cli.SnapArgs;
import org.esa.snap.rcp.session.OpenSessionAction;
import org.esa.snap.rcp.util.ContextGlobalExtenderImpl;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.rcp.util.internal.DefaultSelectionSupport;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.NotificationDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.awt.ToolbarPool;
import org.openide.awt.UndoRedo;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;
import org.openide.modules.OnStart;
import org.openide.modules.OnStop;
import org.openide.util.*;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.OnShowing;
import org.openide.windows.WindowManager;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * The class {@code SnapApp} is a facade for SNAP Desktop applications. There is only a single instance of
 * a SNAP application which is retrieved by
 * <pre>
 *     SnapApp app = SnapApp.getDefault();
 * </pre>
 * {@code SnapApp} is the main entry point for most SNAP Desktop extensions. An extension might want to be informed
 * about selection changes in the application. Here are some examples:
 * <pre>
 *     app.getSelectionSupport(Product.class).addHandler(myProductSelectionHandler);
 *     app.getSelectionSupport(ProductNode.class).addHandler(myProductNodeSelectionHandler);
 *     app.getSelectionSupport(RasterDataNode.class).addHandler(myRasterDataNodeSelectionHandler);
 *     app.getSelectionSupport(ProductSceneView.class).addHandler(myViewSelectionHandler);
 * </pre>
 * Or might want to retrieve the currently selected objects:
 * <pre>
 *     Product product = app.getSelectedProduct();
 *     ProductNode productNode = getSelectedProductNode();
 *     ProductSceneView view = app.getSelectedProductSceneView();
 *     // For any other type of selected object, use:
 *     Figure figure = Utilities.actionsGlobalContext().lookup(Figure.class);
 * </pre>
 * <p>
 * If you want to alter the behaviour of the default implementation of the SNAP Desktop application,
 * then register your derived class as a service using
 * <pre>
 *     &#64;ServiceProvider(service = MoonApp.class, supersedes = "SnapApp")
 *     public class MoonApp extends SnapApp {
 *         ...
 *     }
 * </pre>
 *
 * @author Norman Fomferra
 * @see SelectionSupport
 * @see org.esa.snap.rcp.util.SelectionSupport.Handler
 * @since 2.0
 */
@ServiceProvider(service = SnapApp.class)
@SuppressWarnings("UnusedDeclaration")
public class SnapApp {

    private final static Logger LOG = Logger.getLogger(SnapApp.class.getName());

    private final ProductManager productManager;
    private final Map<Class<?>, SelectionSupport<?>> selectionChangeSupports;
    private Engine engine;

    /**
     * Constructor.
     * <p>
     * As this class is a registered service, the constructor is not supposed to be called directly.
     */
    public SnapApp() {
        productManager = new ProductManager();
        // Register a provider that delivers an UndoManager for a Product instance.
        UndoManagerProvider undoManagerProvider = new UndoManagerProvider();
        ExtensionManager.getInstance().register(Product.class, undoManagerProvider);
        productManager.addListener(undoManagerProvider);
        productManager.addListener(new MultiSizeWarningListener());
        selectionChangeSupports = new HashMap<>();
    }

    /**
     * Gets the SNAP application singleton which provides access to various SNAP APIs and resources.
     * <p>
     * The the method basically returns
     * <pre>
     *    Lookup.getDefault().lookup(SnapApp.class)
     * </pre>
     *
     * @return The SNAP applications global singleton instance.
     */
    public static SnapApp getDefault() {
        SnapApp instance = Lookup.getDefault().lookup(SnapApp.class);
        if (instance == null) {
            instance = new SnapApp();
        }
        return instance;
    }

    /**
     * The method changes the default look and feel to FlatLaf Light on Windows, if the user has not changed the look and feel before.
     * Because of two reasons. First, it looks nicer, and second but more important, it fixes a bug in the Windows look and feel.
     * The bug is that icons disappear in the Analysis menu when one of the items clicked. It occurs in SNAP 10 with JDK11 and Netbeans 11.3.
     */
    private static void initialiseLookAndFeel() {
        if (Utilities.isWindows()) {
            Preferences lafPreference = NbPreferences.root().node("laf");
            if (lafPreference == null || lafPreference.get("laf", null) == null) {
                try {
                    UIManager.setLookAndFeel(FlatLightLaf.class.getName());
                    lafPreference.put("laf", FlatLightLaf.class.getName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                         UnsupportedLookAndFeelException e) {
                    LOG.warning("Could not set FlatLaf Light Look and Feel");
                }
            }
        }
    }

    private static void initImageIO() {
        // todo - actually this should be done in the activator of ceres-jai which does not exist yet
        Lookup.Result<ModuleInfo> moduleInfos = Lookup.getDefault().lookupResult(ModuleInfo.class);
        String ceresJaiCodeName = "org.esa.snap.ceres.jai";
        Optional<? extends ModuleInfo> info = moduleInfos.allInstances().stream().filter(
                moduleInfo -> ceresJaiCodeName.equals(moduleInfo.getCodeName())).findFirst();

        if (info.isPresent()) {
            ClassLoader classLoader = info.get().getClassLoader();
            IIORegistry iioRegistry = IIORegistry.getDefaultInstance();
            iioRegistry.registerServiceProviders(IIORegistry.lookupProviders(ImageReaderSpi.class, classLoader));
            iioRegistry.registerServiceProviders(IIORegistry.lookupProviders(ImageWriterSpi.class, classLoader));
        } else {
            LOG.warning(String.format("Module '%s' not found. Not able to load image-IO services.", ceresJaiCodeName));
        }
    }

    private static String getOperatorName(Operator operator) {
        String operatorName = operator.getSpi().getOperatorDescriptor().getAlias();
        if (operatorName == null) {
            operatorName = operator.getSpi().getOperatorDescriptor().getName();
        }
        return operatorName;
    }

    private static <T extends ProductNode> T getProductNode(T explorerNode, T viewNode, ProductSceneView sceneView, SelectionSourceHint hint) {
        switch (hint) {
            case VIEW:
                if (viewNode != null) {
                    return viewNode;
                } else {
                    return explorerNode;
                }
            case EXPLORER:
                if (explorerNode != null) {
                    return explorerNode;
                } else {
                    return viewNode;
                }
            case AUTO:
            default:
                if (sceneView != null && sceneView.hasFocus()) {
                    return viewNode;
                }
                return explorerNode;
        }
    }

    /**
     * Gets SNAP's global document window manager. Use it to open your own document windows, or register a listener to
     * be notified on window events such as opening, closing, selection, deselection.
     *
     * @return SNAP's global document window manager.
     */
    public DocumentWindowManager getDocumentWindowManager() {
        return DocumentWindowManager.getDefault();
    }

    /**
     * Gets SNAP's global data product manager. Use it to add your own data product instances, or register a listener to
     * be notified on product addition and removal events.
     *
     * @return SNAP's global product manager.
     */
    public ProductManager getProductManager() {
        return productManager;
    }

    /**
     * @return SNAP's global undo / redo manager.
     */
    public UndoRedo.Manager getUndoManager(Product product) {
        return product.getExtension(UndoRedo.Manager.class);
    }

    /**
     * @return SNAP's main frame window.
     */
    public Frame getMainFrame() {
        return WindowManager.getDefault().getMainWindow();
    }

    /**
     * Sets the current status bar message.
     *
     * @param message The new status bar message.
     */
    public void setStatusBarMessage(String message) {
        StatusDisplayer.getDefault().setStatusText(message);
    }

    /**
     * @return The SNAP application's name. The default is {@code "SNAP"}.
     */
    public String getInstanceName() {
        try {
            final ModuleInfo moduleInfo = Modules.getDefault().ownerOf(SnapApp.class);
            Version specVersion = Version.parseVersion(moduleInfo.getImplementationVersion() );
//            return NbBundle.getBundle("org.netbeans.core.ui.Bundle").getString("LBL_ProductInformation") + " " + specVersion.getMajor();
            return NbBundle.getBundle("org.netbeans.core.ui.Bundle").getString("LBL_ProductInformation");
        } catch (Exception e) {
            return SystemUtils.getApplicationName();
        }
    }

    /**
     * @return The user's application preferences.
     */
    public Preferences getPreferences() {
        return NbPreferences.forModule(getClass());
    }

    /**
     * @return The SNAP logger.
     */
    public Logger getLogger() {
        return LOG;
    }

    /**
     * Handles an error.
     *
     * @param message An error message.
     * @param t       An exception or {@code null}.
     */
    public void handleError(String message, Throwable t) {
        if (t != null) {
            t.printStackTrace();
        }
        Dialogs.showError("Error", message);
        getLogger().log(Level.SEVERE, message, t);

        ImageIcon icon = TangoIcons.status_dialog_error(TangoIcons.Res.R16);
        JLabel balloonDetails = new JLabel(message);
        JButton popupDetails = new JButton("Report");
        popupDetails.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("http://forum.step.esa.int/"));
            } catch (URISyntaxException | IOException e1) {
                getLogger().log(Level.SEVERE, message, e1);
            }
        });
        NotificationDisplayer.getDefault().notify("Error",
                icon,
                balloonDetails,
                popupDetails,
                NotificationDisplayer.Priority.HIGH,
                NotificationDisplayer.Category.ERROR);
    }

    /**
     * Provides a {@link SelectionSupport} instance for object selections.
     *
     * @param type The type of selected objects whose selection state to observe.
     * @return A selection support instance for the given object type, or {@code null}.
     */
    public <T> SelectionSupport<T> getSelectionSupport(Class<T> type) {
        @SuppressWarnings("unchecked")
        DefaultSelectionSupport<T> selectionChangeSupport = (DefaultSelectionSupport<T>) selectionChangeSupports.get(type);
        if (selectionChangeSupport == null) {
            selectionChangeSupport = new DefaultSelectionSupport<>(type);
            selectionChangeSupports.put(type, selectionChangeSupport);
        }
        return selectionChangeSupport;
    }

    /**
     * @return The currently selected product scene view, or {@code null}.
     */
    public ProductSceneView getSelectedProductSceneView() {
        return Utilities.actionsGlobalContext().lookup(ProductSceneView.class);
    }

    /**
     * Return the currently selected product node.
     * <p>
     * The {@link SelectionSourceHint hint} defines what is the primary and secondary selection source. Source is either the
     * {@link SelectionSourceHint#VIEW scene view} or the {@link SelectionSourceHint#EXPLORER product explorer}. If it is set to
     * {@link SelectionSourceHint#AUTO} the algorithm tries to make a good guess, checking which component has the focus.
     *
     * @return The currently selected product node, or {@code null}.
     */
    public ProductNode getSelectedProductNode(SelectionSourceHint hint) {
        ProductNode viewNode = null;
        ProductSceneView productSceneView = getSelectedProductSceneView();
        if (productSceneView != null) {
            viewNode = productSceneView.getProduct();
        }
        ProductNode explorerNode = Utilities.actionsGlobalContext().lookup(ProductNode.class);

        return getProductNode(explorerNode, viewNode, productSceneView, hint);
    }

    /**
     * Return the currently selected product.
     * <p>
     * The {@link SelectionSourceHint hint} defines what is the primary and secondary selection source. Source is either the
     * {@link SelectionSourceHint#VIEW scene view} or the {@link SelectionSourceHint#EXPLORER product explorer}. If it is set to
     * {@link SelectionSourceHint#AUTO} the algorithm tries to make a good guess, checking which component has the focus.
     *
     * @param hint gives a hint to the implementation which selection source should be preferred.
     * @return The currently selected product or {@code null}.
     */
    public Product getSelectedProduct(SelectionSourceHint hint) {
        Product viewProduct = null;
        ProductSceneView productSceneView = getSelectedProductSceneView();
        if (productSceneView != null) {
            viewProduct = productSceneView.getProduct();
        }
        Product explorerProduct = null;
        ProductNode productNode = Utilities.actionsGlobalContext().lookup(ProductNode.class);
        if (productNode != null) {
            explorerProduct = productNode.getProduct();
        }
        return getProductNode(explorerProduct, viewProduct, productSceneView, hint);
    }

    /**
     * Gets an {@link AppContext} representation of the SNAP application.
     * <p>
     * Its main use is to provide compatibility for SNAP heritage GUI code (from BEAM & NEST) which used
     * the {@link AppContext} interface.
     *
     * @return An {@link AppContext} representation of this {@code SnapApp}.
     */
    public AppContext getAppContext() {
        return new SnapContext();
    }

    /**
     * Called if SNAP starts up. The method is not supposed to be called by clients directly.
     * <p>
     * Overrides should call {@code super.onStart()} as a first step unless they know what they are doing.
     */
    public void onStart() {
        engine = Engine.start(false);

        initialiseLookAndFeel();

        String toolbarConfig = "Standard";
        if (Config.instance().debug()) {
            WindowManager.getDefault().setRole("developer");
            toolbarConfig = "Developer";
        }
        // See src/main/resources/org/esa/snap/rcp/layer.xml
        // See src/main/resources/org/esa/snap/rcp/toolbars/Standard.xml
        // See src/main/resources/org/esa/snap/rcp/toolbars/Developer.xml
        ToolbarPool.getDefault().setConfiguration(toolbarConfig);
    }

    /**
     * Called if SNAP shuts down. The method is not supposed to be called by clients directly.
     * <p>
     * Overrides should call {@code super.onStop()} in a final step unless they know what they are doing.
     */
    public void onStop() {
        engine.stop();
        disposeProducts();
        try {
            getPreferences().flush();
        } catch (BackingStoreException e) {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Called if SNAP is showing on the user's desktop. The method is not supposed to be called by clients directly.
     * <p>
     * Overrides should call {@code super.onShowing()} as a first step unless they know whet they are doing.
     */
    public void onShowing() {
        getMainFrame().setTitle(getEmptyTitle());
        getSelectionSupport(ProductSceneView.class).addHandler(new SceneViewListener());
        getSelectionSupport(ProductNode.class).addHandler(new ProductNodeListener());
        NodeNameListener nodeNameListener = new NodeNameListener();
        getProductManager().addListener(new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {
                event.getProduct().addProductNodeListener(nodeNameListener);
            }

            @Override
            public void productRemoved(ProductManager.Event event) {
                event.getProduct().removeProductNodeListener(nodeNameListener);
            }
        });
        if (SnapArgs.getDefault().getSessionFile() != null) {
            File sessionFile = SnapArgs.getDefault().getSessionFile().toFile();
            if (sessionFile != null) {
                new OpenSessionAction().openSession(sessionFile);
            }
        }
        List<Path> fileList = SnapArgs.getDefault().getFileList();
        if (!fileList.isEmpty()) {
            OpenProductAction productAction = new OpenProductAction();
            File[] files = fileList.stream().map(Path::toFile).filter(Objects::nonNull).toArray(File[]::new);
            productAction.setFiles(files);
            productAction.execute();
        }
    }

    /**
     * Called if SNAP is about to shut down. The method is not supposed to be called by clients directly.
     * <p>
     * Overrides should call {@code super.onTryStop()()} unless they know whet they are doing. The method should return
     * immediately {@code false} if the super call returns {@code false}.
     *
     * @return {@code false} if the shutdown process shall be cancelled immediately. {@code true}, if it is ok
     * to continue shut down.
     */
    public boolean onTryStop() {

        final ArrayList<Product> modifiedProducts = new ArrayList<>(5);
        final Product[] products = getProductManager().getProducts();
        for (final Product product : products) {
            final ProductReader reader = product.getProductReader();
            if (reader != null) {
                final Object input = reader.getInput();
                if (input instanceof Product) {
                    modifiedProducts.add(product);
                }
            }
            if (!modifiedProducts.contains(product) && product.isModified()) {
                modifiedProducts.add(product);
            }
        }
        if (!modifiedProducts.isEmpty()) {
            final StringBuilder message = new StringBuilder();
            if (modifiedProducts.size() == 1) {
                message.append("The following product has been modified:");
                message.append("\n    ").append(modifiedProducts.get(0).getDisplayName());
                message.append("\n\nDo you wish to save it?");
            } else {
                message.append("The following products have been modified:");
                for (Product modifiedProduct : modifiedProducts) {
                    message.append("\n    ").append(modifiedProduct.getDisplayName());
                }
                message.append("\n\nDo you want to save them?");
            }
            Dialogs.Answer answer = Dialogs.requestDecision("Exit", message.toString(), true, null);
            // decision request cancelled --> cancel SNAP shutdown
            if (answer == Dialogs.Answer.YES) {
                //Save Products in reverse order is necessary because derived products must be saved first
                Collections.reverse(modifiedProducts);
                for (Product modifiedProduct : modifiedProducts) {
                    Boolean saveStatus = new SaveProductAction(modifiedProduct).execute();
                    if (saveStatus == null) {
                        // save cancelled --> cancel SNAP shutdown
                        return false;
                    }
                }
            } else return answer != Dialogs.Answer.CANCELLED;
        }

        return true;
    }

    private void initGPF() {
        GPF gpf = GPF.getDefaultInstance();
        OperatorSpiRegistry operatorSpiRegistry = gpf.getOperatorSpiRegistry();
        Set<OperatorSpi> services = operatorSpiRegistry.getServiceRegistry().getServices();
        for (OperatorSpi service : services) {
            LOG.info(String.format("GPF operator SPI: %s (alias '%s')", service.getClass(), service.getOperatorAlias()));
        }
        gpf.setProductManager(getProductManager());
        gpf.setProgressMonitoredOperatorExecutor(operator -> {
            SnapAppGPFOperatorExecutor snapAppGPFOperatorExecutor = new SnapAppGPFOperatorExecutor(operator);
            snapAppGPFOperatorExecutor.executeWithBlocking();
        });
    }

    private void updateMainFrameTitle(ProductSceneView sceneView) {
        String title;
        if (sceneView != null) {
            Product product = sceneView.getProduct();
            if (product != null) {
                title = String.format("%s - %s - %s", sceneView.getSceneName(), product.getName(), getProductPath(product));
            } else {
                title = sceneView.getSceneName();
            }
            title = appendTitleSuffix(title);
        } else {
            title = getEmptyTitle();
        }
        getMainFrame().setTitle(title);
    }

    private void updateMainFrameTitle(ProductNode node) {
        String title;
        if (node != null) {
            Product product = node.getProduct();
            if (product != null) {
                if (node == product) {
                    title = String.format("%s - [%s]", product.getDisplayName(), getProductPath(product));
                } else {
                    title = String.format("%s - [%s] - [%s]", node.getDisplayName(), product.getName(), getProductPath(product));
                }
            } else {
                title = node.getDisplayName();
            }
            title = appendTitleSuffix(title);
        } else {
            title = getEmptyTitle();
        }
        getMainFrame().setTitle(title);
    }

    private String getProductPath(Product product) {
        File fileLocation = product.getFileLocation();
        if (fileLocation != null) {
            try {
                return fileLocation.getCanonicalPath();
            } catch (IOException e) {
                return fileLocation.getAbsolutePath();
            }
        } else {
            return "not saved";
        }
    }

    private void disposeProducts() {
        Product[] products = getProductManager().getProducts();
        getProductManager().removeAllProducts();
        for (Product product : products) {
            product.dispose();
        }
    }

    private String appendTitleSuffix(String title) {
        String appendix = !Utilities.isMac() ? String.format(" - %s", getInstanceName()) : "";
        return title + appendix;
    }

    private String getEmptyTitle() {
        String instanceName = getInstanceName();

        if (instanceName != null && instanceName.length() > 0) {
            if (SystemUtils.isMainframeTitleIncludeVersion()) {
                String version = SystemUtils.getReleaseVersion();
                if (version != null && version.length() > 0) {
                    return String.format("%s %s", instanceName, version);
                }
            }
            return String.format("%s", instanceName);
        }
            return String.format("[%s]", "Empty");
        }


    /**
     * Provides a hint to {@link SnapApp#getSelectedProduct(SelectionSourceHint)} } which selection provider should be used as primary selection source
     */
    public enum SelectionSourceHint {
        /**
         * The scene view shall be preferred as selection source.
         */
        VIEW,
        /**
         * The product explorer shall be preferred as selection source.
         */
        EXPLORER,
        /**
         * The primary selection source is automatically detected.
         */
        AUTO,
    }

    /**
     * This non-API class is public as an implementation detail. Don't use it, it may be removed anytime.
     * <p>
     * NetBeans {@code @OnStart}: {@code Runnable}s defined by various modules are invoked in parallel and as soon
     * as possible. It is guaranteed that execution of all {@code runnable}s is finished
     * before the startup sequence is claimed over.
     */
    @OnStart
    public static class StartOp implements Runnable {

        @Override
        public void run() {
            LOG.info("Starting SNAP Desktop");
            try {
                SnapApp.getDefault().onStart();
            } finally {
                initImageIO();
                SystemUtils.init3rdPartyLibsByCl(Lookup.getDefault().lookup(ClassLoader.class));
                SnapApp.getDefault().initGPF();
            }
        }
    }

    /**
     * This non-API class is public as an implementation detail. Don't use it, it may be removed anytime.
     * <p>
     * NetBeans {@code @OnShowing}: Annotation to place on a {@code Runnable} with default constructor which should be invoked as soon as the window
     * system is shown. The {@code Runnable}s are invoked in AWT event dispatch thread one by one
     */
    @OnShowing
    public static class ShowingOp implements Runnable {

        @Override
        public void run() {
            LOG.info("Showing SNAP Desktop");
            SnapApp.getDefault().onShowing();
        }
    }

    /**
     * This non-API class is public as an implementation detail. Don't use it, it may be removed anytime.
     * <p>
     * NetBeans {@code @OnStop}: Annotation that can be applied to {@code Runnable} or {@code Callable<Boolean>}
     * subclasses with default constructor which will be invoked during shutdown sequence or when the
     * module is being shutdown.
     * <p>
     * First of all call {@code Callable}s are consulted to allow or deny proceeding with the shutdown.
     * <p>
     * If the shutdown is approved, all {@code Runnable}s registered are acknowledged and can perform the shutdown
     * cleanup. The {@code Runnable}s are invoked in parallel. It is guaranteed their execution is finished before
     * the shutdown sequence is over.
     */
    @OnStop
    public static class MaybeStopOp implements Callable {

        @Override
        public Boolean call() {
            LOG.info("Request to stop SNAP Desktop");
            return SnapApp.getDefault().onTryStop();
        }
    }

    /**
     * This non-API class is public as an implementation detail. Don't use it, it may be removed anytime.
     */
    @OnStop
    public static class StopOp implements Runnable {

        @Override
        public void run() {
            LOG.info("Stopping SNAP Desktop");
            SnapApp.getDefault().onStop();
        }
    }

    /**
     * This non-API class is public as an implementation detail. Don't use it, it may be removed anytime.
     * <p>
     * This class proxies the original ContextGlobalProvider and ensures that a set
     * of additional objects remain in the GlobalContext regardless of the TopComponent
     * selection.
     *
     * @see org.esa.snap.rcp.util.ContextGlobalExtenderImpl
     */
    @ServiceProvider(
            service = ContextGlobalProvider.class,
            supersedes = "org.netbeans.modules.openide.windows.GlobalActionContextImpl"
    )
    public static class ActionContextExtender extends ContextGlobalExtenderImpl {

    }

    /**
     * Associates objects with an undo manager.
     */
    private static class UndoManagerProvider implements ExtensionFactory, ProductManager.Listener {

        private final Map<Object, UndoRedo.Manager> undoManagers = new HashMap<>();

        @Override
        public Class<?>[] getExtensionTypes() {
            return new Class<?>[]{UndoRedo.Manager.class};
        }

        @Override
        public Object getExtension(Object object, Class<?> extensionType) {
            return undoManagers.get(object);
        }

        @Override
        public void productAdded(ProductManager.Event event) {
            undoManagers.put(event.getProduct(), new UndoRedo.Manager());
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            UndoRedo.Manager manager = undoManagers.remove(event.getProduct());
            if (manager != null) {
                manager.die();
            }
        }
    }

    private static class SnapContext implements AppContext {

        @Override
        public ProductManager getProductManager() {
            return getDefault().getProductManager();
        }

        @Override
        public Product getSelectedProduct() {
            return getDefault().getSelectedProduct(SelectionSourceHint.AUTO);
        }

        @Override
        public Window getApplicationWindow() {
            return getDefault().getMainFrame();
        }

        @Override
        public String getApplicationName() {
            return getDefault().getInstanceName();
        }

        @Override
        public void handleError(String message, Throwable t) {
            getDefault().handleError(message, t);
        }

        @Override
        @Deprecated
        public PropertyMap getPreferences() {
            final Preferences preferences = getDefault().getPreferences();
            return new PreferencesPropertyMap(preferences);
        }

        @Override
        public ProductSceneView getSelectedProductSceneView() {
            return getDefault().getSelectedProductSceneView();
        }
    }

    private static class MultiSizeWarningListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            final Product product = event.getProduct();
        }

        @Override
        public void productRemoved(ProductManager.Event event) {

        }
    }

    private class SceneViewListener implements SelectionSupport.Handler<ProductSceneView> {

        @Override
        public void selectionChange(ProductSceneView oldValue, ProductSceneView newValue) {
            updateMainFrameTitle(newValue);
        }
    }

    private class ProductNodeListener implements SelectionSupport.Handler<ProductNode> {

        @Override
        public void selectionChange(ProductNode oldValue, ProductNode newValue) {
            updateMainFrameTitle(newValue);
        }
    }

    private class NodeNameListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (ProductNode.PROPERTY_NAME_NAME.equals(event.getPropertyName())) {
                updateMainFrameTitle(event.getSourceNode());
            }
        }
    }

    private class SnapAppGPFOperatorExecutor extends ProgressMonitorSwingWorker<Void, Void> {

        private final Operator operator;

        private SnapAppGPFOperatorExecutor(Operator operator) {
            super(SnapApp.getDefault().getMainFrame(), "Executing " + getOperatorName(operator));
            this.operator = operator;
        }

        @Override
        protected Void doInBackground(ProgressMonitor pm) throws Exception {
            operator.execute(pm);
            return null;
        }
    }
}
