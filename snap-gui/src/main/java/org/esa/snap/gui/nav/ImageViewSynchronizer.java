package org.esa.snap.gui.nav;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.swing.LayerCanvasModel;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.actions.view.SyncImageViewsAction;
import org.esa.snap.gui.windows.ProductSceneViewTopComponent;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.windows.OnShowing;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * @author Norman
 */
@OnShowing
public class ImageViewSynchronizer implements Runnable, PreferenceChangeListener, LookupListener {

    public static final String PROPERTY_KEY_AUTO_SYNC_VIEWS = SyncImageViewsAction.PREFERENCE_KEY;

    private ProductSceneView lastView;
    private LayerCanvasModelChangeHandler layerCanvasModelChangeHandler;

    @Override
    public void run() {

        layerCanvasModelChangeHandler = new LayerCanvasModelChangeHandler();

        Preferences preferences = SnapApp.getDefault().getPreferences();
        preferences.addPreferenceChangeListener(WeakListeners.create(PreferenceChangeListener.class, this, preferences));

        Lookup.Result<ProductSceneView> lookupResult = Utilities.actionsGlobalContext().lookupResult(ProductSceneView.class);
        lookupResult.addLookupListener(WeakListeners.create(LookupListener.class, this, lookupResult));

        syncImageViewsWithSelectedView();
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        if (PROPERTY_KEY_AUTO_SYNC_VIEWS.equals(evt.getKey())) {
            syncImageViewsWithSelectedView();
        }
    }

    @Override
    public void resultChanged(LookupEvent ev) {

        ProductSceneView newView = SnapApp.getDefault().getSelectedProductSceneView();

        if (lastView != newView) {
            final ProductSceneView oldView = lastView;
            if (oldView != null) {
                if (oldView.getLayerCanvas() != null) {
                    oldView.getLayerCanvas().getModel().removeChangeListener(layerCanvasModelChangeHandler);
                }
            }
            lastView = newView;
            if (lastView != null) {
                syncImageViews(lastView);
                if (lastView.getLayerCanvas() != null) {
                    lastView.getLayerCanvas().getModel().addChangeListener(layerCanvasModelChangeHandler);
                }
            }
        }
    }

    private void syncImageViewsWithSelectedView() {
        ProductSceneView currentSceneView = SnapApp.getDefault().getSelectedProductSceneView();
        if (currentSceneView != null) {
            syncImageViews(currentSceneView);
        }
    }

    private void syncImageViews(ProductSceneView currentSceneView) {
        if (isActive()) {
            WindowUtilities.getOpened(ProductSceneViewTopComponent.class).forEach(topComponent -> {
                ProductSceneView oldSceneView = topComponent.getView();
                if (oldSceneView != currentSceneView) {
                    currentSceneView.synchronizeViewportIfPossible(oldSceneView);
                }
            });
        }
    }

    private boolean isActive() {
        return SnapApp.getDefault().getPreferences().getBoolean(PROPERTY_KEY_AUTO_SYNC_VIEWS,
                                                                SyncImageViewsAction.PREFERENCE_DEFAULT_VALUE);
    }

    private class LayerCanvasModelChangeHandler implements LayerCanvasModel.ChangeListener {

        @Override
        public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
        }

        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
        }

        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
        }

        @Override
        public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
        }

        @Override
        public void handleViewportChanged(Viewport viewport, boolean orientationChanged) {
            syncImageViewsWithSelectedView();
        }
    }
}
