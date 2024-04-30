package org.esa.snap.product.library.ui.v2.worldwind;

import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.product.library.ui.v2.thread.AbstractRunnable;
import org.esa.snap.ui.loading.GenericRunnable;
import org.esa.snap.worldwind.productlibrary.PolygonMouseListener;
import org.esa.snap.worldwind.productlibrary.PolygonsLayerModel;
import org.esa.snap.worldwind.productlibrary.WorldMap3DPanel;
import org.esa.snap.worldwind.productlibrary.WorldMapPanelWrapper;

import javax.swing.*;
import java.awt.*;

public class WorldMapPanelWrapperImpl extends WorldMapPanelWrapper {
    public WorldMapPanelWrapperImpl(PolygonMouseListener mouseListener, Color backgroundColor, PropertyMap persistencePreferences) {
        super(mouseListener, backgroundColor, persistencePreferences);
    }

    @Override
    public void addWorldMapPanelAsync(boolean flat3DEarth, boolean removeExtraLayers) {
        InitWorldMap3DPanelRunnable thread = new InitWorldMap3DPanelRunnable(this, flat3DEarth, removeExtraLayers, this.polygonsLayerModel);
        thread.executeAsync(); // start the thread
    }

    private static class InitWorldMap3DPanelRunnable extends AbstractRunnable<WorldMap3DPanel> {

        private final WorldMapPanelWrapper worldWindowPanel;
        private final boolean flatEarth;
        private final boolean removeExtraLayers;
        private final PolygonsLayerModel polygonsLayerModel;

        public InitWorldMap3DPanelRunnable(WorldMapPanelWrapper worldWindowPanel, boolean flatEarth, boolean removeExtraLayers, PolygonsLayerModel polygonsLayerModel) {
            this.polygonsLayerModel = polygonsLayerModel;
            this.worldWindowPanel = worldWindowPanel;
            this.flatEarth = flatEarth;
            this.removeExtraLayers = removeExtraLayers;
        }

        @Override
        protected WorldMap3DPanel execute() throws Exception {
            return new WorldMap3DPanel(this.flatEarth, this.removeExtraLayers, this.polygonsLayerModel);
        }

        @Override
        protected String getExceptionLoggingMessage() {
            return "Failed to create the world window panel.";
        }

        @Override
        protected final void successfullyExecuting(WorldMap3DPanel result) {
            GenericRunnable<WorldMap3DPanel> runnable = new GenericRunnable<WorldMap3DPanel>(result) {
                @Override
                protected void execute(WorldMap3DPanel item) {
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
