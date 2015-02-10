package org.esa.snap.rcp.util;

/**
 * Allows to dynamically extend the contents of the global lookup.
 * <p>
 * This interface may be implemented by a special {@code org.openide.util.ContextGlobalProvider} which should
 * also register an instance of this interface so that is is accessible via the global lookup.
 *
 * @author Norman Fomferra
 * @since 2.0
 */
public interface ContextGlobalExtender {
    Object get(Object key);

    Object put(Object key, Object value);

    Object remove(Object key);
}
