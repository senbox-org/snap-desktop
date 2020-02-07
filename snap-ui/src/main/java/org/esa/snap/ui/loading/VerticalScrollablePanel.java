package org.esa.snap.ui.loading;

import java.awt.Rectangle;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.awt.Dimension;

/**
 * Created by jcoravu on 23/9/2019.
 */
public class VerticalScrollablePanel extends JPanel implements Scrollable {

    private final int maximumUnitIncrement;

    public VerticalScrollablePanel(LayoutManager layoutManager) {
        super(layoutManager);

        this.maximumUnitIncrement = 10;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        int currentPosition = 0;
        if (orientation == SwingConstants.HORIZONTAL) {
            currentPosition = visibleRect.x;
        } else {
            currentPosition = visibleRect.y;
        }
        if (direction < 0) {
            int newPosition = currentPosition - (currentPosition / this.maximumUnitIncrement) * this.maximumUnitIncrement;
            return (newPosition == 0) ? this.maximumUnitIncrement : newPosition;
        }
        return ((currentPosition / this.maximumUnitIncrement) + 1) * this.maximumUnitIncrement - currentPosition;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return visibleRect.width - this.maximumUnitIncrement;
        }
        return visibleRect.height - this.maximumUnitIncrement;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
