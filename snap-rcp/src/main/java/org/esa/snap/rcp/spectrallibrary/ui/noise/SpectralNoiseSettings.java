package org.esa.snap.rcp.spectrallibrary.ui.noise;

import org.esa.snap.speclib.util.noise.SpectralNoiseKernelFactory;


public record SpectralNoiseSettings(String filterType, int kernelSize, double gaussianSigma, int sgPolynomialOrder,
                                    double rickerWidth, double trapezoidWidth, double trapezoidSlope) {

    public void validate() {
        SpectralNoiseKernelFactory factory = new SpectralNoiseKernelFactory(
                filterType, kernelSize, gaussianSigma, sgPolynomialOrder, rickerWidth, trapezoidWidth, trapezoidSlope
        );
        factory.validateFilterParameters();
    }

    public static SpectralNoiseSettings defaults() {
        return new SpectralNoiseSettings(
                SpectralNoiseKernelFactory.FILTER_SG, 11, 1.0, 3, 1.0, 3.0, 1.0
        );
    }
}
