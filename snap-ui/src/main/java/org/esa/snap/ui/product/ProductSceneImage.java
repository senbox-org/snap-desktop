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

package org.esa.snap.ui.product;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.image.ColoredBandImageMultiLevelSource;
import org.esa.snap.core.layer.GraticuleLayer;
import org.esa.snap.core.layer.GraticuleLayerType;
import org.esa.snap.core.layer.MaskCollectionLayerType;
import org.esa.snap.core.layer.MaskLayerType;
import org.esa.snap.core.layer.NoDataLayerType;
import org.esa.snap.core.layer.ProductLayerContext;
import org.esa.snap.core.layer.RasterImageLayerType;
import org.esa.snap.core.layer.RgbImageLayerType;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.ui.PackageDefaults;

import java.awt.Color;
import java.awt.geom.AffineTransform;


// SEP2018 - Daniel Knowles - added multiple new properties to the Graticule layer configuration

public class ProductSceneImage implements ProductLayerContext {

    private static final ImageLayerFilter IMAGE_LAYER_FILTER = new ImageLayerFilter();
    private final String name;
    private final PropertyMap configuration;
    private RasterDataNode[] rasters;
    private Layer rootLayer;
    private ColoredBandImageMultiLevelSource coloredBandImageMultiLevelSource;

    /**
     * Creates a color indexed product scene for the given product raster.
     *
     * @param raster        the product raster, must not be null
     * @param configuration a configuration
     * @param pm            a monitor to inform the user about progress @return a color indexed product scene image
     */
    public ProductSceneImage(RasterDataNode raster, PropertyMap configuration, ProgressMonitor pm) {
        this(raster.getDisplayName(),
                new RasterDataNode[]{raster},
                configuration);
        coloredBandImageMultiLevelSource = ColoredBandImageMultiLevelSource.create(raster, pm);
        initRootLayer();
    }

    /**
     * Creates a new scene image for an existing view.
     *
     * @param raster The product raster.
     * @param view   An existing view.
     */
    public ProductSceneImage(RasterDataNode raster, ProductSceneView view) {
        this(raster.getDisplayName(),
                new RasterDataNode[]{raster},
                view.getSceneImage().getConfiguration());
        coloredBandImageMultiLevelSource = view.getSceneImage().getColoredBandImageMultiLevelSource();
        initRootLayer();
    }

    /**
     * Creates an RGB product scene for the given raster datasets.
     *
     * @param name          the name of the scene view
     * @param redRaster     the product raster used for the red color component, must not be null
     * @param greenRaster   the product raster used for the green color component, must not be null
     * @param blueRaster    the product raster used for the blue color component, must not be null
     * @param configuration a configuration
     * @param pm            a monitor to inform the user about progress @return an RGB product scene image @throws java.io.IOException if the image creation failed due to an I/O problem
     */
    public ProductSceneImage(String name, RasterDataNode redRaster,
                             RasterDataNode greenRaster,
                             RasterDataNode blueRaster,
                             PropertyMap configuration,
                             ProgressMonitor pm) {
        this(name, new RasterDataNode[]{redRaster, greenRaster, blueRaster}, configuration);
        coloredBandImageMultiLevelSource = ColoredBandImageMultiLevelSource.create(rasters, pm);
        initRootLayer();
    }

    private ProductSceneImage(String name, RasterDataNode[] rasters, PropertyMap configuration) {
        this.name = name;
        this.rasters = rasters;
        this.configuration = configuration;
    }

    public PropertyMap getConfiguration() {
        return configuration;
    }

    public String getName() {
        return name;
    }

    public ImageInfo getImageInfo() {
        return coloredBandImageMultiLevelSource.getImageInfo();
    }

    public void setImageInfo(ImageInfo imageInfo) {
        coloredBandImageMultiLevelSource.setImageInfo(imageInfo);
    }

    public RasterDataNode[] getRasters() {
        return rasters;
    }

    public void setRasters(RasterDataNode[] rasters) {
        this.rasters = rasters;
    }

    @Override
    public Object getCoordinateReferenceSystem() {
        return getProduct().getSceneCRS();
    }

    @Override
    public Layer getRootLayer() {
        return rootLayer;
    }

    Layer getLayer(String id) {
        return LayerUtils.getChildLayerById(getRootLayer(), id);
    }

    void addLayer(int index, Layer layer) {
        rootLayer.getChildren().add(index, layer);
    }

    int getFirstImageLayerIndex() {
        return LayerUtils.getChildLayerIndex(getRootLayer(), LayerUtils.SEARCH_DEEP, 0, IMAGE_LAYER_FILTER);
    }

    ImageLayer getBaseImageLayer() {
        return (ImageLayer) getLayer(ProductSceneView.BASE_IMAGE_LAYER_ID);
    }

    Layer getNoDataLayer(boolean create) {
        Layer layer = getLayer(ProductSceneView.NO_DATA_LAYER_ID);
        if (layer == null && create) {
            layer = createNoDataLayer();
            addLayer(getFirstImageLayerIndex(), layer);
        }
        return layer;
    }

    Layer getVectorDataCollectionLayer(boolean create) {
        Layer layer = getLayer(ProductSceneView.VECTOR_DATA_LAYER_ID);
        if (layer == null && create) {
            layer = createVectorDataCollectionLayer();
            addLayer(getFirstImageLayerIndex(), layer);
        }
        return layer;
    }

    Layer getMaskCollectionLayer(boolean create) {
        Layer layer = getLayer(ProductSceneView.MASKS_LAYER_ID);
        if (layer == null && create) {
            layer = createMaskCollectionLayer();
            addLayer(getFirstImageLayerIndex(), layer);
        }
        return layer;
    }

    GraticuleLayer getGraticuleLayer(boolean create) {
        GraticuleLayer layer = (GraticuleLayer) getLayer(ProductSceneView.GRATICULE_LAYER_ID);
        if (layer == null && create) {
            layer = createGraticuleLayer(getImageToModelTransform());
            addLayer(0, layer);
        }
        return layer;
    }

    Layer getGcpLayer(boolean create) {
        final Product product = getProduct();
        if (product != null) {
            final VectorDataNode vectorDataNode = product.getGcpGroup().getVectorDataNode();
            final Layer vectorDataCollectionLayer = getVectorDataCollectionLayer(create);
            if (vectorDataCollectionLayer != null) {
                return LayerUtils.getChildLayer(getRootLayer(),
                        LayerUtils.SEARCH_DEEP,
                        VectorDataLayerFilterFactory.createNodeFilter(vectorDataNode));
            }
        }
        return null;
    }

    Layer getPinLayer(boolean create) {
        final Product product = getProduct();
        if (product != null) {
            final VectorDataNode vectorDataNode = product.getPinGroup().getVectorDataNode();
            final Layer vectorDataCollectionLayer = getVectorDataCollectionLayer(create);
            if (vectorDataCollectionLayer != null) {
                return LayerUtils.getChildLayer(getRootLayer(),
                        LayerUtils.SEARCH_DEEP,
                        VectorDataLayerFilterFactory.createNodeFilter(vectorDataNode));
            }
        }
        return null;
    }

    private RasterDataNode getRaster() {
        return rasters[0];
    }

    private void initRootLayer() {
        rootLayer = new CollectionLayer();
        addLayer(0, createBaseImageLayer());
    }

    public void initVectorDataCollectionLayer() {
        if (mustEnableVectorDataCollectionLayer()) {
            getVectorDataCollectionLayer(true);
        }
    }

    public void initMaskCollectionLayer() {
        if (mustEnableMaskCollectionLayer()) {
            getMaskCollectionLayer(true);
        }
    }

    private boolean mustEnableVectorDataCollectionLayer() {
        return getRaster().getProduct().getVectorDataGroup().getNodeCount() > 0;
    }

    private boolean mustEnableMaskCollectionLayer() {
        return getRaster().getOverlayMaskGroup().getNodeCount() > 0;
    }

    private AffineTransform getImageToModelTransform() {
        return coloredBandImageMultiLevelSource.getModel().getImageToModelTransform(0);
    }

    private Layer createBaseImageLayer() {
        final Layer layer;
        if (getRasters().length == 1) {
            final RasterImageLayerType type = LayerTypeRegistry.getLayerType(RasterImageLayerType.class);
            layer = type.createLayer(getRaster(), coloredBandImageMultiLevelSource);
        } else {
            final RgbImageLayerType type = LayerTypeRegistry.getLayerType(RgbImageLayerType.class);
            layer = type.createLayer(getRasters(), coloredBandImageMultiLevelSource);
        }

        layer.setName(getName());
        layer.setVisible(true);
        layer.setId(ProductSceneView.BASE_IMAGE_LAYER_ID);
        applyBaseImageLayerStyle(configuration, layer);
        return layer;
    }

    static void applyBaseImageLayerStyle(PropertyMap configuration, Layer layer) {
        final boolean borderShown = configuration.getPropertyBool("image.border.shown",
                ImageLayer.DEFAULT_BORDER_SHOWN);
        final double borderWidth = configuration.getPropertyDouble("image.border.size",
                ImageLayer.DEFAULT_BORDER_WIDTH);
        final Color borderColor = configuration.getPropertyColor("image.border.color",
                ImageLayer.DEFAULT_BORDER_COLOR);
        final boolean pixelBorderShown = configuration.getPropertyBool("pixel.border.shown",
                ImageLayer.DEFAULT_PIXEL_BORDER_SHOWN);
        final double pixelBorderWidth = configuration.getPropertyDouble("pixel.border.size",
                ImageLayer.DEFAULT_PIXEL_BORDER_WIDTH);
        final Color pixelBorderColor = configuration.getPropertyColor("pixel.border.color",
                ImageLayer.DEFAULT_PIXEL_BORDER_COLOR);

        final PropertySet layerConfiguration = layer.getConfiguration();
        layerConfiguration.setValue(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, borderShown);
        layerConfiguration.setValue(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, borderWidth);
        layerConfiguration.setValue(ImageLayer.PROPERTY_NAME_BORDER_COLOR, borderColor);
        layerConfiguration.setValue(ImageLayer.PROPERTY_NAME_PIXEL_BORDER_SHOWN, pixelBorderShown);
        layerConfiguration.setValue(ImageLayer.PROPERTY_NAME_PIXEL_BORDER_WIDTH, pixelBorderWidth);
        layerConfiguration.setValue(ImageLayer.PROPERTY_NAME_PIXEL_BORDER_COLOR, pixelBorderColor);
    }

    private Layer createNoDataLayer() {
        final LayerType noDataType = LayerTypeRegistry.getLayerType(NoDataLayerType.class);
        final PropertySet configTemplate = noDataType.createLayerConfig(null);

        final Color color = configuration.getPropertyColor("noDataOverlay.color", PackageDefaults.NO_DATA_LAYER_COLOR);
        configTemplate.setValue(NoDataLayerType.PROPERTY_NAME_COLOR, color);
        configTemplate.setValue(NoDataLayerType.PROPERTY_NAME_RASTER, getRaster());
        final Layer layer = noDataType.createLayer(this, configTemplate);
        final double transparency = configuration.getPropertyDouble("noDataOverlay.transparency", 0.3);
        layer.setTransparency(transparency);
        return layer;
    }

    private synchronized Layer createVectorDataCollectionLayer() {
        final LayerType collectionLayerType = LayerTypeRegistry.getLayerType(VectorDataCollectionLayerType.class);
        final Layer collectionLayer = collectionLayerType.createLayer(this, collectionLayerType.createLayerConfig(this));
        final ProductNodeGroup<VectorDataNode> vectorDataGroup = getRaster().getProduct().getVectorDataGroup();

        final VectorDataNode[] vectorDataNodes = vectorDataGroup.toArray(new VectorDataNode[vectorDataGroup.getNodeCount()]);
        for (final VectorDataNode vectorDataNode : vectorDataNodes) {
            final Layer layer = VectorDataLayerType.createLayer(this, vectorDataNode);
            collectionLayer.getChildren().add(layer);
        }

        return collectionLayer;
    }

    private synchronized Layer createMaskCollectionLayer() {
        final LayerType maskCollectionType = LayerTypeRegistry.getLayerType(MaskCollectionLayerType.class);
        final PropertySet layerConfig = maskCollectionType.createLayerConfig(null);
        layerConfig.setValue(MaskCollectionLayerType.PROPERTY_NAME_RASTER, getRaster());
        final Layer maskCollectionLayer = maskCollectionType.createLayer(this, layerConfig);
        ProductNodeGroup<Mask> productNodeGroup = getRaster().getProduct().getMaskGroup();
        final RasterDataNode raster = getRaster();
        for (int i = 0; i < productNodeGroup.getNodeCount(); i++) {
            final Mask mask = productNodeGroup.get(i);
            //todo add all mask layers as soon as the masks have been scaled to fit the raster
            if (raster.getRasterSize().equals(mask.getRasterSize())) {
                Layer layer = MaskLayerType.createLayer(raster, mask);
                maskCollectionLayer.getChildren().add(layer);
            }
        }
        return maskCollectionLayer;
    }

    static void applyNoDataLayerStyle(PropertyMap configuration, Layer layer) {
        final PropertySet layerConfiguration = layer.getConfiguration();
        final Color color = configuration.getPropertyColor("noDataOverlay.color", NoDataLayerType.DEFAULT_COLOR);
        layerConfiguration.setValue(NoDataLayerType.PROPERTY_NAME_COLOR, color);

        final double transparency = configuration.getPropertyDouble("noDataOverlay.transparency", 0.3);
        layer.setTransparency(transparency);
    }

    static void applyFigureLayerStyle(PropertyMap configuration, Layer layer) {
        final PropertySet layerConfiguration = layer.getConfiguration();
/*
        layerConfiguration.setValue(VectorDataLayer.PROPERTY_NAME_SHAPE_OUTLINED,
                                    configuration.getPropertyBool(VectorDataLayer.PROPERTY_NAME_SHAPE_OUTLINED,
                                                                  VectorDataLayer.DEFAULT_SHAPE_OUTLINED));
        layerConfiguration.setValue(VectorDataLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR,
                                    configuration.getPropertyColor(VectorDataLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR,
                                                                   VectorDataLayer.DEFAULT_SHAPE_OUTL_COLOR));
        layerConfiguration.setValue(VectorDataLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY,
                                    configuration.getPropertyDouble(
                                            VectorDataLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY,
                                            VectorDataLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY));
        layerConfiguration.setValue(VectorDataLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH,
                                    configuration.getPropertyDouble(VectorDataLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH,
                                                                    VectorDataLayer.DEFAULT_SHAPE_OUTL_WIDTH));
        layerConfiguration.setValue(VectorDataLayer.PROPERTY_NAME_SHAPE_FILLED,
                                    configuration.getPropertyBool(VectorDataLayer.PROPERTY_NAME_SHAPE_FILLED,
                                                                  VectorDataLayer.DEFAULT_SHAPE_FILLED));
        layerConfiguration.setValue(VectorDataLayer.PROPERTY_NAME_SHAPE_FILL_COLOR,
                                    configuration.getPropertyColor(VectorDataLayer.PROPERTY_NAME_SHAPE_FILL_COLOR,
                                                                   VectorDataLayer.DEFAULT_SHAPE_FILL_COLOR));
        layerConfiguration.setValue(VectorDataLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY,
                                    configuration.getPropertyDouble(
                                            VectorDataLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY,
                                            VectorDataLayer.DEFAULT_SHAPE_FILL_TRANSPARENCY));
*/
    }

    private GraticuleLayer createGraticuleLayer(AffineTransform i2mTransform) {
        final LayerType layerType = LayerTypeRegistry.getLayerType(GraticuleLayerType.class);
        final PropertySet template = layerType.createLayerConfig(null);
        template.setValue(GraticuleLayerType.PROPERTY_NAME_RASTER, getRaster());
        final GraticuleLayer graticuleLayer = (GraticuleLayer) layerType.createLayer(null, template);
        graticuleLayer.setId(ProductSceneView.GRATICULE_LAYER_ID);
        graticuleLayer.setVisible(false);
        graticuleLayer.setName("Graticule");
        applyGraticuleLayerStyle(configuration, graticuleLayer);
        return graticuleLayer;
    }

    static void applyGraticuleLayerStyle(PropertyMap configuration, Layer layer) {
        final PropertySet layerConfiguration = layer.getConfiguration();

        // Added multiple new properties here
        // Daniel Knowles - Sept 2018

        // Added section break properties

//        layerConfiguration.setValue(GraticuleLayerType.PROPERTY_NUM_GRID_LINES_NAME,
//                configuration.getPropertyInt(GraticuleLayerType.PROPERTY_NUM_GRID_LINES_NAME,
//                        GraticuleLayerType.PROPERTY_NUM_GRID_LINES_DEFAULT));


        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_NUM_GRID_LINES_NAME,
                GraticuleLayerType.PROPERTY_NUM_GRID_LINES_DEFAULT,
                GraticuleLayerType.PROPERTY_NUM_GRID_LINES_TYPE);


        // Grid Spacing Section

        addSectionPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_GRID_SPACING_SECTION_NAME);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_NAME,
                GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_DEFAULT,
                GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_GRID_SPACING_LON_NAME,
                GraticuleLayerType.PROPERTY_GRID_SPACING_LON_DEFAULT,
                GraticuleLayerType.PROPERTY_GRID_SPACING_LON_TYPE);


        // Labels Section

        addSectionPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_SECTION_NAME);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_NORTH_NAME,
                GraticuleLayerType.PROPERTY_LABELS_NORTH_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_NORTH_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_SOUTH_NAME,
                GraticuleLayerType.PROPERTY_LABELS_SOUTH_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_SOUTH_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_WEST_NAME,
                GraticuleLayerType.PROPERTY_LABELS_WEST_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_WEST_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_EAST_NAME,
                GraticuleLayerType.PROPERTY_LABELS_EAST_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_EAST_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_NAME,
                GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_NAME,
                GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME,
                GraticuleLayerType.PROPERTY_LABELS_INSIDE_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_INSIDE_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_ITALIC_NAME,
                GraticuleLayerType.PROPERTY_LABELS_ITALIC_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_ITALIC_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_BOLD_NAME,
                GraticuleLayerType.PROPERTY_LABELS_BOLD_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_BOLD_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_FONT_NAME,
                GraticuleLayerType.PROPERTY_LABELS_FONT_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_FONT_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_NAME,
                GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_NAME,
                GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_SIZE_NAME,
                GraticuleLayerType.PROPERTY_LABELS_SIZE_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_SIZE_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_LABELS_COLOR_NAME,
                GraticuleLayerType.PROPERTY_LABELS_COLOR_DEFAULT,
                GraticuleLayerType.PROPERTY_LABELS_COLOR_TYPE);


        // Gridlines Section

        addSectionPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_GRIDLINES_SECTION_NAME);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME,
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_DEFAULT,
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_NAME,
                GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_DEFAULT,
                GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_NAME,
                GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_DEFAULT,
                GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_NAME,
                GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_DEFAULT,
                GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_NAME,
                GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_DEFAULT,
                GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_TYPE);


        // Border Section

        addSectionPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_BORDER_SECTION_NAME);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_BORDER_SHOW_NAME,
                GraticuleLayerType.PROPERTY_BORDER_SHOW_DEFAULT,
                GraticuleLayerType.PROPERTY_BORDER_SHOW_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_BORDER_WIDTH_NAME,
                GraticuleLayerType.PROPERTY_BORDER_WIDTH_DEFAULT,
                GraticuleLayerType.PROPERTY_BORDER_WIDTH_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_BORDER_COLOR_NAME,
                GraticuleLayerType.PROPERTY_BORDER_COLOR_DEFAULT,
                GraticuleLayerType.PROPERTY_BORDER_COLOR_TYPE);


        // Tickmarks Section

        addSectionPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_NAME);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_NAME,
                GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT,
                GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_NAME,
                GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_DEFAULT,
                GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_NAME,
                GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT,
                GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_NAME,
                GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT,
                GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_TYPE);


        // Corner Labels Section

        addSectionPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_SECTION_NAME);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_NAME,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_DEFAULT,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_NAME,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_DEFAULT,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_NAME,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_DEFAULT,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_NAME,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_DEFAULT,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_TYPE);


        // Inside Labels Section

        addSectionPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_INSIDE_LABELS_SECTION_NAME);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_NAME,
                GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_DEFAULT,
                GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_TYPE);

        addPropertyToLayerConfiguration(configuration, layer,
                GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_NAME,
                GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_DEFAULT,
                GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_TYPE);


    }


    private static void addPropertyToLayerConfiguration(PropertyMap configuration, Layer layer, String propertyName, Object propertyDefault, Class type) {
        final PropertySet layerConfiguration = layer.getConfiguration();

        if (type == Boolean.class) {
            layerConfiguration.setValue(propertyName,
                    configuration.getPropertyBool(propertyName, (Boolean) propertyDefault));
        } else if (type == Double.class) {
            layerConfiguration.setValue(propertyName,
                    configuration.getPropertyDouble(propertyName, (Double) propertyDefault));
        } else if (type == Color.class) {
            layerConfiguration.setValue(propertyName,
                    configuration.getPropertyColor(propertyName, (Color) propertyDefault));
        } else if (type == Integer.class) {
            layerConfiguration.setValue(propertyName,
                    configuration.getPropertyInt(propertyName, (Integer) propertyDefault));
        } else if (type == String.class) {
            layerConfiguration.setValue(propertyName,
                    configuration.getPropertyString(propertyName, (String) propertyDefault));
        }

    }


    private static void addSectionPropertyToLayerConfiguration(PropertyMap configuration, Layer layer, String propertyName) {
        addPropertyToLayerConfiguration(configuration, layer, propertyName, true, Boolean.class);
    }


    private ColoredBandImageMultiLevelSource getColoredBandImageMultiLevelSource() {
        return coloredBandImageMultiLevelSource;
    }

    @Override
    public Product getProduct() {
        return getRaster().getProduct();
    }

    @Override
    public ProductNode getProductNode() {
        return getRaster();
    }

    private static class ImageLayerFilter implements LayerFilter {

        @Override
        public boolean accept(Layer layer) {
            return layer instanceof ImageLayer;
        }
    }
}
