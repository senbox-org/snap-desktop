package org.esa.snap.rcp.scripting;


import com.bc.ceres.core.Assert;
import org.esa.snap.core.util.SystemUtils;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A proxy action which can be used to programmatically register delegate actions which will *not* be serialized
 * into the NetBeans filesystem.
 * The proxy protects the delegate action from being serialized/deserialized by the NetBeans Platform
 * by putting the delegate into a static hash table. On serialisation request only the file system path is serialized
 * and used to look up the delegate action later on deserialisation request.
 *
 * @author Norman Fomferra
 */
public class TransientAction implements Action, Serializable {

    private static final long serialVersionUID = 3069372659219673560L;
    private static final Map<String, Action> DELEGATES = new HashMap<>();

    private String path;
    private Action delegate;

    TransientAction(Action delegate, String path) {
        Assert.notNull(delegate, "delegate");
        Assert.notNull(path, "path");
        Assert.argument(path.endsWith(".instance"), "path");
        Assert.argument(path.contains("/"), "path");
        this.path = path;
        this.delegate = delegate;
        Action oldDelegate = DELEGATES.put(path, delegate);
        if (oldDelegate != null) {
            SystemUtils.LOG.info(String.format("Proxy action %s registered once more. Replacing the old action.%n", this.path));
        }
        SystemUtils.LOG.info(String.format("Proxy action added as %s%n", this.path));
    }

    static Action getAction(String path1) {
        return DELEGATES.get(path1);
    }

    static boolean hasAction(String path) {
        return DELEGATES.containsKey(path);
    }

    static Action removeAction(String path1) {
        return DELEGATES.remove(path1);
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

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        String path = in.readUTF();
        Action delegate = DELEGATES.get(path);
        if (delegate == null) {
            throw new IOException(String.format("Action delegate not found for file %s.\n" +
                                                        "Please make sure to call removeAction() before SNAP shuts down.", path));
        }
        this.path = path;
        this.delegate = delegate;
        SystemUtils.LOG.info(String.format("Deserialized proxy action %s%n", this.path));
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(path);
        SystemUtils.LOG.info(String.format("Serialized proxy action %s%n", this.path));
    }

}
