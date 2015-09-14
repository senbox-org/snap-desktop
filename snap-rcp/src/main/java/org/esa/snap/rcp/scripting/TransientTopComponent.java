package org.esa.snap.rcp.scripting;

import org.openide.util.Lookup;
import org.openide.windows.TopComponent;

/**
 * The {@code TransientTopComponent} is a convenience base class for SNAP Desktop
 * windows that are registered through scripts.
 * <p>
 * Script programmers may use this class as a base class for their windows
 * in order to avoid serialisation errors caused by the NetBeans Platform.
 * The serialisation of windows occurs in order store and restore window state.
 * <p>
 * A {@code TransientTopComponent} differs from the "normal" {@link TopComponent} class
 * only in that it overrides the {@link TopComponent#getPersistenceType()} to always return
 * {@link TopComponent#PERSISTENCE_NEVER}.
 *
 * @author Norman Fomferra
 */
public class TransientTopComponent extends TopComponent {

    public TransientTopComponent() {
        this(null);
    }

    public TransientTopComponent(Lookup lookup) {
        super(lookup);
    }

    @Override
    public final int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }
}
