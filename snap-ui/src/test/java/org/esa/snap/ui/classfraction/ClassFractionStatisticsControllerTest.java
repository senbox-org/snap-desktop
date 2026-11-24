package org.esa.snap.ui.classfraction;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.renderer.classfraction.ClassBandConfig;
import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig;
import eu.esa.snap.core.renderer.classfraction.ClassFractionRendererConfig;
import eu.esa.snap.core.renderer.classfraction.ClassFractionStatisticsResult;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ClassFractionStatisticsControllerTest {


    @Test
    @STTM("SNAP-4339")
    public void exposesOnlyRoisOnTheConfiguredClassGridAndRejectsOthers() {
        Product product = product();
        ClassFractionStatisticsController controller = new ClassFractionStatisticsController(new ClassFractionRendererModel(product));
        Mask compatible = mask("compatible", 4, 3);
        Mask differentSize = mask("different-size", 3, 3);
        Mask differentTransform = mask("different-transform", 4, 3);
        differentTransform.setImageToModelTransform(AffineTransform.getTranslateInstance(1.0, 0.0));
        product.getMaskGroup().add(compatible);
        product.getMaskGroup().add(differentSize);
        product.getMaskGroup().add(differentTransform);

        assertEquals(List.of(compatible), controller.getCompatibleRoiMasks());

        try {
            controller.compute(differentTransform, 4, ProgressMonitor.NULL);
            fail("Expected incompatible ROI rejection");
        } catch (IllegalArgumentException expected) {
            assertEquals("Selected ROI mask is not compatible with the configured class grid", expected.getMessage());
        }
    }

    @Test
    @STTM("SNAP-4339")
    public void computesStatisticsFromAnExplicitRestoredConfiguration() {
        Product product = product();
        Band first = product.getBand("first");
        Band second = product.getBand("second");
        first.setRasterData(ProductData.createInstance(new float[]{0, 0.25f, 0.5f, 0.75f, 1, 0, 0.25f, 0.5f, 0.75f, 1, 0, 0.25f}));
        second.setRasterData(ProductData.createInstance(new float[]{1, 0.75f, 0.5f, 0.25f, 0, 1, 0.75f, 0.5f, 0.25f, 0, 1, 0.75f}));
        Mask roi = mask("selected", 4, 3);
        product.getMaskGroup().add(roi);

        ClassFractionRendererConfig configuration = new ClassFractionRendererConfig("Restored class view",
                new ClassBandGroupConfig(Arrays.asList(
                        new ClassBandConfig(product.getName(), first.getName(), "Water probability", 0xFF0000FF, true),
                        new ClassBandConfig(product.getName(), second.getName(), "Land probability", 0xFF00FF00, false)),
                        2.0, ClassBandGroupConfig.ValidityPolicy.STRICT_SOURCE_VALIDITY));
        ClassFractionStatisticsController controller = new ClassFractionStatisticsController(configuration, Arrays.asList(first, second), product);

        ClassFractionStatisticsResult result = controller.compute(roi, 4, ProgressMonitor.NULL);

        assertEquals("selected", result.getRoiName());
        assertEquals(12, result.getValidPixelCount());
        assertEquals("Water probability", result.getBandStatistics().get(0).getClassBand().getDisplayName());
        assertEquals("Land probability", result.getBandStatistics().get(1).getClassBand().getDisplayName());
        assertTrue(result.getValidityRule().contains("ROI 'selected'"));
    }

    private static Product product() {
        Product product = new Product("classes", "test", 4, 3);
        product.addBand("first", ProductData.TYPE_FLOAT32);
        product.addBand("second", ProductData.TYPE_FLOAT32);
        return product;
    }

    private static Mask mask(String name, int width, int height) {
        return Mask.BandMathsType.create(name, name, width, height, "true", Color.RED, 0.5);
    }
}
