package org.esa.snap.rcp;

import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.core.ExtensionManager;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductManager;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.datamodel.ProductNodeEvent;
import org.esa.snap.framework.datamodel.ProductNodeListener;
import org.esa.snap.framework.gpf.GPF;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.OperatorSpiRegistry;
import org.esa.snap.framework.ui.AppContext;
import org.esa.snap.framework.ui.product.ProductSceneView;
import org.esa.snap.rcp.actions.file.OpenProductAction;
import org.esa.snap.rcp.actions.file.SaveProductAction;
import org.esa.snap.rcp.cli.SnapArgs;
import org.esa.snap.rcp.session.OpenSessionAction;
import org.esa.snap.rcp.util.ContextGlobalExtenderImpl;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.rcp.util.internal.DefaultSelectionSupport;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.util.PreferencesPropertyMap;
import org.esa.snap.util.PropertyMap;
import org.esa.snap.util.SystemUtils;
import org.openide.awt.NotificationDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.awt.ToolbarPool;
import org.openide.awt.UndoRedo;
import org.openide.modules.ModuleInfo;
import org.openide.modules.OnStart;
import org.openide.modules.OnStop;
import org.openide.util.ContextGlobalProvider;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.OnShowing;
import org.openide.windows.WindowManager;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * The central SNAP application class.
 * <p>
 * If you want to provide alter behaviour of this class, register your derived class as a service using
 * <pre>
 *     &#64;ServiceProvider(service = MoonApp.class, supersedes = "SnapApp")
 *     public class MoonApp extends SnapApp {
 *         ...
 *     }
 * </pre>
 *
 * @author Norman Fomferra
 * @since 2.0
 */
@ServiceProvider(service = SnapApp.class)
@SuppressWarnings("UnusedDeclaration")
public class SnapApp {

    private final static Logger LOG = Logger.getLogger(SnapApp.class.getName());

    private final ProductManager productManager;
    private Map<Class<?>, SelectionSupport<?>> selectionChangeSupports;
    private Engine engine;

    /**
     * Gets the SNAP application singleton which provides access to various SNAP APIs and resources.
     * <p>
     * The the method basically returns
     * <pre>
     *    Lookup.getDefault().lookup(SnapApp.class)
     * </pre>
     * @return The SNAP applications global singleton instance.
     */
    public static SnapApp getDefault() {
        SnapApp instance = Lookup.getDefault().lookup(SnapApp.class);
        if (instance == null) {
            instance = new SnapApp();
        }
        return instance;
    }

    public SnapApp() {
        productManager = new ProductManager();
        // Register a provider that delivers an UndoManager for a Product instance.
        UndoManagerProvider undoManagerProvider = new UndoManagerProvider();
        ExtensionManager.getInstance().register(Product.class, undoManagerProvider);
        productManager.addListener(undoManagerProvider);
        selectionChangeSupports = new HashMap<>();
    }

    /**
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
     * @return The (display) name of this application.
     * @deprecated use {@link #getInstanceName()}
     */
    @Deprecated
    public String getAppName() {
        return getInstanceName();
    }

    public String getInstanceName() {
        try {
            return NbBundle.getBundle("org.netbeans.core.ui.Bundle").getString("LBL_ProductInformation");
        } catch (Exception e) {
            return "SNAP";
        }
    }

    /**
     * @return The user's application preferences.
     */
    public Preferences getPreferences() {
        return NbPreferences.forModule(getClass());
    }

    /**
     * Gets the {@link #getPreferences() preferences} wrapped by a {@link PropertyMap}.
     * Using a {@link PropertyMap} for configuration of components is preferred over
     * using Java {@link Preferences} because of easier unit-testing.
     *
     * @return The user's application preferences as {@link PropertyMap} instance.
     */
    public PropertyMap getPreferencesPropertyMap() {
        return new PreferencesPropertyMap(getPreferences());
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
        SnapDialogs.showError("Error", message);
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
     * @return The currently selected product node, or {@code null}.
     */
    public ProductNode getSelectedProductNode() {
        return Utilities.actionsGlobalContext().lookup(ProductNode.class);
    }

    /**
     * @return The currently selected product or {@code null}.
     */
    public Product getSelectedProduct() {
        ProductSceneView productSceneView = getSelectedProductSceneView();
        if (productSceneView != null) {
            return productSceneView.getProduct();
        }
        ProductNode productNode = Utilities.actionsGlobalContext().lookup(ProductNode.class);
        if (productNode != null) {
            return productNode.getProduct();
        }
        return null;
    }

    private String getMainFrameTitle() {

        ProductNode selectedProductNode = getSelectedProductNode();
        Product selectedProduct = null;
        if (selectedProductNode != null) {
            selectedProduct = selectedProductNode.getProduct();
            if (selectedProduct == null) {
                selectedProduct = getSelectedProduct();
            }
        }

        String title;
        if (selectedProduct == null) {
            if (Utilities.isMac()) {
                title = String.format("[%s]", "Empty");
            } else {
                title = String.format("%s", getInstanceName());
            }
        } else if (selectedProduct == selectedProductNode) {
            File fileLocation = selectedProduct.getFileLocation();
            String path = fileLocation != null ? fileLocation.getPath() : "not saved";
            if (Utilities.isMac()) {
                title = String.format("%s - [%s]",
                                      selectedProduct.getName(), path);
            } else {
                title = String.format("%s - [%s] - %s",
                                      selectedProduct.getName(), path, getInstanceName());
            }
        } else {
            File fileLocation = selectedProduct.getFileLocation();
            String path = fileLocation != null ? fileLocation.getPath() : "not saved";
            if (Utilities.isMac()) {
                title = String.format("%s - [%s] - [%s]",
                                      selectedProduct.getName(), path, selectedProductNode.getName());
            } else {
                title = String.format("%s - [%s] - [%s] - %s",
                                      selectedProduct.getName(), path, selectedProductNode.getName(), getInstanceName());
            }
        }

        return title;
    }

    private void updateMainFrameTitle() {
        getMainFrame().setTitle(getMainFrameTitle());
    }

    public void onStart() {
        engine = Engine.start(false);

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

    public void onStop() {
        engine.stop();
        try {
            getPreferences().flush();
        } catch (BackingStoreException e) {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void onShowing() {
        updateMainFrameTitle();
        MainFrameTitleUpdater updater = new MainFrameTitleUpdater();
        getSelectionSupport(ProductNode.class).addHandler(updater);
        getProductManager().addListener(new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {
                event.getProduct().addProductNodeListener(updater);
            }

            @Override
            public void productRemoved(ProductManager.Event event) {
                event.getProduct().removeProductNodeListener(updater);
            }
        });
        if (SnapArgs.getDefault().getSessionFile() != null) {
            File sessionFile = SnapArgs.getDefault().getSessionFile().toFile();
            if (sessionFile!= null) {
                new OpenSessionAction().openSession(sessionFile);
            }
        }
        List<Path> fileList = SnapArgs.getDefault().getFileList();
        if (!fileList.isEmpty()) {
            OpenProductAction productAction = new OpenProductAction();
            File[] files = fileList.stream().map(Path::toFile).filter(file -> file != null).toArray(File[]::new);
            productAction.setFiles(files);
            productAction.execute();
        }
    }

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
            SnapDialogs.Answer answer = SnapDialogs.requestDecision("Exit", message.toString(), true, null);
            if (answer == SnapDialogs.Answer.YES) {
                //Save Products in reverse order is necessary because derived products must be saved first
                Collections.reverse(modifiedProducts);
                for (Product modifiedProduct : modifiedProducts) {
                    Boolean saveStatus = new SaveProductAction(modifiedProduct).execute();
                    if (saveStatus == null) {
                        // save cancelled --> cancel SNAP shutdown
                        return false;
                    }
                }
            } else if (answer == SnapDialogs.Answer.CANCELLED) {
                // decision request cancelled --> cancel SNAP shutdown
                return false;
            }
        }

        return true;
    }

    /**
     * {@code @OnStart}: {@code Runnable}s defined by various modules are invoked in parallel and as soon
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
                SystemUtils.initGeoTools();
                SystemUtils.initJAI(Lookup.getDefault().lookup(ClassLoader.class));
                // uncomment if we encounter problems with the stmt above
                //SystemUtils.init3rdPartyLibs(null);
                initGPF();
            }
        }
    }

    /**
     * {@code @OnShowing}: Annotation to place on a {@code Runnable} with default constructor which should be invoked as soon as the window
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
     * {@code @OnStop}: Annotation that can be applied to {@code Runnable} or {@code Callable<Boolean>}
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

    @OnStop
    public static class StopOp implements Runnable {

        @Override
        public void run() {
            LOG.info("Stopping SNAP Desktop");
            SnapApp.getDefault().onStop();
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

    private static void initGPF() {
        OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        operatorSpiRegistry.loadOperatorSpis();
        Set<OperatorSpi> services = operatorSpiRegistry.getServiceRegistry().getServices();
        for (OperatorSpi service : services) {
            LOG.info(String.format("GPF operator SPI: %s (alias '%s')", service.getClass(), service.getOperatorAlias()));
        }
    }

    /**
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

        private Map<Object, UndoRedo.Manager> undoManagers = new HashMap<>();

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

    public static class SnapContext implements AppContext {

        private final SnapApp app = getDefault();

        @Override
        public ProductManager getProductManager() {
            return app.getProductManager();
        }

        @Override
        public Product getSelectedProduct() {
            return app.getSelectedProduct();
        }

        @Override
        public Window getApplicationWindow() {
            return app.getMainFrame();
        }

        @Override
        public String getApplicationName() {
            return app.getInstanceName();
        }

        @Override
        public void handleError(String message, Throwable t) {
            app.handleError(message, t);
        }

        @Override
        @Deprecated
        public PropertyMap getPreferences() {
            return app.getPreferencesPropertyMap();
        }

        @Override
        public ProductSceneView getSelectedProductSceneView() {
            return app.getSelectedProductSceneView();
        }
    }

    private class MainFrameTitleUpdater implements SelectionSupport.Handler<ProductNode>, ProductNodeListener {

        @Override
        public void selectionChange(ProductNode oldValue, ProductNode newValue) {
            updateMainFrameTitle();
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (ProductNode.PROPERTY_NAME_NAME.equals(event.getPropertyName())) {
                updateMainFrameTitle();
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
        }
    }
}
