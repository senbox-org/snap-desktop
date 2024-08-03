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

package org.esa.snap.timeseries.ui.player;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.image.ColoredBandImageMultiLevelSource;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.esa.snap.timeseries.core.TimeSeriesMapper;
import org.esa.snap.timeseries.core.TimeSeriesModule;
import org.esa.snap.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.snap.timeseries.core.timeseries.datamodel.TimeSeriesChangeEvent;
import org.esa.snap.timeseries.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.snap.ui.product.ProductSceneImage;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Main class for the player tool.
 *
 * @author Thomas Storm
 * @author Marco Peters
 */
@TopComponent.Description(
        preferredID = "TimeSeriesPlayerTopComponent",
        iconBase = "org/esa/snap/timeseries/ui/icons/timeseries-player.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = false,
        position = 3
)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_TimeSeriesPlayerTopComponentName",
        preferredID = "TimeSeriesPlayerTopComponent"
)
@ActionID(category = "Window", id = "org.esa.snap.timeseries.ui.player.TimeSeriesPlayerTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Raster/Time Series", position = 1230),
        @ActionReference(path = "Toolbars/Time Series", position = 30)
})
@NbBundle.Messages({
        "CTL_TimeSeriesPlayerTopComponentName=Time Series Player"
})
public class TimeSeriesPlayerTopComponent extends TopComponent {

    private static final String HELP_ID = "timeSeriesPlayer";

    private final TimeSeriesListener timeSeriesPlayerTSL;
    private ProductSceneView currentView;

    private TimeSeriesPlayerForm form;

    public TimeSeriesPlayerTopComponent() {
        initComponent();
        timeSeriesPlayerTSL = new TimeSeriesPlayerTSL();
        SnapApp.getDefault().getSelectionSupport(ProductSceneView.class).addHandler(new SceneViewSelectionChangeHandler());
    }


    private void initComponent() {
        form = new TimeSeriesPlayerForm(HELP_ID);
        form.getTimeSlider().addChangeListener(new SliderChangeListener());
        ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        if (view != null) {
            maybeUpdateCurrentView(view, view.getProduct().getProductType());
        }
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(form, BorderLayout.CENTER);
        setDisplayName(Bundle.CTL_TimeSeriesPlayerTopComponentName());
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }

    private void setCurrentView(ProductSceneView newView) {
        if (currentView != newView) {
            TimeSeriesMapper timeSeriesMapper = TimeSeriesMapper.getInstance();
            if (currentView != null) {
                final AbstractTimeSeries timeSeries = timeSeriesMapper.getTimeSeries(currentView.getProduct());
                if (timeSeries != null) {
                    timeSeries.removeTimeSeriesListener(timeSeriesPlayerTSL);
                }
            }
            currentView = newView;
            form.setView(currentView);
            if (currentView != null) {
                final Product currentProduct = currentView.getProduct();
                final AbstractTimeSeries timeSeries = timeSeriesMapper.getTimeSeries(currentProduct);
                timeSeries.addTimeSeriesListener(timeSeriesPlayerTSL);
                form.setTimeSeries(timeSeries);
                exchangeRasterInProductSceneView(currentView.getRaster());
                reconfigureBaseImageLayer(currentView);
                form.configureTimeSlider(currentView.getRaster());
            } else {
                form.setTimeSeries(null);
                form.configureTimeSlider(null);
                form.getTimer().stop();
            }
        }
    }


    private void exchangeRasterInProductSceneView(RasterDataNode nextRaster) {
        // todo use a real ProgressMonitor
        final RasterDataNode currentRaster = currentView.getRaster();
        final ImageInfo imageInfoClone = currentRaster.getImageInfo(ProgressMonitor.NULL).createDeepCopy();
        nextRaster.setImageInfo(imageInfoClone);
        currentView.setRasters(new RasterDataNode[]{nextRaster});
        currentView.setImageInfo(imageInfoClone.createDeepCopy());

        getTCForView(currentView).setDisplayName(nextRaster.getDisplayName());
    }

    private TopComponent getTCForView(ProductSceneView view) {
        return WindowUtilities.getOpened(ProductSceneViewTopComponent.class)
                .filter(topComponent -> view == topComponent.getView())
                .findFirst()
                .orElse(null);
    }

    private void reconfigureBaseImageLayer(ProductSceneView sceneView) {
        final Layer rootLayer = currentView.getRootLayer();
        final ImageLayer baseImageLayer = (ImageLayer) LayerUtils.getChildLayerById(rootLayer,
                ProductSceneView.BASE_IMAGE_LAYER_ID);
        final List<Band> bandList = form.getBandList(currentView.getRaster().getName());
        final Band band = (Band) sceneView.getRaster();
        int nextIndex = bandList.indexOf(band) + 1;
        if (nextIndex >= bandList.size()) {
            nextIndex = 0;
        }

        if (!(baseImageLayer instanceof BlendImageLayer)) {
            final Band nextBand = bandList.get(nextIndex);
            MultiLevelSource nextLevelSource = ColoredBandImageMultiLevelSource.create(nextBand, ProgressMonitor.NULL);
            final BlendImageLayer blendLayer = new BlendImageLayer(baseImageLayer.getMultiLevelSource(),
                    nextLevelSource);

            final List<Layer> children = rootLayer.getChildren();
            final int baseIndex = children.indexOf(baseImageLayer);
            children.remove(baseIndex);
            blendLayer.setId(ProductSceneView.BASE_IMAGE_LAYER_ID);
            blendLayer.setName(band.getDisplayName());
            blendLayer.setTransparency(0);
            children.add(baseIndex, blendLayer);
            configureSceneView(sceneView, blendLayer.getBaseMultiLevelSource());
        }
    }

    private void configureSceneView(ProductSceneView sceneView, MultiLevelSource multiLevelSource) {
        // This is needed because sceneView must return correct ImageInfo
        try {
            final Field sceneImageField = ProductSceneView.class.getDeclaredField("sceneImage");
            sceneImageField.setAccessible(true);
            final Object sceneImage = sceneImageField.get(sceneView);
            final Field multiLevelSourceField = ProductSceneImage.class.getDeclaredField("bandImageMultiLevelSource");
            multiLevelSourceField.setAccessible(true);
            multiLevelSourceField.set(sceneImage, multiLevelSource);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private class SceneViewSelectionChangeHandler implements SelectionSupport.Handler<ProductSceneView> {

        @Override
        public void selectionChange(ProductSceneView oldValue, ProductSceneView newValue) {
            if (currentView == oldValue) {
                setCurrentView(null);
            }
            if (currentView != newValue) {
                if (newValue != null) {
                    final RasterDataNode viewRaster = newValue.getRaster();
                    final String viewProductType = viewRaster.getProduct().getProductType();
                    maybeUpdateCurrentView(newValue, viewProductType);
                } else {
                    setCurrentView(null);
                }
            }
        }
    }


    private void maybeUpdateCurrentView(ProductSceneView view, String viewProductType) {
        if (!view.isRGB() &&
                viewProductType.equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE) &&
                TimeSeriesMapper.getInstance().getTimeSeries(view.getProduct()) != null) {
            setCurrentView(view);
        }
    }

    private class SliderChangeListener implements ChangeListener {

        private int value;

        @Override
        public void stateChanged(ChangeEvent e) {
            if (currentView == null) {
                return;
            }
            final int currentValue = form.getTimeSlider().getValue();
            if (currentValue == value || currentValue == -1) {
                // nothing has changed -- do nothing
                return;
            }
            if (currentView.getBaseImageLayer() instanceof BlendImageLayer) {
                BlendImageLayer blendLayer = (BlendImageLayer) currentView.getBaseImageLayer();
                int stepsPerTimespan = form.getStepsPerTimespan();
                final float transparency = (currentValue % stepsPerTimespan) / (float) stepsPerTimespan;
                blendLayer.setBlendFactor(transparency);
                final List<Band> bandList = form.getBandList(currentView.getRaster().getName());
                value = currentValue;
                final int firstBandIndex = MathUtils.floorInt(currentValue / (float) stepsPerTimespan);
                final int secondBandIndex = MathUtils.ceilInt(currentValue / (float) stepsPerTimespan);
                ColoredBandImageMultiLevelSource newSource =
                        ColoredBandImageMultiLevelSource.create(bandList.get(secondBandIndex), ProgressMonitor.NULL);
                if (secondBandIndex == firstBandIndex) {
                    exchangeRasterInProductSceneView(bandList.get(firstBandIndex));
                    blendLayer.setBaseLayer(newSource);

                    configureSceneView(currentView, blendLayer.getBaseMultiLevelSource());
                    blendLayer.setName(currentView.getRaster().getDisplayName());
//                 todo why use view to fire property changes and not time series itself?
                    currentView.firePropertyChange(TimeSeriesModule.TIME_PROPERTY, -1, firstBandIndex);
                } else {
                    if (transparency == (float) 1 / stepsPerTimespan) {
                        blendLayer.setBlendLayer(newSource);
                    }
                    currentView.getLayerCanvas().repaint();
                }
            }
        }

    }

    private class TimeSeriesPlayerTSL extends TimeSeriesListener {

        @Override
        public void timeSeriesChanged(TimeSeriesChangeEvent event) {
            if (event.getType() == TimeSeriesChangeEvent.PROPERTY_PRODUCT_LOCATIONS ||
                    event.getType() == TimeSeriesChangeEvent.PROPERTY_EO_VARIABLE_SELECTION) {
                form.configureTimeSlider(currentView.getRaster());
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            final ProductNode productNode = event.getSourceNode();
            if (isValidProductNode(productNode) && currentView != null) {
                form.configureTimeSlider((RasterDataNode) productNode);
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            final ProductNode productNode = event.getSourceNode();
            if (isValidProductNode(productNode) && currentView != null) {
                if (currentView.getRaster() == productNode) {
                    form.configureTimeSlider((RasterDataNode) productNode);
                }
            }
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            String propertyName = event.getPropertyName();
            if (propertyName.equals(RasterDataNode.PROPERTY_NAME_IMAGE_INFO)) {
                adjustImageInfos(event);
            }
        }


        private boolean isValidProductNode(ProductNode productNode) {
            return productNode instanceof RasterDataNode && !(productNode instanceof Mask);
        }

        private void adjustImageInfos(ProductNodeEvent event) {

            final ProductNode node = event.getSourceNode();
            if (isValidProductNode(node)) {
                final RasterDataNode raster = (RasterDataNode) node;
                final ImageLayer baseImageLayer = currentView.getBaseImageLayer();
                final ImageInfo imageInfo = raster.getImageInfo();
                if (baseImageLayer instanceof BlendImageLayer) {
                    BlendImageLayer blendLayer = (BlendImageLayer) baseImageLayer;
                    blendLayer.getBlendMultiLevelSource().setImageInfo(imageInfo.createDeepCopy());
                }

            }
        }
    }

}
