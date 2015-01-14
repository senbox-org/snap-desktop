package org.esa.snap.gui;

import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.jai.operator.ReinterpretDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;
import org.esa.snap.gui.util.CompatiblePropertyMap;
import org.esa.snap.gui.util.ContextGlobalExtenderImpl;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.awt.UndoRedo;
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

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * The central SNAP application class.
 * <p>
 * If you want to provide alter behaviour of this class, register your derived class as a service using
 * <pre>
 *     &#64;ServiceProvider(service = MoonApp.class, supersedes = "org.esa.snap.gui.SnapApp")
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

        Lookup.Result<ProductNode> productNodeSelection = Utilities.actionsGlobalContext().lookupResult(ProductNode.class);
        productNodeSelection.addLookupListener(ev -> {
            updateMainFrameTitle();
        });
    }

    public ProductManager getProductManager() {
        return productManager;
    }

    public UndoRedo.Manager getUndoManager(Product product) {
        return product.getExtension(UndoRedo.Manager.class);
    }

    public Frame getMainFrame() {
        return WindowManager.getDefault().getMainWindow();
    }

    public ProductNode getSelectedProductNode() {
        return Utilities.actionsGlobalContext().lookup(ProductNode.class);
    }

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
        return NbBundle.getBundle("org.netbeans.core.ui.Bundle").getString("LBL_ProductInformation");
    }

    /**
     * @return The user's application preferences.
     */
    public Preferences getPreferences() {
        return NbPreferences.forModule(getClass());
    }

    /**
     * @return The user's application preferences.
     * @deprecated this is for compatibility only, use #getPreferences()
     */
    @Deprecated
    public PropertyMap getCompatiblePreferences() {
        return new CompatiblePropertyMap(getPreferences());
    }

    public Logger getLogger() {
        return LOG;
    }

    public void handleError(String message, Throwable t) {
        if (t != null) {
            t.printStackTrace();
        }
        SnapDialogs.showError(getInstanceName() + " - Error", message);
        getLogger().log(Level.SEVERE, message, t);
    }

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

    public String getMainFrameTitle() {

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

    public ProductSceneView getSelectedProductSceneView() {
        return Utilities.actionsGlobalContext().lookup(ProductSceneView.class);
    }


    public void onStart() {
        WindowManager.getDefault().setRole("developer");
    }

    public void onShowing() {
        updateMainFrameTitle();
    }

    public boolean onTryStop() {
        Frame mainWindow = getDefault().getMainFrame();
        if (mainWindow == null || !mainWindow.isShowing()) {
            return true;
        }
        ActionListener actionListener = (ActionEvent e) -> LOG.info(">>> " + getClass() + " action called");
        JLabel label = new JLabel("<html>SNAP found some cached <b>bazoo files</b> in your <b>gnarz folder</b>.<br>" +
                                          "Should they be rectified now?");
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(label);
        DialogDescriptor dialogDescriptor = new DialogDescriptor(
                panel,
                "Confirm",
                true,
                DialogDescriptor.YES_NO_CANCEL_OPTION,
                null,
                actionListener);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor, mainWindow);
        dialog.setVisible(true);
        Object value = dialogDescriptor.getValue();
        return !new Integer(2).equals(value);
    }

    public void onStop() {
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
            LOG.fine(">>> " + getClass() + " called");
            initJAI();
            SnapApp.getDefault().onStart();
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
            LOG.fine(getClass() + " called");
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
            LOG.fine(">>> " + getClass() + " called");
            return SnapApp.getDefault().onTryStop();
        }
    }

    @OnStop
    public static class StopOp implements Runnable {

        @Override
        public void run() {
            LOG.fine(">>> " + getClass() + " called");
            SnapApp.getDefault().onStop();
        }
    }

    private static void initJAI() {
        // Disable native libraries for JAI:
        // This suppresses ugly (and harmless) JAI error messages saying that a JAI is going to
        // continue in pure Java mode.
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");

        // Set JAI tile scheduler parallelism
        int processorCount = Runtime.getRuntime().availableProcessors();
        int parallelism = Integer.getInteger("snap.jai.parallelism", processorCount);
        JAI.getDefaultInstance().getTileScheduler().setParallelism(parallelism);
        LOG.info(MessageFormat.format(">>> JAI tile scheduler parallelism set to {0}", parallelism));

        // Load JAI registry files.
        // For some reason registry file loading must be done in this order: first our own, then JAI's descriptors (nf)
        loadJaiRegistryFile(ReinterpretDescriptor.class, "/META-INF/registryFile.jai");
        loadJaiRegistryFile(JAI.class, "/META-INF/javax.media.jai.registryFile.jai");
    }

    private static void loadJaiRegistryFile(Class<?> cls, String jaiRegistryPath) {
        LOG.info("Reading JAI registry file from " + jaiRegistryPath);
        // Must use a new operation registry in order to register JAI operators defined in Ceres and BEAM
        OperationRegistry operationRegistry = OperationRegistry.getThreadSafeOperationRegistry();
        InputStream is = cls.getResourceAsStream(jaiRegistryPath);
        if (is != null) {
            final PrintStream oldErr = System.err;
            try {
                // Suppress annoying and harmless JAI error messages saying that a descriptor is already registered.
                System.setErr(new PrintStream(new ByteArrayOutputStream()));
                operationRegistry.updateFromStream(is);
                operationRegistry.registerServices(cls.getClassLoader());
                JAI.getDefaultInstance().setOperationRegistry(operationRegistry);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, MessageFormat.format("Error loading {0}: {1}", jaiRegistryPath, e.getMessage()), e);
            } finally {
                System.setErr(oldErr);
            }
        } else {
            LOG.warning(MessageFormat.format("{0} not found", jaiRegistryPath));
        }
    }

    /**
     * This class proxies the original ContextGlobalProvider and ensures that a set
     * of additional objects remain in the GlobalContext regardless of the TopComponent
     * selection.
     *
     * @see org.esa.snap.gui.util.ContextGlobalExtenderImpl
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
}
