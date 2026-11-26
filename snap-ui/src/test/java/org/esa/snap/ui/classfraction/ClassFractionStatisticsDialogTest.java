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
import java.util.Arrays;

import static org.junit.Assert.assertTrue;


public class ClassFractionStatisticsDialogTest {


    @Test
    @STTM("SNAP-4339")
    public void exportsQuotedMetadataStatisticsAndHistogramRowsAsCsv() {
        Product product = new Product("Product, \"one\"", "test", 2, 1);
        Band first = product.addBand("first", ProductData.TYPE_FLOAT32);
        Band second = product.addBand("second", ProductData.TYPE_FLOAT32);
        first.setRasterData(ProductData.createInstance(new float[]{0, 1}));
        second.setRasterData(ProductData.createInstance(new float[]{1, 0}));
        Mask roi = Mask.BandMathsType.create("ROI, \"selected\"", "roi", 2, 1, "true", Color.RED, 0.5);
        product.getMaskGroup().add(roi);
        ClassFractionRendererConfig configuration = new ClassFractionRendererConfig("view",
                new ClassBandGroupConfig(Arrays.asList(
                        new ClassBandConfig(product.getName(), "first", "Class, \"one\"", 0xFFFF0000, true),
                        new ClassBandConfig(product.getName(), "second", "Class two", 0xFF00FF00, true)),
                        1.0, ClassBandGroupConfig.ValidityPolicy.STRICT_SOURCE_VALIDITY));

        ClassFractionStatisticsResult result = new ClassFractionStatisticsController(configuration, Arrays.asList(first, second), product)
                .compute(roi, 2, ProgressMonitor.NULL);
        String csv = ClassFractionStatisticsDialog.toCsv(result);

        assertTrue(csv.contains("\"Product\",\"Product, \"\"one\"\"\""));
        assertTrue(csv.contains("\"ROI\",\"ROI, \"\"selected\"\"\""));
        assertTrue(csv.contains("\"Class, \"\"one\"\"\",\"first\",\"2\",\"0.0\",\"1.0\""));
        assertTrue(csv.contains("\"Class, \"\"one\"\"\",\"0.0\",\"0.5\",\"1\""));
        assertTrue(csv.contains("\"Class, \"\"one\"\"\",\"0.5\",\"1.0\",\"1\""));
    }
}
