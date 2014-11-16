package org.esa.snap.gui;

import com.bc.ceres.jai.operator.ReinterpretDescriptor;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.util.PropertyMap;
import org.esa.snap.gui.compat.CompatiblePropertyMap;
import org.esa.snap.tango.TangoIcons;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.NotificationDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.modules.OnStart;
import org.openide.modules.OnStop;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;
import org.openide.windows.OnShowing;
import org.openide.windows.WindowManager;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * The central SNAP application class (dummy).
 *
 * @author Norman Fomferra
 */
@SuppressWarnings("UnusedDeclaration")
public class SnapApp {

    private final static Logger LOG = Logger.getLogger("org.esa.snap.gui");

    private static SnapApp instance;

    protected SnapApp() {
    }

    public static SnapApp getInstance() {
        return instance;
    }

    protected static void setInstance(SnapApp instance) {
        SnapApp.instance = instance;
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

    public String getInstanceName() {
        return NbBundle.getBundle("org.netbeans.core.ui.Bundle").getString("LBL_ProductInformation");
    }

    public void showOutOfMemoryErrorDialog(String message) {
        showErrorDialog("Out of Memory", message);
    }

    public void showErrorDialog(String title, String message) {
        NotifyDescriptor nd = new NotifyDescriptor(message,
                                                   title,
                                                   JOptionPane.OK_OPTION,
                                                   NotifyDescriptor.ERROR_MESSAGE,
                                                   null,
                                                   null);
        DialogDisplayer.getDefault().notify(nd);

        ImageIcon icon = TangoIcons.status_dialog_error(TangoIcons.Res.R16);
        JLabel balloonDetails = new JLabel(message);
        JButton popupDetails = new JButton("Call ESA");
        NotificationDisplayer.getDefault().notify(title,
                                                  icon,
                                                  balloonDetails,
                                                  popupDetails,
                                                  NotificationDisplayer.Priority.HIGH,
                                                  NotificationDisplayer.Category.ERROR);
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

    /**
     * @deprecated Should be superfluous now. Kept for compatibility issues only. Remove ASAP and before 2.0 release.
     */
    @Deprecated
    public void updateState() {
    }

    public void handleError(String message, Throwable t) {
        if (t != null) {
            t.printStackTrace();
        }
        showErrorDialog(getInstanceName() + " - Error", message);
        getLogger().log(Level.SEVERE, message, t);
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
            System.out.println(">>> " + getClass() + " called");
            setInstance(new SnapApp());
            initJAI();
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
            assert EventQueue.isDispatchThread();
            System.out.println(">>> " + getClass() + " called");
            // do something visual
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
            Frame mainWindow = getInstance().getMainFrame();
            if (mainWindow == null || !mainWindow.isShowing()) {
                return true;
            }
            System.out.println(">>> " + getClass() + " called");
            ActionListener actionListener = (ActionEvent e) -> System.out.println(">>> " + getClass() + " action called");
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
    }

    @OnStop
    public static class StopOp implements Runnable {

        @Override
        public void run() {
            System.out.println(">>> " + getClass() + " called");
            // do some cleanup
            setInstance(null);
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
        LOG.info(MessageFormat.format("JAI tile scheduler parallelism set to {0}", parallelism));

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

}
