package org.esa.snap.product.library.ui.v2;

/**
 * Created by jcoravu on 9/8/2019.
 */
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

public class CustomLayeredPane extends JLayeredPane {

    private final JPanel contentPanel;
    private JPanel panel;

    public CustomLayeredPane(LayoutManager contentPanelLayoutManager) {
        super();

        this.contentPanel = new JPanel(contentPanelLayoutManager);
        add(this.contentPanel, JLayeredPane.FRAME_CONTENT_LAYER);
    }

    @Override
    public final void doLayout() {
        super.doLayout();

        // set content panel bounds
        Rectangle layeredPaneBounds = getBounds();
        this.contentPanel.setBounds(new Rectangle(0, 0, layeredPaneBounds.width, layeredPaneBounds.height));
        if (this.panel != null && this.equals(this.panel.getParent())) {
            Dimension size = this.panel.getPreferredSize();
            int x = layeredPaneBounds.width / 2 - size.width / 2;
            int y = layeredPaneBounds.height / 2 - size.height / 2;
            this.panel.setBounds(x, y, size.width, size.height);
        }
    };

    @Override
    public final Dimension getPreferredSize() {
        return this.contentPanel.getPreferredSize();
    }

    @Override
    public final void setPreferredSize(Dimension preferredSize) {
        this.contentPanel.setPreferredSize(preferredSize);
    }

    public final JPanel getContentPanel() {
        return this.contentPanel;
    }

    public final void addToContentPanel(JComponent component, Object constraint) {
        this.contentPanel.add(component, constraint);
    }

    public final void addPanelToModalLayerAndPositionInCenter(JPanel panel) {
        if (this.panel != null) {
            remove(this.panel);
        }
        this.panel = panel;
        add(this.panel, JLayeredPane.MODAL_LAYER);
    }
}
