package org.esa.snap.rcp.session;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.renderer.classfraction.ClassBandConfig;
import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig;
import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig.ValidityPolicy;
import eu.esa.snap.core.renderer.classfraction.ClassFractionImageLayerType;
import eu.esa.snap.core.renderer.classfraction.ClassFractionRendererConfig;
import eu.esa.snap.core.renderer.classfraction.ClassFractionRendererConfigJson;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.ui.product.ProductSceneView;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ClassFractionSessionRestoreTest {


    @Test
    @STTM("SNAP-4339")
    public void restoresClassFractionSceneBeforeRgbInference() throws Exception {
        Product product = product();
        ProductManager manager = new ProductManager();
        manager.addProduct(product);
        Session.ViewRef ref = new Session.ViewRef(1, ProductSceneView.class.getName(), null, null, product.getRefNo(),
                                           null, "fractions", null, null, null,
                                           ClassFractionImageLayerType.class.getName(),
                                           ClassFractionRendererConfigJson.encode(config()),
                                            new Session.LayerRef[0]);

        ProductSceneView view = Session.createSceneView(ref, manager, new DefaultPropertyMap(), ProgressMonitor.NULL);

        assertTrue(view.getBaseImageLayer().getLayerType() instanceof ClassFractionImageLayerType);
        assertEquals("fractions", view.getSceneName());
        assertEquals(4, view.getRasters().length);
    }

    @Test
    @STTM("SNAP-4339")
    public void rejectsAChangedOrMissingConfiguredSource() throws Exception {
        Product product = product();
        ProductManager manager = new ProductManager();
        manager.addProduct(product);
        ClassFractionRendererConfig missingSourceConfig = new ClassFractionRendererConfig(
                "fractions",
                new ClassBandGroupConfig(Arrays.asList(
                        entry("a", 0xFFFF0000), entry("b", 0xFF00FF00), entry("c", 0xFF0000FF), entry("missing", 0xFFFFFF00)),
                        1.0, ValidityPolicy.STRICT_SOURCE_VALIDITY
                )
        );

        Session.ViewRef ref = new Session.ViewRef(1, ProductSceneView.class.getName(), null, null, product.getRefNo(),
                                           null, "fractions", null, null, null,
                                           ClassFractionImageLayerType.class.getName(),
                                           ClassFractionRendererConfigJson.encode(missingSourceConfig),
                                            new Session.LayerRef[0]);
        try {
            Session.createSceneView(ref, manager, new DefaultPropertyMap(), ProgressMonitor.NULL);
            fail("Expected an unresolved class renderer source");
        } catch (Exception expected) {
            assertEquals("Class renderer source is unresolved: missing", expected.getMessage());
        }
    }

    @Test
    @STTM("SNAP-4339")
    public void restoresThePrePackageRefactoringSessionDiscriminator() throws Exception {
        Product product = product();
        ProductManager manager = new ProductManager();
        manager.addProduct(product);
        Session.ViewRef ref = new Session.ViewRef(1, ProductSceneView.class.getName(), null, null, product.getRefNo(),
                                       null, "fractions", null, null, null,
                                       ClassFractionImageLayerType.LEGACY_TYPE_NAME,
                                       ClassFractionRendererConfigJson.encode(config()),
                                        new Session.LayerRef[0]);

        ProductSceneView view = Session.createSceneView(ref, manager, new DefaultPropertyMap(), ProgressMonitor.NULL);

        assertTrue(view.getBaseImageLayer().getLayerType() instanceof ClassFractionImageLayerType);
    }


    private static Product product() {
        Product product = new Product("p", "t", 2, 1);
        product.setRefNo(42);
        for (String name : Arrays.asList("a", "b", "c", "d")) {
            Band band = product.addBand(name, ProductData.TYPE_FLOAT32);
            band.setRasterData(ProductData.createInstance(new float[]{0.25f, 0.25f}));
        }
        return product;
    }

    private static ClassFractionRendererConfig config() {
        return new ClassFractionRendererConfig("fractions", new ClassBandGroupConfig(Arrays.asList(
                entry("a", 0xFFFF0000), entry("b", 0xFF00FF00), entry("c", 0xFF0000FF), entry("d", 0xFFFFFF00)),
                1.0, ValidityPolicy.STRICT_SOURCE_VALIDITY));
    }

    private static ClassBandConfig entry(String band, int color) {
        return new ClassBandConfig("p", band, band, color, true);
    }
}
