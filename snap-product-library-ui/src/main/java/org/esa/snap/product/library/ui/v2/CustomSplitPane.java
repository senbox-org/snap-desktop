package org.esa.snap.product.library.ui.v2;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Graphics;

/**
 * Created by jcoravu on 20/8/2019.
 */
public class CustomSplitPane extends JSplitPane {

    private final int visibleDividerSize;
    private final int dividerMargins;

    public CustomSplitPane(int newOrientation, int visibleDividerSize, int dividerMargins) {
        super(newOrientation);

        this.visibleDividerSize = visibleDividerSize;
        this.dividerMargins = dividerMargins;

        super.setDividerSize((2*this.dividerMargins) + this.visibleDividerSize);
        setContinuousLayout(true);
        setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    public void setDividerSize(int newSize) {
        // do nothing
    }

    @Override
    public void updateUI() {
        setUI(new CustomSplitPaneDividerUI());
        revalidate();
    }

    private class CustomSplitPaneDividerUI extends BasicSplitPaneUI {

        private CustomSplitPaneDividerUI() {
        }

        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new CustomSplitPaneDivider(this);
        }
    }

    private class CustomSplitPaneDivider extends BasicSplitPaneDivider {

        private CustomSplitPaneDivider(BasicSplitPaneUI ui) {
            super(ui);

            super.setBorder(BorderFactory.createEmptyBorder());
            setBackground(UIManager.getColor("controlShadow"));
        }

        @Override
        public void setBorder(Border border) {
            // do nothing
        }

        @Override
        public void paint(Graphics graphics) {
            graphics.setColor(getBackground());
            if (this.orientation == HORIZONTAL_SPLIT) {
                graphics.fillRect(dividerMargins, 0, visibleDividerSize, getHeight());
            } else {
                graphics.fillRect(0, dividerMargins, getWidth(), visibleDividerSize);
            }
        }
    }
}
