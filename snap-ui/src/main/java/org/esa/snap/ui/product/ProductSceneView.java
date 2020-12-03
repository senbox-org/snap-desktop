/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.glayer.swing.AdjustableViewScrollPane;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.ViewportAware;
import com.bc.ceres.grender.support.DefaultViewport;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureChangeListener;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorAware;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.Handle;
import com.bc.ceres.swing.figure.ShapeFigure;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.undo.UndoContext;
import com.bc.ceres.swing.undo.support.DefaultUndoContext;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.PlacemarkGroup;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListener;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.image.ColoredMaskImageMultiLevelSource;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.layer.GraticuleLayer;
import org.esa.snap.core.layer.MaskCollectionLayer;
import org.esa.snap.core.layer.NoDataLayerType;
import org.esa.snap.core.layer.ProductLayerContext;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.ui.*;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.opengis.referencing.operation.TransformException;
import org.openide.util.Utilities;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import javax.swing.undo.UndoManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * The class {@code ProductSceneView} is a high-level image display component for color index/RGB images created
 * from one or more raster datasets of a data product.
 * <p>
 * <p>It is also capable of displaying a graticule (geographical grid) and a ROI associated with a displayed raster
 * dataset.
 *
 * @author Norman Fomferra
 */
public class ProductSceneView extends BasicView
        implements FigureEditorAware, ProductNodeView, PropertyChangeListener, ProductLayerContext, ViewportAware {

    public static final String BASE_IMAGE_LAYER_ID = "org.esa.snap.layers.baseImage";
    public static final String NO_DATA_LAYER_ID = "org.esa.snap.layers.noData";
    public static final String VECTOR_DATA_LAYER_ID = VectorDataCollectionLayer.ID;
    public static final String MASKS_LAYER_ID = MaskCollectionLayer.ID;
    public static final String GRATICULE_LAYER_ID = "org.esa.snap.layers.graticule";

    /**
     * Property name for the pixel border
     */
    public static final String PREFERENCE_KEY_PIXEL_BORDER_SHOWN = "pixel.border.shown";
    /**
     * Name of property which switches display of af a navigation control in the image view.
     */
    public static final String PREFERENCE_KEY_IMAGE_NAV_CONTROL_SHOWN = "image.navControlShown";
    /**
     * Name of property which switches display of af a navigation control in the image view.
     */
    public static final String PREFERENCE_KEY_IMAGE_SCROLL_BARS_SHOWN = "image.scrollBarsShown";
    /**
     * Name of property which inverts the zooming with the mouse wheel.
     */
    public static final String PREFERENCE_KEY_INVERT_ZOOMING = "image.reverseZooming";

    /**
     * Name of property of image info
     */
    public static final String PROPERTY_NAME_IMAGE_INFO = "imageInfo";

    /**
     * Name of property of selected layer
     */
    public static final String PROPERTY_NAME_SELECTED_LAYER = "selectedLayer";

    /**
     * Name of property of selected pin
     */
    public static final String PROPERTY_NAME_SELECTED_PIN = "selectedPin";
    public static final Color DEFAULT_IMAGE_BACKGROUND_COLOR = PackageDefaults.IMAGE_BACKGROUND_COLOR;


    private ProductSceneImage sceneImage;
    private LayerCanvas layerCanvas;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Properties corresponding to the base image displaying the raster data returned by #getRaster()
    //
    // layer which displays the base image
    private final ImageLayer baseImageLayer;
    // current resolution level of the base image
    private int currentLevel = 0;
    // current pixel X (from mouse cursor) at current resolution level of the base image
    private int currentLevelPixelX = -1;
    // current pixel Y (from mouse cursor) at current resolution level of the base image
    private int currentLevelPixelY = -1;
    // current pixel X (from mouse cursor) at highest resolution level of the base image
    private int currentPixelX = -1;
    // current pixel Y (from mouse cursor) at highest resolution level of the base image
    private int currentPixelY = -1;
    // display properties for the current pixel (from mouse cursor)
    private boolean pixelBorderShown; // can it be shown?
    private boolean pixelBorderDrawn; // has it been drawn?
    private double pixelBorderViewScale;
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Vector<PixelPositionListener> pixelPositionListeners;

    private Layer selectedLayer;
    private ComponentAdapter layerCanvasComponentHandler;
    private LayerCanvasMouseHandler layerCanvasMouseHandler;
    private RasterChangeHandler rasterChangeHandler;
    private boolean scrollBarsShown;

    private AdjustableViewScrollPane scrollPane;
    private UndoContext undoContext;
    private VectorDataFigureEditor figureEditor;

    public ProductSceneView(ProductSceneImage sceneImage) {
        this(sceneImage, new UndoManager());
    }

    public ProductSceneView(ProductSceneImage sceneImage, UndoManager undoManager) {
        Assert.notNull(sceneImage, "sceneImage");

        setOpaque(true);
        setLayout(new BorderLayout());
        // todo - use sceneImage.getConfiguration() (nf, 18.09.2008)
        setBackground(DEFAULT_IMAGE_BACKGROUND_COLOR);

        this.pixelBorderShown = sceneImage.getConfiguration().getPropertyBool(PREFERENCE_KEY_PIXEL_BORDER_SHOWN, true);

        this.sceneImage = sceneImage;
        this.baseImageLayer = sceneImage.getBaseImageLayer();
        this.pixelBorderViewScale = 2.0;
        this.pixelPositionListeners = new Vector<>();

        undoContext = new DefaultUndoContext(this, undoManager);

        DefaultViewport viewport = new DefaultViewport(isModelYAxisDown(baseImageLayer));

        final Layer rootLayer = sceneImage.getRootLayer();
        this.layerCanvas = new LayerCanvas(rootLayer, viewport);
        rootLayer.addListener(new AbstractLayerListener() {
            @Override
            public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
                for (Layer childLayer : childLayers) {
                    if (childLayer == selectedLayer) {
                        setSelectedLayer(null);
                        return;
                    }
                }
            }
        });
        final boolean navControlShown = sceneImage.getConfiguration().getPropertyBool(
                PREFERENCE_KEY_IMAGE_NAV_CONTROL_SHOWN, true);


        this.layerCanvas.setNavControlShown(navControlShown);
        this.layerCanvas.setAntialiasing(true);
        this.layerCanvas.setPreferredSize(new Dimension(400, 400));
        this.layerCanvas.addOverlay((canvas, rendering) -> {
            figureEditor.drawFigureSelection(rendering);
            figureEditor.drawSelectionRectangle(rendering);
        });

        figureEditor = new VectorDataFigureEditor(this);
        figureEditor.addSelectionChangeListener(new PinSelectionChangeListener());

        this.scrollBarsShown = sceneImage.getConfiguration().getPropertyBool(PREFERENCE_KEY_IMAGE_SCROLL_BARS_SHOWN,
                false);
        if (scrollBarsShown) {
            this.scrollPane = createScrollPane();
            add(scrollPane, BorderLayout.CENTER);
        } else {
            add(layerCanvas, BorderLayout.CENTER);
        }

        registerLayerCanvasListeners();

        this.rasterChangeHandler = new RasterChangeHandler();
        getRaster().getProduct().addProductNodeListener(rasterChangeHandler);

        setMaskOverlayEnabled(true);
        setName(sceneImage.getName());

        appyLayerProperties(sceneImage.getConfiguration());
        sceneImage.getConfiguration().addPropertyChangeListener(this);

        addDefaultLayers(sceneImage);
    }

    private void addDefaultLayers(final ProductSceneImage sceneImage) {
        final Layer rootLayer = sceneImage.getRootLayer();

        final Set<LayerType> layerTypes = LayerTypeRegistry.getLayerTypes();
        for(LayerType layerType : layerTypes) {
            if(layerType.isValidFor(sceneImage) && layerType.createWithSceneView(sceneImage)) {
                PropertyContainer config = new PropertyContainer();
                config.addProperty(Property.create("raster", getRaster()));
                Layer layer = layerType.createLayer(sceneImage, config);
                rootLayer.getChildren().add(0, layer);
                layer.setVisible(true);
            }
        }
    }

    /**
     * Called if the property map changed. Simply calls {@link #appyLayerProperties(PropertyMap)}.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        appyLayerProperties(sceneImage.getConfiguration());
    }

    public UndoContext getUndoContext() {
        return undoContext;
    }

    @Override
    public FigureEditor getFigureEditor() {
        return figureEditor;
    }

    @Override
    public Viewport getViewport() {
        return layerCanvas.getViewport();
    }

    public int getCurrentPixelX() {
        return currentPixelX;
    }

    public int getCurrentPixelY() {
        return currentPixelY;
    }

    public boolean isCurrentPixelPosValid() {
        return isPixelPosValid(currentLevelPixelX, currentLevelPixelY, currentLevel);
    }

    private AdjustableViewScrollPane createScrollPane() {
        AbstractButton zoomAllButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomAll13.gif"),
                false);
        zoomAllButton.setFocusable(false);
        zoomAllButton.setFocusPainted(false);
        zoomAllButton.addActionListener(e -> getLayerCanvas().zoomAll());

        AdjustableViewScrollPane scrollPane = new AdjustableViewScrollPane(layerCanvas);
        // todo - use sceneImage.getConfiguration() (nf, 18.09.2008)
        scrollPane.setBackground(DEFAULT_IMAGE_BACKGROUND_COLOR);
        scrollPane.setCornerComponent(zoomAllButton);
        return scrollPane;
    }

    public ProductSceneImage getSceneImage() {
        return sceneImage;
    }

    /**
     * Gets the current selection context, if any.
     *
     * @return The current selection context, or {@code null} if none exists.
     * @since BEAM 4.7
     */
    @Override
    public SelectionContext getSelectionContext() {
        return getFigureEditor().getSelectionContext();
    }

    /**
     * @return The root layer.
     */
    @Override
    public Layer getRootLayer() {
        return sceneImage.getRootLayer();
    }

    /**
     * The coordinate reference system (CRS) used by all the layers in this context.
     * May be used by a {@link com.bc.ceres.glayer.LayerType} in order to decide whether
     * the source can provide a new layer instance for this context.
     *
     * @return The CRS. May be {@code null}.
     */
    @Override
    public Object getCoordinateReferenceSystem() {
        return sceneImage.getCoordinateReferenceSystem();
    }

    /**
     * @deprecated since BEAM 4.7
     */
    @Deprecated
    public LayerContext getLayerContext() {
        return sceneImage;
    }

    public LayerCanvas getLayerCanvas() {
        return layerCanvas;
    }

    /**
     * Returns the currently visible product node.
     */
    @Override
    public ProductNode getVisibleProductNode() {
        if (isRGB()) {
            return getProduct();
        }
        return getRaster();
    }

    /**
     * If the {@code preferredSize} has been set to a
     * non-{@code null} value just returns it.
     * If the UI delegate's {@code getPreferredSize}
     * method returns a non {@code null} value then return that;
     * otherwise defer to the component's layout manager.
     *
     * @return the value of the {@code preferredSize} property
     * @see #setPreferredSize
     * @see javax.swing.plaf.ComponentUI
     */
    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        } else {
            if (getLayerCanvas() != null) {
                return getLayerCanvas().getPreferredSize();
            } else {
                return super.getPreferredSize();
            }
        }
    }

    @Override
    public JPopupMenu createPopupMenu(Component component) {
        return null;
    }

    @Override
    public JPopupMenu createPopupMenu(MouseEvent event) {
        JPopupMenu popupMenu = new JPopupMenu();
        List<? extends Action> viewActions = Utilities.actionsForPath("Context/ProductSceneView");
        for (Action action : viewActions) {
            JMenuItem menuItem = popupMenu.add(action);
            String popupText = (String) action.getValue("popupText");
            if (StringUtils.isNotNullAndNotEmpty(popupText)) {
                menuItem.setText(popupText);
            }
        }
        return popupMenu;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to {@code dispose()} are undefined.
     * <p>
     * <p>Overrides of this method should always call {@code super.dispose();} after disposing this instance.
     */
    @Override
    public synchronized void dispose() {
        if (pixelPositionListeners != null) {
            pixelPositionListeners.clear();
        }

        deregisterLayerCanvasListeners();
        if (sceneImage != null) {
            sceneImage.getConfiguration().removePropertyChangeListener(this);
        }

        for (int i = 0; i < getSceneImage().getRasters().length; i++) {
            final RasterDataNode raster = getSceneImage().getRasters()[i];
            if (raster instanceof RGBChannel) {
                RGBChannel rgbChannel = (RGBChannel) raster;
                rgbChannel.dispose();
            }
            sceneImage.getRasters()[i] = null;
        }

        sceneImage = null;

        if (getLayerCanvas() != null) {
            // ensure that imageDisplay.dispose() is run in the EDT
            SwingUtilities.invokeLater(this::disposeImageDisplayComponent);
        }

        super.dispose();
    }

    /**
     * @return the associated product.
     */
    @Override
    public Product getProduct() {
        return getRaster().getProduct();
    }

    @Override
    public ProductNode getProductNode() {
        return getRaster();
    }

    public String getSceneName() {
        return getSceneImage().getName();
    }

    public ImageInfo getImageInfo() {
        return getSceneImage().getImageInfo();
    }

    public void setImageInfo(ImageInfo imageInfo) {
        final ImageInfo oldImageInfo = getImageInfo();
        getSceneImage().setImageInfo(imageInfo);
        updateImage();
        firePropertyChange(PROPERTY_NAME_IMAGE_INFO, oldImageInfo, imageInfo);
    }

    /**
     * Gets the number of raster datasets.
     *
     * @return the number of raster datasets, always {@code 1} for single banded palette images or {@code 3}
     * for RGB images
     */
    public int getNumRasters() {
        return getSceneImage().getRasters().length;
    }

    /**
     * Gets the product raster with the specified index.
     *
     * @param index the zero-based product raster index
     * @return the product raster with the given index
     */
    public RasterDataNode getRaster(int index) {
        return getSceneImage().getRasters()[index];
    }

    /**
     * Gets the product raster of a single banded view.
     *
     * @return the product raster, in case of a 3-banded RGB view it returns the first raster.
     *
     * @see #isRGB()
     */
    public RasterDataNode getRaster() {
        return getSceneImage().getRasters()[0];
    }

    /**
     * Gets all rasters of this view.
     *
     * @return all rasters of this view, array size is either 1 or 3 (RGB)
     */
    public RasterDataNode[] getRasters() {
        return getSceneImage().getRasters();
    }

    public void setRasters(RasterDataNode[] rasters) {
        getSceneImage().setRasters(rasters);
    }

    public boolean isRGB() {
        return getSceneImage().getRasters().length >= 3;
    }

    public boolean isNoDataOverlayEnabled() {
        final Layer noDataLayer = getNoDataLayer(false);
        return noDataLayer != null && noDataLayer.isVisible();
    }

    public void setNoDataOverlayEnabled(boolean enabled) {
        if (isNoDataOverlayEnabled() != enabled) {
            getNoDataLayer(true).setVisible(enabled);
        }
    }

    public ImageLayer getBaseImageLayer() {
        return getSceneImage().getBaseImageLayer();
    }

    public boolean isGraticuleOverlayEnabled() {
        final GraticuleLayer graticuleLayer = getGraticuleLayer(false);
        return graticuleLayer != null && graticuleLayer.isVisible();
    }

    public void setGraticuleOverlayEnabled(boolean enabled) {
        if (isGraticuleOverlayEnabled() != enabled) {
            getGraticuleLayer(true).setVisible(enabled);
        }
    }

    public boolean isPinOverlayEnabled() {
        Layer pinLayer = getPinLayer(false);
        return pinLayer != null && pinLayer.isVisible();
    }

    public void setPinOverlayEnabled(boolean enabled) {
        if (isPinOverlayEnabled() != enabled) {
            Layer layer = getPinLayer(true);
            layer.setVisible(enabled);
            setSelectedLayer(layer);
        }
    }

    public boolean isGcpOverlayEnabled() {
        Layer gcpLayer = getGcpLayer(false);
        return gcpLayer != null && gcpLayer.isVisible();
    }

    public void setGcpOverlayEnabled(boolean enabled) {
        if (isGcpOverlayEnabled() != enabled) {
            Layer layer = getGcpLayer(true);
            layer.setVisible(enabled);
            setSelectedLayer(layer);
        }
    }

    public boolean isMaskOverlayEnabled() {
        final Layer layer = getMaskCollectionLayer(false);
        return layer != null && layer.isVisible();
    }

    public void setMaskOverlayEnabled(boolean enabled) {
        if (isMaskOverlayEnabled() != enabled) {
            getMaskCollectionLayer(true).setVisible(enabled);
        }
    }

    /**
     * @param vectorDataNodes The vector data nodes whose layer shall be made visible.
     * @since BEAM 4.10
     */
    public void setLayersVisible(VectorDataNode... vectorDataNodes) {
        for (VectorDataNode vectorDataNode : vectorDataNodes) {
            final LayerFilter nodeFilter = VectorDataLayerFilterFactory.createNodeFilter(vectorDataNode);
            Layer vectorDataLayer = LayerUtils.getChildLayer(getRootLayer(),
                    LayerUtils.SEARCH_DEEP,
                    nodeFilter);
            if (vectorDataLayer != null) {
                vectorDataLayer.setVisible(true);
            }
        }
    }

    public ShapeFigure getCurrentShapeFigure() {
        FigureSelection figureSelection = getFigureEditor().getFigureSelection();
        if (figureSelection.getFigureCount() > 0) {
            Figure figure = figureSelection.getFigure(0);
            if (figure instanceof ShapeFigure) {
                return (ShapeFigure) figure;
            }
        } else {
            Layer layer = null;
            final Layer selLayer = getSelectedLayer();
            if (selLayer instanceof VectorDataLayer) {
                final VectorDataLayer vectorLayer = (VectorDataLayer) selLayer;
                if (vectorLayer.getVectorDataNode() != null) {
                    final String typeName = vectorLayer.getVectorDataNode().getFeatureType().getTypeName();
                    if (Product.GEOMETRY_FEATURE_TYPE_NAME.equals(typeName)) {
                        layer = vectorLayer;
                    }
                }
            }

            if (layer == null) {
                layer = LayerUtils.getChildLayer(getRootLayer(), LayerUtils.SearchMode.DEEP,
                        VectorDataLayerFilterFactory.createGeometryFilter());
            }
            if (layer != null) {
                final VectorDataLayer vectorDataLayer = (VectorDataLayer) layer;
                if (vectorDataLayer.getFigureCollection().getFigureCount() > 0) {
                    Figure figure = vectorDataLayer.getFigureCollection().getFigure(0);
                    if (figure instanceof ShapeFigure) {
                        return (ShapeFigure) figure;
                    }
                }
            }
        }
        return null;
    }

    public void setScrollBarsShown(boolean scrollBarsShown) {
        if (scrollBarsShown != this.scrollBarsShown) {
            this.scrollBarsShown = scrollBarsShown;
            if (scrollBarsShown) {
                remove(layerCanvas);
                scrollPane = createScrollPane();
                add(scrollPane, BorderLayout.CENTER);
            } else {
                remove(scrollPane);
                scrollPane = null;
                add(layerCanvas, BorderLayout.CENTER);
            }
            invalidate();
            validate();
            repaint();
        }
    }

    /**
     * Called after SNAP preferences have changed.
     * This behaviour is deprecated since we want to uswe separate style editors for each layers.
     *
     * @param configuration the configuration.
     */
    public void appyLayerProperties(PropertyMap configuration) {
        setScrollBarsShown(configuration.getPropertyBool(PREFERENCE_KEY_IMAGE_SCROLL_BARS_SHOWN, false));
        layerCanvas.setAntialiasing(true);
        layerCanvas.setNavControlShown(configuration.getPropertyBool(PREFERENCE_KEY_IMAGE_NAV_CONTROL_SHOWN, true));
        layerCanvas.setBackground(
                configuration.getPropertyColor("image.background.color", DEFAULT_IMAGE_BACKGROUND_COLOR));

        layerCanvasMouseHandler.setInvertZooming(configuration.getPropertyBool(PREFERENCE_KEY_INVERT_ZOOMING, false));

        ImageLayer imageLayer = getBaseImageLayer();
        if (imageLayer != null) {
            ProductSceneImage.applyBaseImageLayerStyle(configuration, imageLayer);
        }
        Layer noDataLayer = getNoDataLayer(false);
        if (noDataLayer != null) {
            ProductSceneImage.applyNoDataLayerStyle(configuration, noDataLayer);
        }
        Layer collectionLayer = getVectorDataCollectionLayer(false);
        if (collectionLayer != null) {
            ProductSceneImage.applyFigureLayerStyle(configuration, collectionLayer);
        }
        GraticuleLayer graticuleLayer = getGraticuleLayer(false);
        if (graticuleLayer != null) {
            ProductSceneImage.applyGraticuleLayerStyle(configuration, graticuleLayer);
        }
    }

    /**
     * Adds a new pixel position listener to this image display component. If
     * the component already contains the given listener, the method does
     * nothing.
     *
     * @param listener the pixel position listener to be added
     */
    public final void addPixelPositionListener(PixelPositionListener listener) {
        if (listener == null) {
            return;
        }
        if (pixelPositionListeners.contains(listener)) {
            return;
        }
        pixelPositionListeners.add(listener);
    }

    /**
     * Removes a pixel position listener from this image display component.
     *
     * @param listener the pixel position listener to be removed
     */
    public final void removePixelPositionListener(PixelPositionListener listener) {
        if (listener == null || pixelPositionListeners.isEmpty()) {
            return;
        }
        pixelPositionListeners.remove(listener);
    }

    public Layer getSelectedLayer() {
        return selectedLayer;
    }

    public void setSelectedLayer(Layer layer) {
        Layer oldLayer = selectedLayer;
        if (oldLayer != layer) {
            selectedLayer = layer;
            firePropertyChange(PROPERTY_NAME_SELECTED_LAYER, oldLayer, selectedLayer);
            maybeUpdateFigureEditor();
        }
    }

    /**
     * @param vectorDataNode The vector data node, whose layer shall be selected.
     * @return The layer, or {@code null}.
     * @since BEAM 4.7
     */
    public VectorDataLayer selectVectorDataLayer(VectorDataNode vectorDataNode) {
        LayerFilter layerFilter = new VectorDataLayerFilter(vectorDataNode);
        VectorDataLayer layer = (VectorDataLayer) LayerUtils.getChildLayer(getRootLayer(),
                LayerUtils.SEARCH_DEEP,
                layerFilter);
        if (layer != null) {
            setSelectedLayer(layer);
        }
        return layer;
    }

    /**
     * @param pin The pins to test.
     * @return {@code true}, if the pin is selected.
     * @since BEAM 4.7
     */
    public boolean isPinSelected(Placemark pin) {
        return isPlacemarkSelected(getProduct().getPinGroup(), pin);
    }

    /**
     * @param gcp The ground control point to test.
     * @return {@code true}, if the ground control point is selected.
     * @since BEAM 4.7
     */
    public boolean isGcpSelected(Placemark gcp) {
        return isPlacemarkSelected(getProduct().getGcpGroup(), gcp);
    }

    /**
     * @return The (first) selected pin.
     * @since BEAM 4.7
     */
    public Placemark getSelectedPin() {
        return getSelectedPlacemark(getProduct().getPinGroup());
    }

    /**
     * @return The selected pins.
     * @since BEAM 4.7
     */
    public Placemark[] getSelectedPins() {
        return getSelectedPlacemarks(getProduct().getPinGroup());
    }

    /**
     * @return The selected ground control points.
     * @since BEAM 4.7
     */
    public Placemark[] getSelectedGcps() {
        return getSelectedPlacemarks(getProduct().getGcpGroup());
    }

    /**
     * @param pins The selected pins.
     * @since BEAM 4.7
     */
    public void selectPins(Placemark[] pins) {
        selectPlacemarks(getProduct().getPinGroup(), pins);
    }

    /**
     * @param gpcs The selected ground control points.
     * @since BEAM 4.7
     */
    public void selectGcps(Placemark[] gpcs) {
        selectPlacemarks(getProduct().getGcpGroup(), gpcs);
    }

    /**
     * @return The (first) selected feature figure.
     * @since BEAM 4.7
     */
    public SimpleFeatureFigure getSelectedFeatureFigure() {
        Figure[] figures = figureEditor.getFigureSelection().getFigures();
        for (Figure figure : figures) {
            if (figure instanceof SimpleFeatureFigure) {
                return (SimpleFeatureFigure) figure;
            }
        }
        return null;
    }

    /**
     * @return The selected feature figures.
     * @since BEAM 4.7
     * @deprecated since BEAM 4.10, use {@link #getFeatureFigures(boolean)} instead
     */
    public SimpleFeatureFigure[] getSelectedFeatureFigures() {
        ArrayList<SimpleFeatureFigure> selectedFigures = new ArrayList<>();
        collectFeatureFigures(figureEditor.getFigureSelection(), selectedFigures);
        return selectedFigures.toArray(new SimpleFeatureFigure[selectedFigures.size()]);
    }

    /**
     * Gets either the selected figures, or all the figures of the currently selected layer.
     *
     * @param selectedOnly If {@code true}, only selected figures are returned.
     * @return The feature figures or an empty array.
     * @since BEAM 4.10
     */
    public SimpleFeatureFigure[] getFeatureFigures(boolean selectedOnly) {
        ArrayList<SimpleFeatureFigure> selectedFigures = new ArrayList<>();
        collectFeatureFigures(figureEditor.getFigureSelection(), selectedFigures);
        if (selectedFigures.isEmpty()
                && !selectedOnly
                && getSelectedLayer() instanceof VectorDataLayer) {
            VectorDataLayer vectorDataLayer = (VectorDataLayer) getSelectedLayer();
            collectFeatureFigures(vectorDataLayer.getFigureCollection(), selectedFigures);
        }
        return selectedFigures.toArray(new SimpleFeatureFigure[selectedFigures.size()]);
    }

    private void collectFeatureFigures(FigureCollection figureCollection, List<SimpleFeatureFigure> selectedFigures) {
        Figure[] figures = figureCollection.getFigures();
        for (Figure figure : figures) {
            if (figure instanceof SimpleFeatureFigure) {
                selectedFigures.add((SimpleFeatureFigure) figure);
            }
        }
    }

    public boolean selectPlacemarks(PlacemarkGroup placemarkGroup, Placemark[] placemarks) {
        VectorDataLayer layer = selectVectorDataLayer(placemarkGroup.getVectorDataNode());
        if (layer != null) {
            FigureCollection figureCollection = layer.getFigureCollection();
            Figure[] figures = figureCollection.getFigures();
            ArrayList<SimpleFeatureFigure> selectedFigures = new ArrayList<>(figures.length);
            HashSet<Placemark> placemarkSet = new HashSet<>(Arrays.asList(placemarks));
            for (Figure figure : figures) {
                if (figure instanceof SimpleFeatureFigure) {
                    SimpleFeatureFigure featureFigure = (SimpleFeatureFigure) figure;
                    Placemark placemark = placemarkGroup.getPlacemark(featureFigure.getSimpleFeature());
                    if (placemarkSet.contains(placemark)) {
                        selectedFigures.add(featureFigure);
                    }
                }
            }
            figureEditor.getFigureSelection().removeAllFigures();
            figureEditor.getFigureSelection().addFigures(selectedFigures.toArray(new Figure[selectedFigures.size()]));
            final int selectionStage = Math.min(selectedFigures.size(), 2);
            figureEditor.getFigureSelection().setSelectionStage(selectionStage);
            return true;
        }
        return false;
    }

    private boolean isPlacemarkSelected(PlacemarkGroup placemarkGroup, Placemark placemark) {
        Figure[] figures = figureEditor.getFigureSelection().getFigures();
        for (Figure figure : figures) {
            if (figure instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure featureFigure = (SimpleFeatureFigure) figure;
                Placemark pin = placemarkGroup.getPlacemark(featureFigure.getSimpleFeature());
                if (pin == placemark) {
                    return true;
                }
            }
        }
        return false;
    }

    private Placemark getSelectedPlacemark(PlacemarkGroup placemarkGroup) {

        Figure[] figures = figureEditor.getFigureSelection().getFigures();
        for (Figure figure : figures) {
            if (figure instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure featureFigure = (SimpleFeatureFigure) figure;
                Placemark placemark = placemarkGroup.getPlacemark(featureFigure.getSimpleFeature());
                if (placemark != null) {
                    return placemark;
                }
            }
        }
        return null;
    }

    private Placemark[] getSelectedPlacemarks(PlacemarkGroup placemarkGroup) {
        Figure[] figures = figureEditor.getFigureSelection().getFigures();
        ArrayList<Placemark> selectedPlacemarks = new ArrayList<>(figures.length);
        for (Figure figure : figures) {
            if (figure instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure featureFigure = (SimpleFeatureFigure) figure;
                Placemark placemark = placemarkGroup.getPlacemark(featureFigure.getSimpleFeature());
                if (placemark != null) {
                    selectedPlacemarks.add(placemark);
                }
            }
        }
        return selectedPlacemarks.toArray(new Placemark[selectedPlacemarks.size()]);
    }

    private void maybeUpdateFigureEditor() {
        if (selectedLayer instanceof VectorDataLayer) {
            VectorDataLayer vectorDataLayer = (VectorDataLayer) selectedLayer;
            figureEditor.vectorDataLayerSelected(vectorDataLayer);
        }
    }

    public void disposeLayers() {
        getSceneImage().getRootLayer().dispose();
    }

    public AffineTransform getBaseImageToViewTransform() {
        AffineTransform viewToModelTransform = layerCanvas.getViewport().getViewToModelTransform();
        AffineTransform modelToImageTransform = getBaseImageLayer().getModelToImageTransform();
        viewToModelTransform.concatenate(modelToImageTransform);
        try {
            return viewToModelTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the visible image area in pixel coordinates
     */
    public Rectangle getVisibleImageBounds() {
        final ImageLayer imageLayer = getBaseImageLayer();

        if (imageLayer != null) {
            final RenderedImage image = imageLayer.getImage();
            final Area imageArea = new Area(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
            final Area visibleImageArea = new Area(
                    imageLayer.getModelToImageTransform().createTransformedShape(getVisibleModelBounds()));
            imageArea.intersect(visibleImageArea);
            return imageArea.getBounds();
        }

        return null;
    }

    /**
     * @return the visible area in model coordinates
     */
    public Rectangle2D getVisibleModelBounds() {
        final Viewport viewport = layerCanvas.getViewport();
        return viewport.getViewToModelTransform().createTransformedShape(viewport.getViewBounds()).getBounds2D();
    }

    /**
     * @return the model bounds in model coordinates
     */
    public Rectangle2D getModelBounds() {
        return layerCanvas.getLayer().getModelBounds();
    }

    public double getOrientation() {
        return layerCanvas.getViewport().getOrientation();
    }

    public double getZoomFactor() {
        return layerCanvas.getViewport().getZoomFactor();
    }

    public void zoom(Rectangle2D modelRect) {
        layerCanvas.getViewport().zoom(modelRect);
    }

    public void zoom(double x, double y, double viewScale) {
        if (viewScale > 0) {
            layerCanvas.getViewport().setZoomFactor(viewScale, x, y);
        }
    }

    public boolean synchronizeViewportIfPossible(ProductSceneView thatView) {
        final RasterDataNode thisRaster = getRaster();
        final RasterDataNode thatRaster = thatView.getRaster();
        final Product thisProduct = thisRaster.getProduct();
        if (thisProduct.isSceneCrsEqualToModelCrsOf(thatRaster)) {
            final Viewport thisViewport = layerCanvas.getViewport();
            final Viewport thatViewport = thatView.layerCanvas.getViewport();
            thatViewport.setTransform(thisViewport);
            return true;
        } else if (thisProduct == thatRaster.getProduct()) {
            final Viewport thisViewport = layerCanvas.getViewport();
            final Viewport thatViewport = thatView.layerCanvas.getViewport();
            final Rectangle thisViewBounds = thisViewport.getViewBounds();
            final Rectangle thisModelBounds = thisViewport.getViewToModelTransform().createTransformedShape(thisViewBounds).getBounds();
            try {
                final Rectangle sceneBounds = thisRaster.getModelToSceneTransform().createTransformedShape(thisModelBounds).getBounds();
                final Rectangle thatModelBounds = thatRaster.getSceneToModelTransform().createTransformedShape(sceneBounds).getBounds();
                thatViewport.zoom(thatModelBounds);
                return true;
            } catch (TransformException e) {
                //try code below
            }
        }
        final GeoCoding thisGeoCoding = thisRaster.getGeoCoding();
        final GeoCoding thatGeoCoding = thatRaster.getGeoCoding();
        if (thisGeoCoding != null && thatGeoCoding != null && thisGeoCoding.canGetGeoPos() && thatGeoCoding.canGetPixelPos()) {
            final Viewport thisViewport = layerCanvas.getViewport();
            final Viewport thatViewport = thatView.layerCanvas.getViewport();
            final double viewCenterX = thisViewport.getViewBounds().getCenterX();
            final double viewCenterY = thisViewport.getViewBounds().getCenterY();
            final Point2D viewCenter = new Point2D.Double(viewCenterX, viewCenterY);
            final Point2D modelCenter = thisViewport.getViewToModelTransform().transform(viewCenter, null);
            final PixelPos imageCenter = new PixelPos();
            getBaseImageLayer().getModelToImageTransform().transform(modelCenter, imageCenter);
            final GeoPos geoCenter = new GeoPos();
            thisGeoCoding.getGeoPos(imageCenter, geoCenter);
            thatGeoCoding.getPixelPos(geoCenter, imageCenter);
            if (imageCenter.isValid()) {
                thatView.getBaseImageLayer().getImageToModelTransform().transform(imageCenter, modelCenter);
                thatViewport.setZoomFactor(thisViewport.getZoomFactor(), modelCenter.getX(), modelCenter.getY());
                return true;
            }
        }
        return false;
    }

    protected void disposeImageDisplayComponent() {
        layerCanvas.dispose();
    }

    // only called from VISAT

    public void updateImage() {
        getBaseImageLayer().regenerate();
    }

    // used by PropertyEditor

    public void updateNoDataImage() {
        // change configuration of layer ; not setting MultiLevelSource
        final String expression = getRaster().getValidMaskExpression();
        final ImageLayer noDataLayer = (ImageLayer) getNoDataLayer(false);
        if (noDataLayer != null) {
            if (expression != null) {
                final Color color = noDataLayer.getConfiguration().getValue(
                        NoDataLayerType.PROPERTY_NAME_COLOR);
                final MultiLevelSource multiLevelSource = ColoredMaskImageMultiLevelSource.create(getRaster().getProduct(),
                        color, expression, true,
                        getBaseImageLayer().getImageToModelTransform());
                noDataLayer.setMultiLevelSource(multiLevelSource);
            } else {
                noDataLayer.setMultiLevelSource(MultiLevelSource.NULL);
            }
        }
    }

    public int getFirstImageLayerIndex() {
        return sceneImage.getFirstImageLayerIndex();
    }


    /**
     * A band that is used as an RGB channel for RGB image views.
     * These bands shall not be added to {@link Product}s but they are always owned by the {@link Product}
     * passed into the constructor.
     */
    public static class RGBChannel extends VirtualBand {

        /**
         * Constructs a new RGB image view band.
         *
         * @param product    the product which takes the ownership
         * @param width      the width of the image
         * @param height     the height of the image
         * @param name       the band's name
         * @param expression the expression
         * @param products   the products used to evaluate the expression
         */
        public RGBChannel(final Product product, final int width, final int height, final String name, final String expression, Product[] products) {
            super(name,
                    ProductData.TYPE_FLOAT32,
                    width,
                    height,
                    expression);
            if(products == null || products.length == 0) {
                deriveRasterPropertiesFromExpression(expression, product);
            } else {
                deriveRasterPropertiesFromExpression(expression, products);
            }
            setOwner(product);
            setModified(false);
        }

        /**
         * Constructs a new RGB image view band.
         *
         * @param product    the product which takes the ownership
         * @param width      the width of the image
         * @param height     the height of the image
         * @param name       the band's name
         * @param expression the expression
         */
        public RGBChannel(final Product product, final int width, final int height, final String name, final String expression) {
            this(product, product.getSceneRasterWidth(), product.getSceneRasterHeight(), name, expression, null);
        }

        /**
         * Constructs a new RGB image view band.
         *
         * @param product    the product which takes the ownership
         * @param name       the band's name
         * @param expression the expression
         * @param products   the products used to evaluate the expression
         */
        public RGBChannel(final Product product, final String name, final String expression, Product[] products) {
            this(product, product.getSceneRasterWidth(), product.getSceneRasterHeight(), name, expression, products);
        }

        /**
         * Constructs a new RGB image view band.
         *
         * @param product    the product which takes the ownership
         * @param name       the band's name
         * @param expression the expression
         */
        public RGBChannel(final Product product, final String name, final String expression) {
            this(product, product.getSceneRasterWidth(), product.getSceneRasterHeight(), name, expression, null);
        }

        private void deriveRasterPropertiesFromExpression(String expression, Product... products) {
            if (products != null) {
                try {
                    String validMaskExpression = BandArithmetic.getValidMaskExpression(getExpression(), products, 0, null);
                    setValidPixelExpression(validMaskExpression);
                    final RasterDataNode[] refRasters = BandArithmetic.getRefRasters(expression, products);
                    if (refRasters.length > 0) {
                        setGeoCoding(refRasters[0].getGeoCoding());
                        setImageToModelTransform(refRasters[0].getImageToModelTransform());
                        setSceneToModelTransform(refRasters[0].getSceneToModelTransform());
                        setModelToSceneTransform(refRasters[0].getModelToSceneTransform());
                    }
                } catch (ParseException e) {
                    // do not set geocoding then

                }
            }
        }

    }

    private final class RasterChangeHandler implements ProductNodeListener {

        @Override
        public void nodeChanged(final ProductNodeEvent event) {
            repaintView();
        }

        @Override
        public void nodeDataChanged(final ProductNodeEvent event) {
            repaintView();
        }

        @Override
        public void nodeAdded(final ProductNodeEvent event) {
            repaintView();
        }

        @Override
        public void nodeRemoved(final ProductNodeEvent event) {
            repaintView();
        }

        private void repaintView() {
            repaint(100);
        }
    }

    private Layer getNoDataLayer(boolean create) {
        return getSceneImage().getNoDataLayer(create);
    }

    public Layer getVectorDataCollectionLayer(boolean create) {
        return getSceneImage().getVectorDataCollectionLayer(create);
    }

    private Layer getMaskCollectionLayer(boolean create) {
        return getSceneImage().getMaskCollectionLayer(create);
    }

    private GraticuleLayer getGraticuleLayer(boolean create) {
        return getSceneImage().getGraticuleLayer(create);
    }

    private Layer getPinLayer(boolean create) {
        return getSceneImage().getPinLayer(create);
    }

    private Layer getGcpLayer(boolean create) {
        return getSceneImage().getGcpLayer(create);
    }

    private static boolean isModelYAxisDown(ImageLayer baseImageLayer) {
        return baseImageLayer.getImageToModelTransform().getDeterminant() > 0.0;
    }

    private void registerLayerCanvasListeners() {
        layerCanvasComponentHandler = new LayerCanvasComponentHandler();
        layerCanvasMouseHandler = new LayerCanvasMouseHandler();

        layerCanvas.addComponentListener(layerCanvasComponentHandler);
        layerCanvas.addMouseListener(layerCanvasMouseHandler);
        layerCanvas.addMouseMotionListener(layerCanvasMouseHandler);
        layerCanvas.addMouseWheelListener(layerCanvasMouseHandler);

        PopupMenuHandler popupMenuHandler = new PopupMenuHandler(this);
        layerCanvas.addMouseListener(popupMenuHandler);
        layerCanvas.addKeyListener(popupMenuHandler);
    }

    private void deregisterLayerCanvasListeners() {
        getRaster().getProduct().removeProductNodeListener(rasterChangeHandler);
        layerCanvas.removeComponentListener(layerCanvasComponentHandler);
        layerCanvas.removeMouseListener(layerCanvasMouseHandler);
        layerCanvas.removeMouseMotionListener(layerCanvasMouseHandler);
    }

    private boolean isPixelPosValid(int currentPixelX, int currentPixelY, int currentLevel) {
        return currentPixelX >= 0 && currentPixelX < baseImageLayer.getImage(
                currentLevel).getWidth() && currentPixelY >= 0
                && currentPixelY < baseImageLayer.getImage(currentLevel).getHeight();
    }

    private void firePixelPosChanged(MouseEvent e, int currentPixelX, int currentPixelY, int currentLevel) {
        boolean pixelPosValid = isPixelPosValid(currentPixelX, currentPixelY, currentLevel);
        for (PixelPositionListener listener : pixelPositionListeners) {
            listener.pixelPosChanged(baseImageLayer, currentPixelX, currentPixelY, currentLevel, pixelPosValid, e);
        }
    }

    private void firePixelPosNotAvailable() {
        for (PixelPositionListener listener : pixelPositionListeners) {
            listener.pixelPosNotAvailable();
        }
    }

    private void setPixelPos(MouseEvent e, boolean showBorder) {
        if (e.getID() == MouseEvent.MOUSE_EXITED) {
            currentLevelPixelX = -1;
            firePixelPosNotAvailable();
        } else {
            Point2D p = new Point2D.Double(e.getX() + 0.5, e.getY() + 0.5);

            Viewport viewport = getLayerCanvas().getViewport();
            AffineTransform v2mTransform = viewport.getViewToModelTransform();
            final Point2D modelP = v2mTransform.transform(p, null);

            AffineTransform m2iTransform = baseImageLayer.getModelToImageTransform();
            Point2D imageP = m2iTransform.transform(modelP, null);
            currentPixelX = (int) Math.floor(imageP.getX());
            currentPixelY = (int) Math.floor(imageP.getY());

            int currentLevel = baseImageLayer.getLevel(viewport);
            AffineTransform m2iLevelTransform = baseImageLayer.getModelToImageTransform(currentLevel);
            Point2D imageLevelP = m2iLevelTransform.transform(modelP, null);
            int currentPixelX = (int) Math.floor(imageLevelP.getX());
            int currentPixelY = (int) Math.floor(imageLevelP.getY());
            if (currentPixelX != currentLevelPixelX || currentPixelY != currentLevelPixelY || currentLevel != this.currentLevel) {
                if (isPixelBorderDisplayEnabled() && (showBorder || pixelBorderDrawn)) {
                    drawPixelBorder(currentPixelX, currentPixelY, currentLevel, showBorder);
                }
                currentLevelPixelX = currentPixelX;
                currentLevelPixelY = currentPixelY;
                this.currentLevel = currentLevel;
                firePixelPosChanged(e, currentLevelPixelX, currentLevelPixelY, this.currentLevel);
            }
        }
    }

    private boolean isPixelBorderDisplayEnabled() {
        return pixelBorderShown &&
                getLayerCanvas().getViewport().getZoomFactor() >= pixelBorderViewScale;
    }

    private void drawPixelBorder(int currentPixelX, int currentPixelY, int currentLevel, boolean showBorder) {
        final Graphics g = getGraphics();
        g.setXORMode(Color.white);
        if (pixelBorderDrawn) {
            drawPixelBorder(g, currentLevelPixelX, currentLevelPixelY, this.currentLevel);
            pixelBorderDrawn = false;
        }
        if (showBorder) {
            drawPixelBorder(g, currentPixelX, currentPixelY, currentLevel);
            pixelBorderDrawn = true;
        }
        g.setPaintMode();
        g.dispose();
    }

    private void drawPixelBorder(final Graphics g, final int x, final int y, final int l) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            AffineTransform i2m = getBaseImageLayer().getImageToModelTransform(l);
            AffineTransform m2v = getLayerCanvas().getViewport().getModelToViewTransform();
            Rectangle imageRect = new Rectangle(x, y, 1, 1);
            Shape modelRect = i2m.createTransformedShape(imageRect);
            Shape transformedShape = m2v.createTransformedShape(modelRect);
            g2d.draw(transformedShape);
        }
    }

    private final class LayerCanvasMouseHandler implements MouseInputListener, MouseWheelListener {

        private boolean invertZooming;

        public LayerCanvasMouseHandler() {
            invertZooming = sceneImage.getConfiguration().getPropertyBool(PREFERENCE_KEY_INVERT_ZOOMING, false);
        }

        public void setInvertZooming(boolean invertZooming) {
            this.invertZooming = invertZooming;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            updatePixelPos(e, false);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            updatePixelPos(e, false);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            updatePixelPos(e, false);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            updatePixelPos(e, false);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            updatePixelPos(e, false);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            updatePixelPos(e, true);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            updatePixelPos(e, true);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isAltDown() || e.isAltGraphDown() || e.isControlDown() || e.isShiftDown()) {
                return;
            }
            Viewport viewport = layerCanvas.getViewport();
            int wheelRotation = e.getWheelRotation();
            if (invertZooming) {
                wheelRotation *= -1;
            }
            double oldZoomFactor = viewport.getZoomFactor();
            double newZoomFactor = oldZoomFactor * Math.pow(1.1, wheelRotation);
            viewport.setZoomFactor(newZoomFactor);
        }

        private void updatePixelPos(MouseEvent e, boolean showBorder) {
            setPixelPos(e, showBorder);
        }
    }

    private class LayerCanvasComponentHandler extends ComponentAdapter {

        /**
         * Invoked when the component has been made invisible.
         */
        @Override
        public void componentHidden(ComponentEvent e) {
            firePixelPosNotAvailable();
        }
    }


    static class NullFigureCollection implements FigureCollection {

        static final FigureCollection INSTANCE = new NullFigureCollection();

        private NullFigureCollection() {
        }

        @Override
        public boolean isCollection() {
            return false;
        }

        @Override
        public boolean contains(Figure figure) {
            return false;
        }

        @Override
        public boolean isCloseTo(Point2D point, AffineTransform m2v) {
            return false;
        }

        @Override
        public Rectangle2D getBounds() {
            return new Rectangle();
        }

        @Override
        public Rank getRank() {
            return Figure.Rank.NOT_SPECIFIED;
        }

        @Override
        public void move(double dx, double dy) {
        }

        @Override
        public void scale(Point2D point, double sx, double sy) {
        }

        @Override
        public void rotate(Point2D point, double theta) {
        }

        @Override
        public double[] getSegment(int index) {
            return null;
        }

        @Override
        public void setSegment(int index, double[] segment) {
        }

        @Override
        public void addSegment(int index, double[] segment) {
        }

        @Override
        public void removeSegment(int index) {
        }

        @Override
        public boolean isSelectable() {
            return false;
        }

        @Override
        public boolean isSelected() {
            return false;
        }

        @Override
        public void setSelected(boolean selected) {
        }

        @Override
        public void draw(Rendering rendering) {
        }

        @Override
        public int getFigureCount() {
            return 0;
        }

        @Override
        public int getFigureIndex(Figure figure) {
            return 0;
        }

        @Override
        public Figure getFigure(int index) {
            return null;
        }

        @Override
        public Figure getFigure(Point2D point, AffineTransform m2v) {
            return null;
        }

        @Override
        public Figure[] getFigures() {
            return new Figure[0];
        }

        @Override
        public Figure[] getFigures(Shape shape) {
            return new Figure[0];
        }

        @Override
        public boolean addFigure(Figure figure) {
            return false;
        }

        @Override
        public boolean addFigure(int index, Figure figure) {
            return false;
        }

        @Override
        public Figure[] addFigures(Figure... figures) {
            return new Figure[0];
        }

        @Override
        public boolean removeFigure(Figure figure) {
            return false;
        }

        @Override
        public Figure[] removeFigures(Figure... figures) {
            return new Figure[0];
        }

        @Override
        public Figure[] removeAllFigures() {
            return new Figure[0];
        }

        @Override
        public int getMaxSelectionStage() {
            return 0;
        }

        @Override
        public Handle[] createHandles(int selectionStage) {
            return new Handle[0];
        }

        @Override
        public void addChangeListener(FigureChangeListener listener) {
        }

        @Override
        public void removeChangeListener(FigureChangeListener listener) {
        }

        @Override
        public FigureChangeListener[] getChangeListeners() {
            return new FigureChangeListener[0];
        }

        @Override
        public void dispose() {
        }

        @Override
        public Object createMemento() {
            return null;
        }

        @Override
        public void setMemento(Object memento) {
        }

        @Override
        public FigureStyle getNormalStyle() {
            return null;
        }

        @Override
        public void setNormalStyle(FigureStyle normalStyle) {
        }

        @Override
        public FigureStyle getSelectedStyle() {
            return null;
        }

        @Override
        public void setSelectedStyle(FigureStyle selectedStyle) {
        }

        @Override
        public FigureStyle getEffectiveStyle() {
            return null;
        }

        @Override
        public Object clone() {
            return INSTANCE;
        }
    }

    private static class VectorDataLayerFilter implements LayerFilter {

        private final VectorDataNode vectorDataNode;

        public VectorDataLayerFilter(VectorDataNode vectorDataNode) {
            this.vectorDataNode = vectorDataNode;
        }

        @Override
        public boolean accept(Layer layer) {
            return layer instanceof VectorDataLayer && ((VectorDataLayer) layer).getVectorDataNode() == vectorDataNode;
        }
    }

    private class PinSelectionChangeListener extends AbstractSelectionChangeListener {

        private boolean firedNoPinSelected = false;

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            Selection selection = event.getSelection();
            if (selection.isEmpty()) {
                if (!firedNoPinSelected) {
                    firePropertyChange(PROPERTY_NAME_SELECTED_PIN, null, null);
                    firedNoPinSelected = true;
                }
            } else {
                Object selectedValue = selection.getSelectedValue();
                if (selectedValue instanceof SimpleFeatureFigure) {
                    SimpleFeatureFigure featureFigure = (SimpleFeatureFigure) selectedValue;
                    PlacemarkGroup pinGroup = getProduct().getPinGroup();
                    Placemark pin = pinGroup.getPlacemark(featureFigure.getSimpleFeature());
                    if (pin != null) {
                        firePropertyChange(PROPERTY_NAME_SELECTED_PIN, null, pin);
                        firedNoPinSelected = false;
                    }
                }
            }
        }
    }

}
