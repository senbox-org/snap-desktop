package org.esa.snap.rcp.scripting;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import javax.swing.Action;
import java.io.IOException;


/**
 * Provides various utility functions allowing scripting clients to register actions and windows
 * for the SNAP Desktop application.
 * <p>
 * The following methods can be used to dynamically add/remove actions and action references to/from
 * the SNAP Desktop application:
 * <ul>
 * <li>{@link SnapUtils#addAction(Action, String)}</li>
 * <li>{@link SnapUtils#removeAction(FileObject)}</li>
 * </ul>
 * The {@code addAction()} methods all return "file objects" which live in the NetBeans
 * Platform's virtual file system.
 * These object can be used to create references to the actions they represent in various places
 * such as menus and tool bars:
 * <ul>
 * <li>{@link SnapUtils#addActionReference(FileObject, String, Integer)}</li>
 * <li>{@link SnapUtils#removeActionReference(FileObject)}</li>
 * </ul>
 * <p>
 * To open a new window in the SNAP Desktop application, the following methods can be used:
 * <ul>
 * <li>{@link SnapUtils#openWindow(TopComponent)}</li>
 * <li>{@link SnapUtils#openWindow(TopComponent, boolean)}</li>
 * <li>{@link SnapUtils#openWindow(TopComponent, String)}</li>
 * <li>{@link SnapUtils#openWindow(TopComponent, String, boolean)}</li>
 * </ul>
 *
 * @author Norman Fomferra
 */
public class SnapUtils {

    private static final String INSTANCE_PREFIX = "TransientAction-";
    private static final String INSTANCE_SUFFIX = ".instance";
    private static final String SHADOW_SUFFIX = ".shadow";

    /**
     * Adds an action into the folder {@code Menu/Tools} of the SNAP Desktop / NetBeans file system.
     *
     * @param action The action.
     * @return The file object representing the action.
     */
    public synchronized static FileObject addAction(Action action) {
        return addAction(action, "Menu/Tools");
    }

    /**
     * Adds an action into the folder given by {@code path} of the SNAP Desktop / NetBeans file system.
     *
     * @param action The action.
     * @param path   The folder path.
     * @return The file object representing the action.
     */
    public synchronized static FileObject addAction(Action action, String path) {
        return addAction(action, path, null);
    }

    /**
     * Adds an action into the folder given by {@code path} of the SNAP Desktop / NetBeans file system at the given {@code position}.
     *
     * @param action   The action.
     * @param path     The folder path.
     * @param position The position within the folder. May be {@code null}.
     * @return The file object representing the action.
     */
    public synchronized static FileObject addAction(Action action, String path, Integer position) {
        FileObject configRoot = FileUtil.getConfigRoot();
        try {
            FileObject actionFile = FileUtil.createData(configRoot, getActionDataPath(path, action));
            actionFile.setAttribute("instanceCreate", new TransientAction(action, actionFile.getPath()));
            if (position != null) {
                actionFile.setAttribute("position", position);
            }
            return actionFile;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    /**
     * Adds an action references into the folder given by {@code path} of the SNAP Desktop / NetBeans file system at the given {@code position}.
     * The following are standard folders in SNAP Desktop/NetBeans where actions and action references may be placed to
     * become visible:
     * <ol>
     * <li>{@code Menu/*} - the main menu</li>
     * <li>{@code Toolbars/*} - the main tool bar</li>
     * </ol>
     *
     * @param instanceFile The file object representing an action instance.
     * @param path         The folder path.
     * @param position     The position within the folder. May be {@code null}.
     * @return The file object representing the action reference.
     */
    public synchronized static FileObject addActionReference(FileObject instanceFile, String path, Integer position) {
        Action actualAction = TransientAction.getAction(instanceFile.getPath());
        if (actualAction == null) {
            return null;
        }
        String shadowId = instanceFile.getName() + SHADOW_SUFFIX;
        FileObject configRoot = FileUtil.getConfigRoot();
        try {
            FileObject actionFile = FileUtil.createData(configRoot, path + "/" + shadowId);
            actionFile.setAttribute("originalFile", instanceFile.getPath());
            if (position != null) {
                actionFile.setAttribute("position", position);
            }
            return actionFile;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return null;
    }


    public synchronized static boolean removeAction(FileObject actionFile) {
        if (TransientAction.hasAction(actionFile.getPath())) {
            try {
                actionFile.delete();
                TransientAction.removeAction(actionFile.getPath());
                return true;
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return false;
    }

    public synchronized static boolean removeActionReference(FileObject actionReferenceFile) {
        Object originalFile = actionReferenceFile.getAttribute("originalFile");
        if (originalFile != null) {
            String originalPath = originalFile.toString();
            int slashPos = originalPath.lastIndexOf('/');
            if (slashPos > 0
                    && originalPath.substring(slashPos + 1).startsWith(INSTANCE_PREFIX)
                    && originalPath.endsWith(INSTANCE_SUFFIX)) {
                try {
                    actionReferenceFile.delete();
                    return true;
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        return false;
    }

    /**
     * Opens a new window in SNAP Desktop in the "explorer" location.
     *
     * @param window The window which must must be an instance of {@link TopComponent}.
     * @see #openWindow(TopComponent, String, boolean)
     */
    public static void openWindow(TopComponent window) {
        openWindow(window, false);
    }

    /**
     * Opens a new window in SNAP Desktop in the "explorer" location.
     *
     * @param window The window which must must be an instance of {@link TopComponent}.
     * @param requestActive {@code true} if a request will be made to activate the window after opening it.
     * @see #openWindow(TopComponent, String, boolean)
     */
    public static void openWindow(TopComponent window, boolean requestActive) {
        openWindow(window, "explorer", requestActive);
    }

    /**
     * Opens a new window in SNAP Desktop at the given location.
     *
     * @param window The window which must must be an instance of {@link TopComponent}.
     * @param location The location where the window should appear when it is first opened.
     * @see #openWindow(TopComponent, String, boolean)
     */
    public static void openWindow(TopComponent window, String location) {
        openWindow(window, location, false);
    }

    /**
     * Opens a new window in SNAP Desktop.
     *
     * @param window The window which must must be an instance of {@link TopComponent}.
     * @param location The location where the window should appear when it is first opened.
     *                 Possible docking areas are
     *                 "explorer" (upper left), "navigator" (lower left), "properties" (upper right),
     *                 "output" (bottom). You may choose "floating" to not dock the window at all. Note
     *                 that this mode requires explicitly setting the window's position and size.
     * @param requestActive {@code true} if a request will be made to activate the window after opening it.
     */
    public static void openWindow(TopComponent window, String location, boolean requestActive) {
        WindowManager.getDefault().invokeWhenUIReady(() -> {
            Mode mode = WindowManager.getDefault().findMode(location);
            if (mode != null) {
                mode.dockInto(window);
            }
            window.open();
            if (requestActive) {
                window.requestActive();
            }
        });
    }

    private static String getActionDataPath(String folderPath, Action delegate) {
        return folderPath + "/" + getActionInstanceName(delegate);
    }

    private static String getActionInstanceName(Action delegate) {
        Object commandKey = delegate.getValue(Action.ACTION_COMMAND_KEY);
        String id;
        if (commandKey != null && !commandKey.toString().isEmpty()) {
            id = commandKey.toString();
        } else {
            id = delegate.getClass().getName();
        }
        id = id.replace('/', '-').replace('.', '-').replace('$', '-');
        if (!id.startsWith(INSTANCE_PREFIX)) {
            id = INSTANCE_PREFIX + id;
        }
        if (!id.endsWith(INSTANCE_SUFFIX)) {
            id += INSTANCE_SUFFIX;
        }
        return id;
    }
}
