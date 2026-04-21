package org.esa.snap.rcp.spectrallibrary.util.resampling;

import org.esa.snap.speclib.io.csv.util.CsvTable;
import org.esa.snap.speclib.io.csv.util.CsvUtils;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.esa.snap.speclib.util.resampling.SpectralResampling;
import org.esa.snap.speclib.util.resampling.SpectralResponseFunction;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.esa.snap.core.util.Debug.assertNotNull;


/**
 * Utility class for interaction of Spectral Resampling with Spectral Library API.
 *
 * @author olafd
 */
public class SpectralResamplingUtils {

    /**
     * Wrapper method for Spectral Resampling call.
     * Provides a {@link  SpectralProfile} compatible with Spectral Library API.
     *
     * @param profile - spectral profile containing the input spectrum
     * @param srcWavelengths - array of source spectrum wavelengths
     * @param fwhmTable - {@link CsvTable} with FWHM input
     * @param targetSensorName - name of target sensor
     * @param suffix - suffix of new library
     *
     * @return spectral profile containing the resampled spectrum
     * @throws IOException -
     * @throws URISyntaxException -
     */
    public static SpectralProfile createResampledProfile(SpectralProfile profile,
                                                         double[] srcWavelengths,
                                                         CsvTable fwhmTable,
                                                         String targetSensorName,
                                                         String suffix) throws IOException, URISyntaxException {

        double[] values = profile.getSignature().getValues();
        SpectralResponseFunction srf = new SpectralResponseFunction(targetSensorName);

        srf.setFWHMList(fwhmTable);
        final List<SpectralResponseFunction.FWHM> fwhmList = srf.getFwhmList();

        final List<SpectralResponseFunction> fullyDefinedSrf = SpectralResponseFunction.getFullyDefinedSrf(fwhmList);

        final double[] resampled = SpectralResampling.resample(values, srcWavelengths, fullyDefinedSrf);

        String yUnit = profile.getSignature().getYUnitOrNull();
        SpectralSignature resampledSignature = (yUnit == null)
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
                resampledSignature,
                profile.getAttributes(),
                profile.getSourceRef().orElse(null)
        );
    }

    /**
     * Reads FWHM info from CSV file and provides as {link CsvTable}.
     *
     * @param targetSensorName - name of target sensor
     *
     * @return CsvTable with FWHM info.
     * @throws IOException -
     * @throws URISyntaxException -
     */
    public static CsvTable readFwhmFromCsv(String targetSensorName) throws IOException, URISyntaxException {
        final URL resource = SpectralResponseFunction.class.getResource("fwhm_" + targetSensorName + ".csv");
        assertNotNull(resource);
        final File csvFile = new File(Objects.requireNonNull(resource).toURI());
        return CsvUtils.read(csvFile.toPath());
    }
}
