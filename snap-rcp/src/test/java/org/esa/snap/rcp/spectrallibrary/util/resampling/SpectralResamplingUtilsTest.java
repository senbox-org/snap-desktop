package org.esa.snap.rcp.spectrallibrary.util.resampling;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.io.csv.util.CsvTable;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;


public class SpectralResamplingUtilsTest {


    private static final double DOUBLE_ERR = 1.0e-10;


    @Test
    @STTM("SNAP-4174")
    public void test_createResampledSpectralProfile() throws IOException, URISyntaxException {
        Map<String, AttributeValue> attrs = new LinkedHashMap<>();
        attrs.put("material", AttributeValue.ofString("vegetation"));

        SpectralProfile.SourceRef sourceRef = new SpectralProfile.SourceRef(12, 34, 0, "prodA");
        SpectralProfile profile = new SpectralProfile(
                UUID.randomUUID(),
                "Spec1",
                SpectralSignature.of(new double[]{
                        10.0, 30.0, 20.0, 15.0, 45.0, 35.0, 55.0, 60.0, 45.0, 40.0
                }, "radiance"),
                attrs,
                sourceRef
        );


        double[] srcWvls = new double[]{
                500.0, 550.0, 600.0, 650.0, 700.0, 750.0, 800.0, 850.0, 900.0, 950.0
        };
        CsvTable fwhmTable = SpectralResamplingUtils.readFwhmFromCsv("OLCI");
        SpectralProfile resampled = SpectralResamplingUtils.createResampledProfile(profile,
                srcWvls, fwhmTable, "OLCI", "_resampled");

        assertNotNull(resampled);
        assertNotEquals(profile.getId(), resampled.getId());
        assertEquals("Spec1_resampled", resampled.getName());
        assertArrayEquals(new double[]{
                Double.NaN, Double.NaN, Double.NaN, 10.0, 10.0, 30.0, Double.NaN,
                Double.NaN, Double.NaN, Double.NaN, 45.0, 35.0, Double.NaN, Double.NaN,
                Double.NaN, Double.NaN, 60.0, Double.NaN, 45.0, 40.0, Double.NaN
        }, resampled.getSignature().getValues(), DOUBLE_ERR);
        assertEquals("radiance", resampled.getSignature().getYUnitOrNull());

        assertEquals(profile.getAttributes(), resampled.getAttributes());
        assertTrue(resampled.getSourceRef().isPresent());
        assertEquals(12, resampled.getSourceRef().get().getX());
        assertEquals(34, resampled.getSourceRef().get().getY());
        assertEquals(0, resampled.getSourceRef().get().getLevel());
        assertEquals("prodA", resampled.getSourceRef().get().getProductId().orElse(null));
    }

}