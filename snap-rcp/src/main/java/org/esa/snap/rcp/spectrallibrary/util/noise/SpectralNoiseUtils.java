package org.esa.snap.rcp.spectrallibrary.util.noise;

import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.esa.snap.speclib.util.noise.SpectralNoiseReducer;

import java.util.UUID;


public class SpectralNoiseUtils {


    public static SpectralProfile createSmoothedProfile(SpectralProfile profile, double[] kernel, String suffix) {
        double[] values = profile.getSignature().getValues();
        boolean[] validMask = new boolean[values.length];
        for (int i = 0; i < values.length; i++) {
            double v = values[i];
            validMask[i] = !Double.isNaN(v) && !Double.isInfinite(v);
        }

        double[] filtered = new double[values.length];
        SpectralNoiseReducer.applyConvolution(values, validMask, kernel, filtered);

        String yUnit = profile.getSignature().getYUnitOrNull();
        SpectralSignature smoothedSignature = (yUnit == null)
                ? SpectralSignature.of(filtered)
                : SpectralSignature.of(filtered, yUnit);

        String baseName = (profile.getName() == null || profile.getName().isBlank())
                ? "Profile"
                : profile.getName().trim();
        String safeSuffix = (suffix == null) ? "" : suffix.trim();
        String newName = safeSuffix.isEmpty() ? baseName : baseName + safeSuffix;

        return new SpectralProfile(
                UUID.randomUUID(),
                newName,
                smoothedSignature,
                profile.getAttributes(),
                profile.getSourceRef().orElse(null)
        );
    }
}
