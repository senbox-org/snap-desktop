package org.esa.snap.gui;

import org.esa.snap.gui.util.ContextGlobalExtender;
import org.netbeans.modules.openide.windows.GlobalActionContextImpl;
import org.openide.util.ContextGlobalProvider;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.util.lookup.ServiceProvider;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * This class proxies the original ContextGlobalProvider and ensures that a set
 * of additional objects remain in the GlobalContext regardless of the TopComponent
 * selection.
 *
 * @see ContextGlobalProvider
 * @see GlobalActionContextImpl
 */
@ServiceProvider(
        service = ContextGlobalProvider.class,
        supersedes = "org.netbeans.modules.openide.windows.GlobalActionContextImpl"
)
public class SnapGlobalActionContext implements ContextGlobalProvider, ContextGlobalExtender {

    private static SnapGlobalActionContext instance;
    private Lookup proxyLookup;
    private final InstanceContent constantContent;
    private final Map<Object, Object> constantInstances;

    public SnapGlobalActionContext() {
        instance = this;
        constantContent = new InstanceContent();
        constantInstances = new LinkedHashMap<>();
    }

    public static SnapGlobalActionContext getInstance() {
        return instance;
    }

    @Override
    public synchronized Object get(Object key) {
        return constantInstances.get(key);
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        Object oldValue = constantInstances.put(key, value);
        if (oldValue != null) {
            constantContent.remove(oldValue);
        }
        constantContent.add(value);
        return oldValue;
    }

    @Override
    public synchronized Object remove(Object key) {
        Object oldValue = constantInstances.remove(key);
        if (oldValue != null) {
            constantContent.remove(oldValue);
        }
        return oldValue;
    }

    /**
     * Returns a ProxyLookup that adds the current extra instance to the
     * global selection returned by Utilities.actionsGlobalContext().
     *
     * @return a ProxyLookup that includes the original global context lookup.
     */
    @Override
    public Lookup createGlobalContext() {
        if (proxyLookup == null) {
            proxyLookup = new ProxyLookup(new GlobalActionContextImpl().createGlobalContext(),
                                          Lookups.singleton(this),
                                          new AbstractLookup(constantContent));
        }
        return proxyLookup;
    }
}