package org.esa.snap.rcp.util;

import com.bc.ceres.core.DefaultResourceLocator;
import org.openide.util.Lookup;

/**
 * A resource locator service for the NetBeans platform.
 *
 * @author Norman
 */
public class NbResourceLocator extends DefaultResourceLocator {
    /**
     * Returns a class loader capable of loading resources from any enabled NetBeans module.
     *
     * See <a href="http://wiki.netbeans.org/ClassloaderTrick">NetBeans Platform ClassLoader Trick</a>.
     *
     * @return the class loader used to load resources.
     */
    @Override
    protected ClassLoader getResourceClassLoader() {
        // see
        ClassLoader classLoader = Lookup.getDefault().lookup(ClassLoader.class);
        if (classLoader == null) {
            throw new IllegalStateException("failed to lookup NetBeans global class loader");
        }
        return classLoader;
    }
}
