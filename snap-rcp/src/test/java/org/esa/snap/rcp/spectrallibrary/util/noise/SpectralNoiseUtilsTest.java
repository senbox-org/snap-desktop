package org.esa.snap.rcp.spectrallibrary.util.noise;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;


public class SpectralNoiseUtilsTest {


    private static final double DOUBLE_ERR = 1.0e-10;


    @Test
    @STTM("SNAP-4173")
    public void test_CreateSmoothedProfileCreatesSmoothedProfileWithSuffix() {
        Map<String, AttributeValue> attrs = new LinkedHashMap<>();
        attrs.put("material", AttributeValue.ofString("vegetation"));

        SpectralProfile.SourceRef sourceRef = new SpectralProfile.SourceRef(12, 34, 0, "prodA");
        SpectralProfile profile = new SpectralProfile(
                UUID.randomUUID(),
                "Spec1",
                SpectralSignature.of(new double[]{10.0, 20.0, 30.0}, "reflectance"),
                attrs,
                sourceRef
        );

        double[] kernel = {0.25, 0.5, 0.25};

        SpectralProfile smoothed = SpectralNoiseUtils.createSmoothedProfile(profile, kernel, "_smoothed");

        assertNotNull(smoothed);
        assertNotEquals(profile.getId(), smoothed.getId());
        assertEquals("Spec1_smoothed", smoothed.getName());
        assertArrayEquals(new double[]{12.5, 20.0, 27.5}, smoothed.getSignature().getValues(), DOUBLE_ERR);
        assertEquals("reflectance", smoothed.getSignature().getYUnitOrNull());

        assertEquals(profile.getAttributes(), smoothed.getAttributes());
        assertTrue(smoothed.getSourceRef().isPresent());
        assertEquals(12, smoothed.getSourceRef().get().getX());
        assertEquals(34, smoothed.getSourceRef().get().getY());
        assertEquals(0, smoothed.getSourceRef().get().getLevel());
        assertEquals("prodA", smoothed.getSourceRef().get().getProductId().orElse(null));
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateSmoothedProfileUsesFallbackNameWhenOriginalNameIsBlank() {
        SpectralProfile profile = new SpectralProfile(
                UUID.randomUUID(),
                "   ",
                SpectralSignature.of(new double[]{1.0, 2.0, 3.0}),
                Map.of(),
                null
        );

        double[] kernel = {0.25, 0.5, 0.25};

        SpectralProfile smoothed = SpectralNoiseUtils.createSmoothedProfile(profile, kernel, "_smoothed");

        assertEquals("Profile_smoothed", smoothed.getName());
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateSmoothedProfileKeepsBaseNameWhenSuffixIsNullOrBlank() {
        SpectralProfile profile = new SpectralProfile(
                UUID.randomUUID(),
                "  Spec1  ",
                SpectralSignature.of(new double[]{1.0, 2.0, 3.0}),
                Map.of(),
                null
        );

        double[] kernel = {0.25, 0.5, 0.25};

        SpectralProfile smoothedWithNullSuffix = SpectralNoiseUtils.createSmoothedProfile(profile, kernel, null);
        SpectralProfile smoothedWithBlankSuffix = SpectralNoiseUtils.createSmoothedProfile(profile, kernel, "   ");

        assertEquals("Spec1", smoothedWithNullSuffix.getName());
        assertEquals("Spec1", smoothedWithBlankSuffix.getName());
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateSmoothedProfilePreservesNaNValuesAtInvalidCenters() {
        SpectralProfile profile = new SpectralProfile(
                UUID.randomUUID(),
                "Spec1",
                SpectralSignature.of(new double[]{10.0, Double.NaN, 30.0}, "reflectance"),
                Map.of(),
                null
        );

        double[] kernel = {0.25, 0.5, 0.25};

        SpectralProfile smoothed = SpectralNoiseUtils.createSmoothedProfile(profile, kernel, "_sm");

        double[] values = smoothed.getSignature().getValues();
        assertEquals(10.0, values[0], DOUBLE_ERR);
        assertTrue(Double.isNaN(values[1]));
        assertEquals(30.0, values[2], DOUBLE_ERR);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateSmoothedProfileDoesNotModifyOriginalProfile() {
        double[] originalValues = {10.0, 20.0, 30.0};
        SpectralProfile profile = new SpectralProfile(
                UUID.randomUUID(),
                "Spec1",
                SpectralSignature.of(originalValues, "reflectance"),
                Map.of(),
                null
        );

        double[] kernel = {0.25, 0.5, 0.25};

        SpectralProfile smoothed = SpectralNoiseUtils.createSmoothedProfile(profile, kernel, "_sm");

        assertArrayEquals(new double[]{10.0, 20.0, 30.0}, profile.getSignature().getValues(), DOUBLE_ERR);
        assertArrayEquals(new double[]{12.5, 20.0, 27.5}, smoothed.getSignature().getValues(), DOUBLE_ERR);
    }

    @Test
    @STTM("SNAP-4173")
    public void test_CreateSmoothedProfileKeepsMissingSourceRefMissing() {
        SpectralProfile profile = new SpectralProfile(
                UUID.randomUUID(),
                "Spec1",
                SpectralSignature.of(new double[]{1.0, 2.0, 3.0}),
                Map.of(),
                null
        );

        double[] kernel = {0.25, 0.5, 0.25};

        SpectralProfile smoothed = SpectralNoiseUtils.createSmoothedProfile(profile, kernel, "_sm");

        assertTrue(smoothed.getSourceRef().isEmpty());
    }
}