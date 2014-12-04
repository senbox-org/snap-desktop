package org.esa.snap.gui.util;

import org.openide.windows.TopComponent;

/**
 * Interface comprising the {@code component<XXX>()} notification methods of a {@code TopComponent}.
 *
 * @author Norman Fomferra
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

