package org.esa.snap.gui;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.modules.OnStart;
import org.openide.modules.OnStop;
import org.openide.windows.OnShowing;
import org.openide.windows.WindowManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;

/**
 * The central SNAP application class (dummy).
 *
 * @author Norman Fomferra
 */
@SuppressWarnings("UnusedDeclaration")
public class SnapApp {

    private static SnapApp instance;

    protected SnapApp() {
    }

    public static SnapApp getInstance() {
        return instance;
    }

    protected static void setInstance(SnapApp instance) {
        SnapApp.instance = instance;
    }

    public Frame getMainWindow() {
        return WindowManager.getDefault().getMainWindow();
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
     * <p/>
     * First of all call {@code Callable}s are consulted to allow or deny proceeding with the shutdown.
     * <p/>
     * If the shutdown is approved, all {@code Runnable}s registered are acknowledged and can perform the shutdown
     * cleanup. The {@code Runnable}s are invoked in parallel. It is guaranteed their execution is finished before
     * the shutdown sequence is over.
     */
    @OnStop
    public static class MaybeStopOp implements Callable {

        @Override
        public Boolean call() {
            Frame mainWindow = getInstance().getMainWindow();
            if (mainWindow == null || !mainWindow.isShowing()) {
                return true;
            }
            System.out.println(">>> " + getClass() + " called");
            ActionListener actionListener = (ActionEvent e) -> {
                System.out.println(">>> " + getClass() + " action called");
                // do something useful;
            };
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
}
