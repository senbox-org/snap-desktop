package org.esa.snap.rcp.util;

import org.netbeans.modules.openide.windows.GlobalActionContextImpl;
import org.openide.util.ContextGlobalProvider;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Default implementation of a {@link ContextGlobalExtender} which is also a {@link ContextGlobalProvider}.
 * <p>
 * In order to register {@link ContextGlobalProvider} service use the following code:
 * <pre>
 *     &#64;ServiceProvider(
 *         service = ContextGlobalProvider.class,
 *         supersedes = "org.netbeans.modules.openide.windows.GlobalActionContextImpl"
 *     )
 *     public class MyGlobalActionContextImpl extends ContextGlobalExtenderImpl {
 *     }
 * </pre>
 *
 * @see org.openide.util.ContextGlobalProvider
 * @see org.netbeans.modules.openide.windows.GlobalActionContextImpl
 * @author Norman Fomferra
 * @since 2.0
 */
public class ContextGlobalExtenderImpl implements ContextGlobalProvider, ContextGlobalExtender {

    private static final Logger LOG = Logger.getLogger(ContextGlobalExtenderImpl.class.getName());

    private Lookup proxyLookup;
    private final InstanceContent constantContent;
    private final Map<Object, Object> constantInstances;

    public ContextGlobalExtenderImpl() {
        constantContent = new InstanceContent();
        constantInstances = new LinkedHashMap<>();
    }

    @Override
    public synchronized Object get(Object key) {
        return constantInstances.get(key);
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        if (value == null) {
            return remove(key);
        }
        Object oldValue = constantInstances.get(key);
        if (oldValue != value) {
            constantInstances.put(key, value);
            if (oldValue != null) {
                constantContent.remove(oldValue);
            }
            constantContent.add(value);
            LOG.info("added: key = " + key + ", value = " + value + ", oldValue = " + oldValue);
        }
        return oldValue;
    }

    @Override
    public synchronized Object remove(Object key) {
        Object oldValue = constantInstances.remove(key);
        if (oldValue != null) {
            constantContent.remove(oldValue);
        }
        LOG.info("removed: key = " + key + ", oldValue = " + oldValue);
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