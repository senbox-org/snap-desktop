package org.esa.snap.netbeans.docwin;

import org.openide.windows.TopComponent;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

/**
 * Implementation of a {@link NotifiableComponent} based on reflection.
 *
 * @author Norman Fomferra
 * @since 1.0
 */
class NotifiableComponentImpl implements NotifiableComponent {
    private final TopComponent topComponent;

    public NotifiableComponentImpl(TopComponent topComponent) {
        this.topComponent = topComponent;
    }

    public void componentOpened() {
        invoke("componentOpened");
    }

    @Override
    public void componentClosed() {
        invoke("componentClosed");
    }

    @Override
    public void componentShowing() {
        invoke("componentShowing");
    }

    @Override
    public void componentHidden() {
        invoke("componentHidden");
    }

    @Override
    public void componentActivated() {
        invoke("componentActivated");
    }

    @Override
    public void componentDeactivated() {
        invoke("componentDeactivated");
    }

    private void invoke(String name) {
        try {
            topComponent.getClass().getMethod(name).invoke(topComponent);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            Logger.getLogger(NotifiableComponentImpl.class.getName()).warning(e.getMessage());
        }
    }
}
