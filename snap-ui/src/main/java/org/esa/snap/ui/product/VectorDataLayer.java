/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.ui.product;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureChangeEvent;
import com.bc.ceres.swing.figure.FigureChangeListener;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.support.DefaultFigureCollection;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.SceneTransformProvider;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.Debug;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A layer for vector data nodes.
 *
 * @author Norman Fomferra
 */
public class VectorDataLayer extends Layer {

    private static final VectorDataLayerType TYPE = LayerTypeRegistry.getLayerType(VectorDataLayerType.class);
    private VectorDataNode vectorDataNode;
    private final SimpleFeatureFigureFactory figureFactory;
    private FigureCollection figureCollection;
    private VectorDataChangeHandler vectorDataChangeHandler;
    private boolean reactingAgainstFigureChange;

    private static int id;

    public VectorDataLayer(LayerContext ctx, VectorDataNode vectorDataNode, SceneTransformProvider provider) {
        this(TYPE, vectorDataNode, provider, TYPE.createLayerConfig(ctx));
        getConfiguration().setValue(VectorDataLayerType.PROPERTY_NAME_VECTOR_DATA, vectorDataNode.getName());
    }

    protected VectorDataLayer(VectorDataLayerType vectorDataLayerType, VectorDataNode vectorDataNode,
                              SceneTransformProvider provider, PropertySet configuration) {
        super(vectorDataLayerType, configuration);

        setUniqueId();

        this.vectorDataNode = vectorDataNode;
        setName(vectorDataNode.getName());
        figureFactory = new SimpleFeatureFigureFactory(vectorDataNode.getFeatureType(), provider);
        figureCollection = new DefaultFigureCollection();
        updateFigureCollection();

        vectorDataChangeHandler = new VectorDataChangeHandler();
        Product product = vectorDataNode.getProduct();
        if (product != null) {
            product.addProductNodeListener(vectorDataChangeHandler);
        }
        figureCollection.addChangeListener(new FigureChangeHandler());
    }

    private void setUniqueId() {
        setId(VectorDataLayerType.VECTOR_DATA_LAYER_ID_PREFIX + (++id));
    }

    public VectorDataNode getVectorDataNode() {
        return vectorDataNode;
    }

    @Override
    protected void disposeLayer() {
        Product product = vectorDataNode.getProduct();
        if (product != null) {
            product.removeProductNodeListener(vectorDataChangeHandler);
        }
        vectorDataNode = null;
        super.disposeLayer();
    }

    private void updateFigureCollection() {
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = vectorDataNode.getFeatureCollection();

        Figure[] figures = figureCollection.getFigures();
        Map<SimpleFeature, SimpleFeatureFigure> figureMap = new HashMap<>();
        for (Figure figure : figures) {
            if (figure instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure simpleFeatureFigure = (SimpleFeatureFigure) figure;
                figureMap.put(simpleFeatureFigure.getSimpleFeature(), simpleFeatureFigure);
            }
        }

        FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();
        while (featureIterator.hasNext()) {
            SimpleFeature simpleFeature = featureIterator.next();
            SimpleFeatureFigure featureFigure = figureMap.get(simpleFeature);
            if (featureFigure != null) {
                figureMap.remove(simpleFeature);
                Placemark placemark = vectorDataNode.getPlacemarkGroup().getPlacemark(simpleFeature);
                if(placemark != null) {
                    String css = placemark.getStyleCss();
                    final FigureStyle normalStyle = DefaultFigureStyle.createFromCss(css);
                    final FigureStyle selectedStyle = getFigureFactory().deriveSelectedStyle(normalStyle);
                    featureFigure.setNormalStyle(normalStyle);
                    featureFigure.setSelectedStyle(selectedStyle);
                }
            } else {
                featureFigure = getFigureFactory().createSimpleFeatureFigure(simpleFeature, vectorDataNode.getDefaultStyleCss());
                figureCollection.addFigure(featureFigure);
            }
            featureFigure.forceRegeneration();
        }

        Collection<SimpleFeatureFigure> remainingFigures = figureMap.values();
        figureCollection.removeFigures(remainingFigures.toArray(new Figure[remainingFigures.size()]));

    }

    private void setLayerStyle(String styleCss) {
        // todo - implement me (nf)
        // this method is called if no figure is selected, but the layer editor is showing and users can modify style settings
        Debug.trace("VectorDataLayer.setLayerStyle: styleCss = " + styleCss);
    }

    public SimpleFeatureFigureFactory getFigureFactory() {
        return figureFactory;
    }

    public FigureCollection getFigureCollection() {
        return figureCollection;
    }

    @Override
    protected Rectangle2D getLayerModelBounds() {
        if (figureCollection.getFigureCount() == 0) {
            return null;
        } else {
            return figureCollection.getBounds();
        }
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        figureCollection.draw(rendering);
    }

    private class VectorDataChangeHandler extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getSourceNode() == getVectorDataNode()) {
                Debug.trace("VectorDataLayer$VectorDataChangeHandler.nodeChanged: event = " + event);
                if (ProductNode.PROPERTY_NAME_NAME.equals(event.getPropertyName())) {
                    setName(getVectorDataNode().getName());
                } else if (VectorDataNode.PROPERTY_NAME_STYLE_CSS.equals(event.getPropertyName())) {
                    if (event.getNewValue() != null) {
                        setLayerStyle(event.getNewValue().toString());
                    }
                } else if (VectorDataNode.PROPERTY_NAME_FEATURE_COLLECTION.equals(event.getPropertyName())) {
                    if (!reactingAgainstFigureChange) {
                        updateFigureCollection();
                        // checkme - we could do better by computing changed modelRegion instead of passing null (nf)
                        fireLayerDataChanged(null);
                    }
                }
            } else if (event.getSourceNode() instanceof Placemark) {
                final Placemark sourceNode = (Placemark) event.getSourceNode();
                if (getVectorDataNode().getPlacemarkGroup().contains(sourceNode))
                    if (event.getPropertyName().equals(Placemark.PROPERTY_NAME_STYLE_CSS)) {
                        updateFigureCollection();
                    } else if (event.getPropertyName().equals("geometry")) {
                        updateFigureCollection();
                    } else if (event.getPropertyName().equals(Placemark.PROPERTY_NAME_GEOPOS)) {
                        updateFigureCollection();
                    } else if (event.getPropertyName().equals(Placemark.PROPERTY_NAME_PIXELPOS)) {
                        updateFigureCollection();
                    }
            }
        }
    }

    private class FigureChangeHandler implements FigureChangeListener {

        @Override
        public void figureChanged(FigureChangeEvent event) {
            final Figure sourceFigure = event.getSourceFigure();
            if (sourceFigure instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure featureFigure = (SimpleFeatureFigure) sourceFigure;
                try {
                    final VectorDataNode vectorDataNode = getVectorDataNode();
                    if (vectorDataNode != null ) {
                        final SimpleFeature simpleFeature = featureFigure.getSimpleFeature();
                        Debug.trace("VectorDataLayer$FigureChangeHandler: vectorDataNode=" + vectorDataNode.getName() +
                                            ", featureType=" + simpleFeature.getFeatureType().getTypeName());
                        reactingAgainstFigureChange = true;
                        vectorDataNode.fireFeaturesChanged(simpleFeature);
                        // checkme - we could do better by computing changed modelRegion instead of passing null (nf)
                        fireLayerDataChanged(null);
                    }
                } finally {
                    reactingAgainstFigureChange = false;
                }
            }
        }
    }
}
