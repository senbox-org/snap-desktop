package org.esa.snap.gui;

import org.openide.modules.OnStart;
import org.openide.modules.OnStop;
import org.openide.windows.OnShowing;

import java.awt.EventQueue;
import java.util.concurrent.Callable;

/**
 * @author Norman Fomferra
 * @see
 */
public class SnapApp {


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
            System.out.println(">>> " + getClass() + " called");
            return true;
        }
    }

    @OnStop
    public static class StopOp implements Runnable {
        
        @Override
        public void run() {
            System.out.println(">>> " + getClass() + " called");
            // do some cleanup
        }
    }
}
