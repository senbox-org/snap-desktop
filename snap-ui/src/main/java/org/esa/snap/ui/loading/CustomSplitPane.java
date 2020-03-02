package org.esa.snap.ui.loading;

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

    private float initialDividerLocationPercent;

    public CustomSplitPane(int newOrientation, int visibleDividerSize, int dividerMargins, float initialDividerLocationPercent) {
        this(newOrientation, visibleDividerSize, dividerMargins, initialDividerLocationPercent, UIManager.getColor("controlShadow"));
    }

    public CustomSplitPane(int newOrientation, int visibleDividerSize, int dividerMargins, float initialDividerLocationPercent, Color dividerColor) {
        super(newOrientation);

        if (visibleDividerSize < 0) {
            throw new IllegalArgumentException("The visible divider size " + visibleDividerSize + " must be >= 0.");
        }
        if (dividerMargins < 0) {
            throw new IllegalArgumentException("The divider margins " + dividerMargins + " must be >= 0.");
        }
        if (initialDividerLocationPercent < 0.0f || initialDividerLocationPercent > 1.0f) {
            throw new IllegalArgumentException("The initial divider location percent " + initialDividerLocationPercent + " must be between 0.0 and 1.0.");
        }

        this.visibleDividerSize = visibleDividerSize;
        this.dividerMargins = dividerMargins;
        this.dividerColor = dividerColor;
        this.initialDividerLocationPercent = initialDividerLocationPercent;

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

    @Override
    public void doLayout() {
        super.doLayout();

        int availableWidth = getSize().width;
        if (availableWidth > 0 && this.initialDividerLocationPercent > 0.0f) {
            int dividerLocation = (int)(this.initialDividerLocationPercent * availableWidth);
            setDividerLocation(dividerLocation);
            this.initialDividerLocationPercent = 0.0f; // reset the divider
        }
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
