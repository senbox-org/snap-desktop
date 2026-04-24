package org.esa.snap.rcp.spectrallibrary.ui.resampling;

import org.esa.snap.speclib.util.resampling.SpectralResamplingSensor;

public record SpectralResamplingSettings(String targetSensorName) {

    public static SpectralResamplingSettings defaults() {
        return new SpectralResamplingSettings(
                SpectralResamplingSensor.ENMAP.getName()
        );
    }
}
