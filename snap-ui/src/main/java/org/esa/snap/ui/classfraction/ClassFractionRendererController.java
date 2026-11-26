package org.esa.snap.ui.classfraction;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.multilevel.MultiLevelSource;
import eu.esa.snap.core.renderer.classfraction.ClassFractionImageLayerType;
import eu.esa.snap.core.renderer.classfraction.ClassFractionRendererConfig;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.ui.product.ProductSceneImage;

import java.util.List;


/**
 * Bridges the class-specific UI state to the existing supplied-layer scene-image seam.
 */
public final class ClassFractionRendererController {


    private final ClassFractionRendererModel model;


    public ClassFractionRendererController(ClassFractionRendererModel model) {
        this.model = model;
    }


    public ClassFractionRendererModel getModel() {
        return model;
    }

    public ClassFractionRendererConfig createConfiguration() {
        return model.createConfiguration();
    }

    public ProductSceneImage createSceneImage(PropertyMap applicationPreferences) {
        ClassFractionRendererConfig config = createConfiguration();
        ClassFractionImageLayerType layerType = new ClassFractionImageLayerType();
        ImageLayer layer = layerType.createLayer(model.getProduct(), config);

        MultiLevelSource source = layer.getConfiguration().getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE);
        List<RasterDataNode> rasters = model.getRows().stream().map(row -> (RasterDataNode) row.getBand()).toList();

        ProductSceneImage sceneImage = new ProductSceneImage(
                config.getDisplayName(), rasters.toArray(new RasterDataNode[0]), applicationPreferences, source, layer
        );
        sceneImage.initVectorDataCollectionLayer();
        sceneImage.initMaskCollectionLayer();
        return sceneImage;
    }
}
