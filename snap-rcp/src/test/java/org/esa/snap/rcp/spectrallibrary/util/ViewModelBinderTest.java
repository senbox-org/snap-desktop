package org.esa.snap.rcp.spectrallibrary.util;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.rcp.spectrallibrary.model.SpectralProfileTableModel;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.junit.Test;

import java.awt.Color;
import java.awt.Paint;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;


public class ViewModelBinderTest {


    @Test
    @STTM("SNAP-4338")
    public void test_ResolvePreviewPaintsUsesPersistedColorForNewFilteredProfile() {
        UUID id = UUID.randomUUID();
        SpectralProfile filtered = profile(id, "filtered", "#2CA02C");

        Map<UUID, Paint> paints = ViewModelBinder.resolvePreviewPaints(Map.of(), List.of(filtered), List.of(filtered));

        assertEquals(new Color(0x2C, 0xA0, 0x2C), paints.get(id));
    }

    @Test
    @STTM("SNAP-4338")
    public void test_ResolvePreviewPaintsUsesLatestLibraryColorInsteadOfStalePreviewOrOverride() {
        UUID id = UUID.randomUUID();
        SpectralProfile stalePreviewProfile = profile(id, "profile", "#FF0000");
        SpectralProfile currentLibraryProfile = profile(id, "profile", "#0000FF");

        Map<UUID, Paint> paints = ViewModelBinder.resolvePreviewPaints(Map.of(id, Color.GREEN), List.of(stalePreviewProfile), List.of(currentLibraryProfile));

        assertEquals(Color.BLUE, paints.get(id));
    }

    @Test
    @STTM("SNAP-4338")
    public void test_ResolvePreviewPaintsRetainsTemporaryExtractionOverrideWithoutPersistedColor() {
        UUID id = UUID.randomUUID();
        SpectralProfile extracted = profile(id, "extracted", null);

        Map<UUID, Paint> paints = ViewModelBinder.resolvePreviewPaints(Map.of(id, Color.MAGENTA), List.of(extracted), List.of());

        assertEquals(Color.MAGENTA, paints.get(id));
    }

    private static SpectralProfile profile(UUID id, String name, String displayColor) {
        Map<String, AttributeValue> attributes = displayColor == null
                ? Map.of()
                : Map.of(SpectralProfileTableModel.ATTR_DISPLAY_COLOR, AttributeValue.ofString(displayColor));
        return new SpectralProfile(id, name, SpectralSignature.of(new double[]{1.0, 2.0}), attributes, null);
    }
}
