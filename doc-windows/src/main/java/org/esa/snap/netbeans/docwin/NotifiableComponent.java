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
    void componentOpened();

    void componentClosed();

    void componentShowing();

    void componentHidden();

    void componentActivated();

    void componentDeactivated();

    static NotifiableComponent get(TopComponent topComponent) {
        if (topComponent instanceof NotifiableComponent) {
            return (NotifiableComponent) topComponent;
        }
        return new NotifiableComponentImpl(topComponent);
    }
}

