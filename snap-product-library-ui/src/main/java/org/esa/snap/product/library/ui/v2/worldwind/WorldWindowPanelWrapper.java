package org.esa.snap.product.library.ui.v2.worldwind;

import gov.nasa.worldwind.geom.Position;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.ui.loading.CircularProgressIndicatorLabel;
import org.esa.snap.ui.loading.GenericRunnable;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

public class WorldWindowPanelWrapper extends JPanel {

    private WorldWindowPanel worldWindowPanel;

    public WorldWindowPanelWrapper() {
        super(new GridBagLayout());

        setBackground(Color.WHITE);
        setOpaque(true);
        setBorder(new EtchedBorder());

        CircularProgressIndicatorLabel circularProgressLabel = new CircularProgressIndicatorLabel();
        circularProgressLabel.setOpaque(false);
        circularProgressLabel.setRunning(true);

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.CENTER, 1, 1, 0, 0);
        add(circularProgressLabel, c);
    }

    public void addWorldWindowPanelAsync(boolean flatWorld, boolean removeExtraLayers) {
        InitWorldWindPanelRunnable thread = new InitWorldWindPanelRunnable(this, flatWorld, removeExtraLayers);
        thread.executeAsync(); // start the thread
    }

    public void clearSelectedArea() {
        if (this.worldWindowPanel != null) {
            this.worldWindowPanel.clearSelectedArea();
        }
    }

    public Rectangle2D getSelectedArea() {
        if (this.worldWindowPanel != null) {
            return this.worldWindowPanel.getSelectedArea();
        }
        return null;
    }

    public void setEyePosition(Path2D.Double polygonPath) {
        if (this.worldWindowPanel != null) {
            Rectangle2D rectangleBounds = polygonPath.getBounds2D();
            Position eyePosition = this.worldWindowPanel.getView().getEyePosition();
            Position position = Position.fromDegrees(rectangleBounds.getCenterY(), rectangleBounds.getCenterX(), eyePosition.getElevation());
            this.worldWindowPanel.getView().setEyePosition(position);
            this.worldWindowPanel.redrawNow();
        }
    }

    public void highlightPolygons(Path2D.Double[] polygonPaths) {
        if (this.worldWindowPanel != null) {
            PolygonLayer polygonLayer = this.worldWindowPanel.getPolygonLayer();
            polygonLayer.highlightPolygons(polygonPaths);
            this.worldWindowPanel.redrawNow();
        }
    }

    public void setPolygons(Path2D.Double[] polygonPaths) {
        if (this.worldWindowPanel != null) {
            PolygonLayer polygonLayer = this.worldWindowPanel.getPolygonLayer();
            polygonLayer.setPolygons(polygonPaths);
            this.worldWindowPanel.redrawNow();
        }
    }

    private void addWorldWindowPanel(WorldWindowPanel worldWindowPanel) {
        removeAll();

        if (worldWindowPanel != null) {
            this.worldWindowPanel = worldWindowPanel;
            this.worldWindowPanel.setBackgroundColor(getBackground());

            GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.BOTH, GridBagConstraints.CENTER, 1, 1, 0, 0);
            add(this.worldWindowPanel, c);
        }

        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private static class InitWorldWindPanelRunnable extends AbstractRunnable<WorldWindowPanel> {

        private final WorldWindowPanelWrapper worldWindowPanel;
        private final boolean flatWorld;
        private final boolean removeExtraLayers;

        public InitWorldWindPanelRunnable(WorldWindowPanelWrapper worldWindowPanel, boolean flatWorld, boolean removeExtraLayers) {
            this.worldWindowPanel = worldWindowPanel;
            this.flatWorld = flatWorld;
            this.removeExtraLayers = removeExtraLayers;
        }

        @Override
        protected WorldWindowPanel execute() throws Exception {
            return new WorldWindowPanel(this.flatWorld, this.removeExtraLayers);
        }

        @Override
        protected String getExceptionLoggingMessage() {
            return "Failed to create the world window panel.";
        }

        @Override
        protected final void successfullyExecuting(WorldWindowPanel result) {
            GenericRunnable<WorldWindowPanel> runnable = new GenericRunnable<WorldWindowPanel>(result) {
                @Override
                protected void execute(WorldWindowPanel item) {
                    worldWindowPanel.addWorldWindowPanel(item);
                }
            };
            SwingUtilities.invokeLater(runnable);
        }

        @Override
        protected void failedExecuting(Exception exception) {
            GenericRunnable<Exception> runnable = new GenericRunnable<Exception>(exception) {
                @Override
                protected void execute(Exception item) {
                    worldWindowPanel.addWorldWindowPanel(null);
                }
            };
            SwingUtilities.invokeLater(runnable);
        }
    }
}
