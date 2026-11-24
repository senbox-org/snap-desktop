package org.esa.snap.ui.classfraction;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig;
import eu.esa.snap.core.renderer.classfraction.ClassFractionImageLayerType;
import eu.esa.snap.core.renderer.classfraction.ClassFractionRendererConfig;
import eu.esa.snap.core.renderer.classfraction.ClassFractionStatisticsResult;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.ui.product.ProductSceneImage;
import org.esa.snap.ui.product.ProductSceneView;
import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;


public class ClassFractionRendererModelTest {


    @Test
    @STTM("SNAP-4339")
    public void buildsArbitraryClassConfigFromExplicitUiState() {
        Product product = product("classes", 4);
        ClassFractionRendererModel model = new ClassFractionRendererModel(product);
        model.addBand(product.getBand("class_3"));
        model.addBand(product.getBand("class_4"));
        model.setDisplayName("Land-cover probabilities");
        model.setDisplayScale(2.5);
        model.setValidityPolicy(ClassBandGroupConfig.ValidityPolicy.FINITE_AND_DECLARED_NODATA);

        ClassFractionRendererModel.ClassBandRow row = model.getRows().get(2);
        row.setDisplayName("Forest");
        row.setColor(new Color(12, 34, 56));
        row.setVisible(false);

        ClassFractionRendererConfig config = model.createConfiguration();

        assertEquals(4, config.getClassBandGroup().getBands().size());
        assertEquals("Land-cover probabilities", config.getDisplayName());
        assertEquals("Forest", config.getClassBandGroup().getBands().get(2).getDisplayName());
        assertEquals(0xFF0C2238, config.getClassBandGroup().getBands().get(2).getArgb());
        assertFalse(config.getClassBandGroup().getBands().get(2).isVisible());
        assertEquals(2.5, config.getClassBandGroup().getDisplayScale(), 0.0);
        assertEquals(ClassBandGroupConfig.ValidityPolicy.FINITE_AND_DECLARED_NODATA, config.getClassBandGroup().getValidityPolicy());
    }

    @Test
    @STTM("SNAP-4339")
    public void rejectsTooSmallUiSelection() {
        ClassFractionRendererModel model = new ClassFractionRendererModel(product("classes", 2));
        model.removeBand(1);

        try {
            model.createConfiguration();
            fail("Expected two-band validation failure");
        } catch (IllegalArgumentException expected) {
            assertEquals("At least two class bands are required", expected.getMessage());
        }

    }

    @Test
    @STTM("SNAP-4339")
    public void keepsAvailableBandChoicesConsistentWithTheClassSelection() {
        Product product = product("classes", 4);
        ClassFractionRendererModel model = new ClassFractionRendererModel(product);

        assertBandNames(model.getRows().stream().map(ClassFractionRendererModel.ClassBandRow::getBand).toList(), "class_1", "class_2");
        assertBandNames(model.getAvailableBands(), "class_3", "class_4");

        model.addBand(product.getBand("class_3"));
        assertBandNames(model.getAvailableBands(), "class_4");

        model.removeBand(1);
        assertBandNames(model.getRows().stream().map(ClassFractionRendererModel.ClassBandRow::getBand).toList(), "class_1", "class_3");
        assertBandNames(model.getAvailableBands(), "class_2", "class_4");
    }

    @Test
    @STTM("SNAP-4339")
    public void rejectsDuplicateAndForeignBands() {
        Product product = product("classes", 3);
        ClassFractionRendererModel model = new ClassFractionRendererModel(product);
        Band selected = product.getBand("class_1");

        assertBandRejected(model, selected, "Band is already selected: class_1");
        assertBandRejected(model, null, "A band from the selected product is required");
        assertBandRejected(model, product("other", 1).getBand("class_1"), "A band from the selected product is required");
    }

    @Test
    @STTM("SNAP-4339")
    public void opensSuppliedLayerSceneWithoutRgbImageInfo() {
        Product product = product("classes", 3);
        ClassFractionRendererModel model = new ClassFractionRendererModel(product);
        model.addBand(product.getBand("class_3"));
        ProductSceneImage sceneImage = new ClassFractionRendererController(model).createSceneImage(new DefaultPropertyMap());
        ProductSceneView view = new ProductSceneView(sceneImage);

        try {
            assertEquals(3, view.getNumRasters());
            assertTrue(view.getBaseImageLayer().getLayerType() instanceof ClassFractionImageLayerType);
            assertFalse(view.isRGB());
            assertNull(sceneImage.getImageInfo());
        } finally {
            view.dispose();
        }
    }

    @Test
    @STTM("SNAP-4339")
    public void reusesTheConfiguredClassContextForAllBandAndRoiStatistics() {
        Product product = product("classes", 3);

        for (Band band : product.getBands()) {
            band.setRasterData(ProductData.createInstance(new float[]{0, 0.5f, 1, 0, 0, 0.5f, 1, 0, 0, 0.5f, 1, 0}));
        }

        Mask roi = Mask.BandMathsType.create("positive", "positive", 4, 3, "class_1 > 0", Color.RED, 0.5);
        product.getMaskGroup().add(roi);
        ClassFractionRendererModel model = new ClassFractionRendererModel(product);
        model.addBand(product.getBand("class_3"));
        model.setDisplayScale(5.0);
        model.getRows().get(1).setVisible(false);

        ClassFractionStatisticsController controller = new ClassFractionStatisticsController(model);
        ClassFractionStatisticsResult result = controller.compute(roi, 4, ProgressMonitor.NULL);

        assertEquals(3, controller.getConfiguration().getClassBandGroup().getBands().size());
        assertEquals(5.0, controller.getConfiguration().getClassBandGroup().getDisplayScale(), 0.0);
        assertEquals(1, controller.getCompatibleRoiMasks().size());
        assertEquals(3, result.getBandStatistics().size());
        assertEquals(6, result.getValidPixelCount());
        assertFalse(result.getBandStatistics().get(1).getClassBand().isVisible());
        assertEquals(0.75, result.getBandStatistics().get(0).getMean(), 0.0);
        assertTrue(result.getValidityRule().contains("ROI 'positive'"));
    }

    @Test
    @STTM("SNAP-4339")
    public void exportsClassStatisticsWithProvenanceSummaryAndHistogramBins() {
        Product product = product("classes", 2);

        for (Band band : product.getBands()) {
            band.setRasterData(ProductData.createInstance(new float[]{0, 0.5f, 1, 0, 0, 0.5f, 1, 0, 0, 0.5f, 1, 0}));
        }

        ClassFractionStatisticsResult result = new ClassFractionStatisticsController(new ClassFractionRendererModel(product))
                .compute(null, 4, ProgressMonitor.NULL);

        String csv = ClassFractionStatisticsDialog.toCsv(result);

        assertTrue(csv.contains("\"ROI\",\"Whole scene\""));
        assertTrue(csv.contains("Class,Source band,Valid pixels,Minimum,Maximum,Mean,Std. deviation,P05,P25,Median,P75,P95"));
        assertTrue(csv.contains("Class,Histogram bin minimum,Histogram bin maximum,Pixel count"));
        assertFalse(csv.contains(",Visible,"));
    }


    private static Product product(String name, int bandCount) {
        Product product = new Product(name, "test", 4, 3);
        for (int index = 1; index <= bandCount; index++) {
            Band band = product.addBand("class_" + index, ProductData.TYPE_FLOAT32);
            band.setRasterData(ProductData.createInstance(new float[12]));
        }
        return product;
    }

    private static void assertBandNames(java.util.List<Band> bands, String... expectedNames) {
        assertArrayEquals(expectedNames, bands.stream().map(Band::getName).toArray(String[]::new));
    }

    private static void assertBandRejected(ClassFractionRendererModel model, Band band, String expectedMessage) {
        try {
            model.addBand(band);
            fail("Expected band selection to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals(expectedMessage, expected.getMessage());
        }
    }
}
