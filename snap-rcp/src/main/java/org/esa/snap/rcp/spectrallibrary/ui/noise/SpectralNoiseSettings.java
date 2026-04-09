package org.esa.snap.rcp.spectrallibrary.ui.noise;

import org.esa.snap.speclib.util.noise.SpectralNoiseKernelFactory;

public record SpectralNoiseSettings(String filterType, int kernelSize, double gaussianSigma, int sgPolynomialOrder) {

    public void validate() {
        SpectralNoiseKernelFactory factory =
                new SpectralNoiseKernelFactory(filterType, kernelSize, gaussianSigma, sgPolynomialOrder);
        factory.validateFilterParameters();
    }

    public static SpectralNoiseSettings defaults() {
        return new SpectralNoiseSettings(
                SpectralNoiseKernelFactory.FILTER_SG, 11, 1.0, 3
        );
    }
}
