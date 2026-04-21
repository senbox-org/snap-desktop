package org.esa.snap.rcp.spectrallibrary.util.resampling;

import org.esa.snap.speclib.io.csv.util.CsvTable;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.esa.snap.speclib.util.resampling.SpectralResampling;
import org.esa.snap.speclib.util.resampling.SpectralResponseFunction;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import static org.esa.snap.core.util.Debug.assertNotNull;


public class SpectralResamplingUtils {


    public static SpectralProfile createResampledProfile(SpectralProfile profile, double[] srcWavelengths, String targetSensorName, String suffix) throws IOException, URISyntaxException {
        double[] values = profile.getSignature().getValues();
        boolean[] validMask = new boolean[values.length];
        for (int i = 0; i < values.length; i++) {
            double v = values[i];
            validMask[i] = !Double.isNaN(v) && !Double.isInfinite(v);
        }


        SpectralResponseFunction srf = new SpectralResponseFunction(targetSensorName);

        final URL resource = SpectralResponseFunction.class.getResource("fwhm_" + srf.getID() + ".csv");
        assertNotNull(resource);
        final File csvFile = new File(resource.toURI());
        final CsvTable fwhmTable = SpectralResponseFunction.readFwhmFromCsv(csvFile);

        srf.setFWHMList(fwhmTable);
        final List<SpectralResponseFunction.FWHM> fwhmList = srf.getFwhmList();

        final List<SpectralResponseFunction> fullyDefinedSrf = SpectralResponseFunction.getFullyDefinedSrf(fwhmList);

        final double[] resampled = SpectralResampling.resample(values, srcWavelengths, fullyDefinedSrf);

        String yUnit = profile.getSignature().getYUnitOrNull();
        SpectralSignature smoothedSignature = (yUnit == null)
                ? SpectralSignature.of(resampled)
                : SpectralSignature.of(resampled, yUnit);

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
