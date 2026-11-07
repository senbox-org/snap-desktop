package org.esa.snap.rcp.util;

import org.esa.snap.core.util.SystemUtils;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

import java.util.logging.Level;

/**
 * Defers non-essential work until SNAP Desktop has finished starting up.
 * <p>
 * Tasks are held back until the main window is showing ({@link WindowManager#invokeWhenUIReady(Runnable)}) and then run
 * on a low-priority background thread after an optional delay, so they neither compete with module loading and window
 * restoration for CPU nor block the event dispatch thread.
 */
public final class StartupTasks {

    /**
     * Default delay after the UI became ready, in milliseconds.
     */
    public static final int DEFAULT_DELAY_MS = 3000;

    private static final RequestProcessor PROCESSOR = new RequestProcessor("SNAP Post-Startup", 2, false, false);

    private StartupTasks() {
    }

    /**
     * Runs the given task in the background {@link #DEFAULT_DELAY_MS} milliseconds after the main window is showing.
     *
     * @param name a short name used for error logging
     * @param task the task, must not touch Swing components
     */
    public static void runAfterStartup(String name, Runnable task) {
        runAfterStartup(name, task, DEFAULT_DELAY_MS);
    }

    /**
     * Runs the given task in the background {@code delayMs} milliseconds after the main window is showing.
     * If the UI is already up, the delay counts from now.
     *
     * @param name    a short name used for error logging
     * @param task    the task, must not touch Swing components
     * @param delayMs delay after the UI became ready, in milliseconds
     */
    public static void runAfterStartup(String name, Runnable task, int delayMs) {
        final Runnable guarded = () -> {
            try {
                task.run();
            } catch (Throwable t) {
                SystemUtils.LOG.log(Level.WARNING, "Post-startup task '" + name + "' failed: " + t.getMessage(), t);
            }
        };
        WindowManager.getDefault().invokeWhenUIReady(() -> PROCESSOR.post(guarded, delayMs, Thread.MIN_PRIORITY));
    }
}
