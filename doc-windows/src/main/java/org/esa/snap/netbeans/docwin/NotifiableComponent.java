package org.esa.snap.netbeans.docwin;

import org.openide.windows.TopComponent;

/**
 * Interface comprising the {@code component<XXX>()} notification methods of a {@code TopComponent}.
 * The purpose of this interface is to publish these notification methods so that they can be called
 * from outside the NetBeans Windows Manager.
 *
 * @author Norman Fomferra
 * @since 1.0
 */
public interface NotifiableComponent {

    /**
     * Invoked when a component has been opened.
     */
    void componentOpened();

    /**
     * Invoked when a component has been closed.
     */
    void componentClosed();

    /**
     * Invoked when a component has been closed.
     */
    void componentShowing();

    /**
     * Invoked when a component has been hidden.
     */
    void componentHidden();

    /**
     * Invoked when a component is activated.
     */
    void componentActivated();

    /**
     * Invoked when a component is de-activated.
     */
    void componentDeactivated();

    /**
     * Gets the notifiable interface for a given window.
     *
     * @param topComponent The window.
     * @return A {@link NotifiableComponent}, never {@code null}.
     */
    static NotifiableComponent get(TopComponent topComponent) {
        if (topComponent instanceof NotifiableComponent) {
            return (NotifiableComponent) topComponent;
        }
        return new NotifiableComponentImpl(topComponent);
    }
}

