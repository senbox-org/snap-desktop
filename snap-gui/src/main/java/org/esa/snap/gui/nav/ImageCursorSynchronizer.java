/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.gui.nav;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.actions.view.SyncImageCursorsAction;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.gui.windows.ProductSceneViewTopComponent;
import org.openide.util.WeakListeners;
import org.openide.windows.OnShowing;
import org.openide.windows.TopComponent;

import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

/**
 * @author Marco Peters, Norman Fomferra
 */
@OnShowing
public class ImageCursorSynchronizer implements Runnable, PreferenceChangeListener {

    public static final String PROPERTY_KEY_AUTO_SYNC_CURSORS = SyncImageCursorsAction.PREFERENCE_KEY;
    private static final GeoPos INVALID_GEO_POS = new GeoPos(Float.NaN, Float.NaN);

    private Map<ProductSceneView, ImageCursorOverlay> psvOverlayMap;
    private Map<ProductSceneView, MyPixelPositionListener> viewPplMap;
    private PsvListUpdater psvOverlayMapUpdater;

    @Override
    public void run() {
        psvOverlayMap = new WeakHashMap<>();
        viewPplMap = new WeakHashMap<>();
        psvOverlayMapUpdater = new PsvListUpdater();

        Preferences preferences = SnapApp.getDefault().getPreferences();
        preferences.addPreferenceChangeListener(WeakListeners.create(PreferenceChangeListener.class, this, preferences));
    }

    private boolean isActive() {
        return SnapApp.getDefault().getPreferences().getBoolean(PROPERTY_KEY_AUTO_SYNC_CURSORS,
                                                                SyncImageCursorsAction.PREFERENCE_DEFAULT_VALUE);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        if (PROPERTY_KEY_AUTO_SYNC_CURSORS.equals(evt.getKey())) {
            if (isActive()) {
                initPsvOverlayMap();
                DocumentWindowManager.getDefault().addListener(psvOverlayMapUpdater);
            } else {
                DocumentWindowManager.getDefault().removeListener(psvOverlayMapUpdater);
                clearPsvOverlayMap();
            }
        }
    }

    public void updateCursorOverlays(GeoPos geoPos, ProductSceneView sourceView) {
        if (!isActive()) {
            return;
        }
        for (Map.Entry<ProductSceneView, ImageCursorOverlay> entry : psvOverlayMap.entrySet()) {
            final ProductSceneView view = entry.getKey();
            ImageCursorOverlay overlay = entry.getValue();
            if (overlay == null) {
                if (view != sourceView) {
                    overlay = new ImageCursorOverlay(view, geoPos);
                    psvOverlayMap.put(view, overlay);
                    view.getLayerCanvas().addOverlay(overlay);
                }
            } else {
                if (view != sourceView) {
                    overlay.setGeoPosition(geoPos);
                    view.getLayerCanvas().repaint();
                } else {
                    view.getLayerCanvas().removeOverlay(overlay);
                    psvOverlayMap.put(view, null);
                }
            }
        }
    }

    private void initPsvOverlayMap() {
        WindowUtilities.getOpened(ProductSceneViewTopComponent.class)
                .map(ProductSceneViewTopComponent::getView)
                .forEach(this::addPPL);
    }

    private void clearPsvOverlayMap() {
        for (Map.Entry<ProductSceneView, ImageCursorOverlay> entry : psvOverlayMap.entrySet()) {
            final ProductSceneView view = entry.getKey();
            removePPL(view);
            view.getLayerCanvas().removeOverlay(entry.getValue());
        }
        psvOverlayMap.clear();
    }

    private void addPPL(ProductSceneView view) {
        GeoCoding geoCoding = view.getProduct().getGeoCoding();
        if (geoCoding != null && geoCoding.canGetPixelPos()) {
            psvOverlayMap.put(view, null);
            MyPixelPositionListener ppl = new MyPixelPositionListener(view);
            viewPplMap.put(view, ppl);
            view.addPixelPositionListener(ppl);
        }
    }

    private void removePPL(ProductSceneView view) {
        MyPixelPositionListener ppl = viewPplMap.get(view);
        if (ppl != null) {
            viewPplMap.remove(view);
            view.removePixelPositionListener(ppl);
        }
    }

    private class PsvListUpdater implements DocumentWindowManager.Listener {

        @Override
        public void windowOpened(DocumentWindowManager.Event e) {
            TopComponent topComponent = e.getDocumentWindow().getTopComponent();
            if (topComponent instanceof ProductSceneViewTopComponent) {
                ProductSceneView view = ((ProductSceneViewTopComponent) topComponent).getView();
                addPPL(view);
            }
        }

        @Override
        public void windowClosed(DocumentWindowManager.Event e) {
            TopComponent topComponent = e.getDocumentWindow().getTopComponent();
            if (topComponent instanceof ProductSceneViewTopComponent) {
                ProductSceneView view = ((ProductSceneViewTopComponent) topComponent).getView();
                removePPL(view);
            }
        }

        @Override
        public void windowSelected(DocumentWindowManager.Event e) {
        }

        @Override
        public void windowDeselected(DocumentWindowManager.Event e) {
        }
    }

    private class MyPixelPositionListener implements PixelPositionListener {

        private final ProductSceneView view;

        private MyPixelPositionListener(ProductSceneView view) {
            this.view = view;
        }

        @Override
        public void pixelPosChanged(ImageLayer baseImageLayer, int pixelX, int pixelY, int currentLevel,
                                    boolean pixelPosValid, MouseEvent e) {
            PixelPos pixelPos = computeLevelZeroPixelPos(baseImageLayer, pixelX, pixelY, currentLevel);
            GeoPos geoPos = view.getRaster().getGeoCoding().getGeoPos(pixelPos, null);
            updateCursorOverlays(geoPos, view);
        }

        private PixelPos computeLevelZeroPixelPos(ImageLayer imageLayer, int pixelX, int pixelY, int currentLevel) {
            if (currentLevel != 0) {
                AffineTransform i2mTransform = imageLayer.getImageToModelTransform(currentLevel);
                Point2D modelP = i2mTransform.transform(new Point2D.Double(pixelX + 0.5, pixelY + 0.5), null);
                AffineTransform m2iTransform = imageLayer.getModelToImageTransform();
                Point2D imageP = m2iTransform.transform(modelP, null);

                return new PixelPos(new Float(imageP.getX()), new Float(imageP.getY()));
            } else {
                return new PixelPos(pixelX + 0.5f, pixelY + 0.5f);
            }
        }

        @Override
        public void pixelPosNotAvailable() {
            updateCursorOverlays(INVALID_GEO_POS, null);
        }
    }
}
