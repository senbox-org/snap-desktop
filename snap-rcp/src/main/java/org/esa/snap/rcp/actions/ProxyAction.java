package org.esa.snap.rcp.actions;


import com.bc.ceres.core.Assert;
import org.esa.snap.util.SystemUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A proxy action which can be used to programmatically register delegate actions which will *not* be serialized
 * into the NetBeans filesystem.
 * The proxy protects the delegate action from being serialized/deserialized by NetBeans.
 * by putting the delegate into a static hash table. On serialisation request only the file system path is serialized
 * and used to look up the delegate action later on deserialisation request.
 * <p>
 * The following methods can be used to dynamically add/remove actions and action references to/from
 * the NetBeans file system:
 * <ol>
 * <li>{@link ProxyAction#addAction(Action, String)} / {@link ProxyAction#removeAction(FileObject)}</li>
 * <li>{@link ProxyAction#addActionReference(FileObject, String, Integer)} / {@link ProxyAction#removeActionReference(FileObject)}</li>
 * </ol>
 *
 * @author Norman Fomferra
 */
public class ProxyAction implements Action, Serializable {

    private static final long serialVersionUID = 3069372659219673560L;
    private static final String INSTANCE_PREFIX = "ProxyAction-";
    private static final String INSTANCE_SUFFIX = ".instance";
    private static final String SHADOW_SUFFIX = ".shadow";
    private static final Map<String, Action> DELEGATES = new HashMap<>();

    private String path;
    private Action delegate;

    ProxyAction(Action delegate, String path) {
        Assert.notNull(delegate, "delegate");
        Assert.notNull(path, "path");
        Assert.argument(path.endsWith(INSTANCE_SUFFIX), "path");
        Assert.argument(path.contains("/"), "path");
        this.path = path;
        this.delegate = delegate;
        DELEGATES.put(path, delegate);
        SystemUtils.LOG.info(String.format(">>> ProxyAction.<init>: path=%s, delegate=%s%n", this.path, this.delegate));
    }

    public Action getDelegate() {
        return delegate;
    }

    public String getPath() {
        return path;
    }

    @Override
    public Object getValue(String key) {
        return delegate.getValue(key);
    }

    @Override
    public void putValue(String key, Object value) {
        delegate.putValue(key, value);
    }

    @Override
    public void setEnabled(boolean b) {
        delegate.setEnabled(b);
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        delegate.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        delegate.removePropertyChangeListener(listener);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        delegate.actionPerformed(e);
    }

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
            FileObject actionFile = FileUtil.createData(configRoot, getDataPath(path, action));
            actionFile.setAttribute("instanceCreate", new ProxyAction(action, actionFile.getPath()));
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
        Action actualAction = DELEGATES.get(instanceFile.getPath());
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
        if (DELEGATES.containsKey(actionFile.getPath())) {
            try {
                actionFile.delete();
                DELEGATES.remove(actionFile.getPath());
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

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        String path = in.readUTF();
        Action delegate = DELEGATES.get(path);
        SystemUtils.LOG.info(String.format(">>> ProxyAction.readObject: path=%s, delegate=%s%n", path, delegate));
        if (delegate == null) {
            throw new IOException(String.format("Action delegate not found for file path %s (don't worry this might be intended)", path));
        }
        this.path = path;
        this.delegate = delegate;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        SystemUtils.LOG.info(String.format(">>> ProxyAction.writeObject: path=%s, delegate=%s%n", path, delegate));
        out.writeUTF(path);
    }

    private static String getDataPath(String folderPath, Action delegate) {
        Object commandKey = delegate.getValue(ACTION_COMMAND_KEY);
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
        return folderPath + "/" + id;
    }
}
