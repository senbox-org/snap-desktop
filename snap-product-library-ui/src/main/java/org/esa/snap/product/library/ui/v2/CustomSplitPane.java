package org.esa.snap.product.library.ui.v2;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Color;
import java.awt.Graphics;

/**
 * Created by jcoravu on 20/8/2019.
 */
public class CustomSplitPane extends JSplitPane {

    private final int visibleDividerSize;
    private final int dividerMargins;
    private final Color dividerColor;

    public CustomSplitPane(int newOrientation, int visibleDividerSize, int dividerMargins) {
        this(newOrientation, visibleDividerSize, dividerMargins, UIManager.getColor("controlShadow"));
    }

    public CustomSplitPane(int newOrientation, int visibleDividerSize, int dividerMargins, Color dividerColor) {
        super(newOrientation);

        this.visibleDividerSize = visibleDividerSize;
        this.dividerMargins = dividerMargins;
        this.dividerColor = dividerColor;

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
            setBackground(CustomSplitPane.this.dividerColor);
        }

        @Override
        public void setBorder(Border border) {
            // do nothing
        }

        @Override
        public void paint(Graphics graphics) {
            graphics.setColor(getBackground());
            if (this.orientation == HORIZONTAL_SPLIT) {
                graphics.fillRect(CustomSplitPane.this.dividerMargins, 0, CustomSplitPane.this.visibleDividerSize, getHeight());
            } else {
                graphics.fillRect(0, CustomSplitPane.this.dividerMargins, getWidth(), CustomSplitPane.this.visibleDividerSize);
            }
        }
    }
}
