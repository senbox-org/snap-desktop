package org.esa.snap.rcp.colormanip;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.multilevel.MultiLevelSource;
import com.bc.ceres.annotation.STTM;
import eu.esa.snap.core.renderer.classfraction.ClassBandConfig;
import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig;
import eu.esa.snap.core.renderer.classfraction.ClassFractionImageLayerType;
import eu.esa.snap.core.renderer.classfraction.ClassFractionRendererConfig;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.ui.product.ProductSceneImage;
import org.esa.snap.ui.product.ProductSceneView;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;


public class ClassFractionColorManipulationTest {


    @Test
    @STTM("SNAP-4339")
    public void doesNotTreatAClassFractionViewAsImageInfoEditable() {
        Product product = new Product("classes", "test", 2, 2);
        Band first = product.addBand("first", ProductData.TYPE_FLOAT32);
        Band second = product.addBand("second", ProductData.TYPE_FLOAT32);
        first.setRasterData(ProductData.createInstance(new float[]{0, 0.5f, 1, 0}));
        second.setRasterData(ProductData.createInstance(new float[]{1, 0.5f, 0, 1}));

        ClassFractionRendererConfig configuration = new ClassFractionRendererConfig("Class fractions",
                new ClassBandGroupConfig(Arrays.asList(
                        new ClassBandConfig(product.getName(), first.getName(), "First", 0xFFFF0000, true),
                        new ClassBandConfig(product.getName(), second.getName(), "Second", 0xFF00FF00, true)),
                        1.0, ClassBandGroupConfig.ValidityPolicy.STRICT_SOURCE_VALIDITY));
        ClassFractionImageLayerType layerType = new ClassFractionImageLayerType();
        ImageLayer layer = layerType.createLayer(product, configuration);
        MultiLevelSource source = layer.getConfiguration().getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE);
        ProductSceneView view = new ProductSceneView(new ProductSceneImage("Class fractions", new Band[]{first, second},
                new DefaultPropertyMap(), source, layer));

        try {
            ColorFormModel model = new ColorFormModel();
            model.setProductSceneView(view);

            assertNull(view.getImageInfo());
            assertFalse(model.isValid());
            new BrightnessContrastPanel(null).productSceneViewSelected(view);
        } finally {
            view.dispose();
        }
    }
}
