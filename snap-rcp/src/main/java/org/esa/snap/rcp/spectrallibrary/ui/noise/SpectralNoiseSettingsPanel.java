package org.esa.snap.rcp.spectrallibrary.ui.noise;

import org.esa.snap.speclib.util.noise.SpectralNoiseKernelFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Map;


public class SpectralNoiseSettingsPanel extends JPanel {


    private final JComboBox<String> filterMethodCombo = new JComboBox<>(new String[]{
            SpectralNoiseKernelFactory.FILTER_SG,
            SpectralNoiseKernelFactory.FILTER_GAUSSIAN,
            SpectralNoiseKernelFactory.FILTER_BOX,
            SpectralNoiseKernelFactory.FILTER_RICKER_WAVELET,
            SpectralNoiseKernelFactory.FILTER_TRAPEZOID
    });

    private final JSpinner kernelSizeSpinner = new JSpinner(new SpinnerNumberModel(11, 3, 9999, 1));
    private final JSpinner gaussianSigmaSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.000001, 9999.0, 0.1));
    private final JSpinner sgPolynomialOrderSpinner = new JSpinner(new SpinnerNumberModel(3, 0, 9999, 1));
    private final JSpinner rickerWidthSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.000001, 9999.0, 0.1));
    private final JSpinner trapezoidWidthSpinner = new JSpinner(new SpinnerNumberModel(3.0, 0.000001, 9999.0, 0.1));
    private final JSpinner trapezoidSlopeSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.000001, 9999.0, 0.1));

    private final JLabel gaussianSigmaLabel = new JLabel("Gaussian Sigma:");
    private final JLabel sgPolynomialOrderLabel = new JLabel("Polynomial Order:");
    private final JLabel rickerWidthLabel = new JLabel("Ricker Width:");
    private final JLabel trapezoidWidthLabel = new JLabel("Trapezoid Width:");
    private final JLabel trapezoidSlopeLabel = new JLabel("Trapezoid Slope:");


    public SpectralNoiseSettingsPanel() {
        super(new GridBagLayout());

        filterMethodCombo.addActionListener(e -> updateFilterSpecificControls());

        GridBagConstraints gbc = createConstraints();
        addRow(this, gbc, 0, new JLabel("Filter Method:"), filterMethodCombo);
        addRow(this, gbc, 1, new JLabel("Kernel Size:"), kernelSizeSpinner);
        addRow(this, gbc, 2, gaussianSigmaLabel, gaussianSigmaSpinner);
        addRow(this, gbc, 3, sgPolynomialOrderLabel, sgPolynomialOrderSpinner);
        addRow(this, gbc, 4, rickerWidthLabel, rickerWidthSpinner);
        addRow(this, gbc, 5, trapezoidWidthLabel, trapezoidWidthSpinner);
        addRow(this, gbc, 6, trapezoidSlopeLabel, trapezoidSlopeSpinner);

        setSettings(SpectralNoiseSettings.defaults());
        updateFilterSpecificControls();
    }


    public SpectralNoiseSettings getSettings() {
        return new SpectralNoiseSettings(
                (String) filterMethodCombo.getSelectedItem(),
                ((Number) kernelSizeSpinner.getValue()).intValue(),
                ((Number) gaussianSigmaSpinner.getValue()).doubleValue(),
                ((Number) sgPolynomialOrderSpinner.getValue()).intValue(),
                ((Number) rickerWidthSpinner.getValue()).doubleValue(),
                ((Number) trapezoidWidthSpinner.getValue()).doubleValue(),
                ((Number) trapezoidSlopeSpinner.getValue()).doubleValue()
        );
    }

    public void setSettings(SpectralNoiseSettings settings) {
        SpectralNoiseSettings s = settings != null ? settings : SpectralNoiseSettings.defaults();
        filterMethodCombo.setSelectedItem(s.filterType());
        kernelSizeSpinner.setValue(s.kernelSize());
        gaussianSigmaSpinner.setValue(s.gaussianSigma());
        sgPolynomialOrderSpinner.setValue(s.sgPolynomialOrder());
        rickerWidthSpinner.setValue(s.rickerWidth());
        trapezoidWidthSpinner.setValue(s.trapezoidWidth());
        trapezoidSlopeSpinner.setValue(s.trapezoidSlope());
        updateFilterSpecificControls();
    }

    public void applyParameterMap(Map<String, Object> paramMap) {
        setSettings(new SpectralNoiseSettings(
                paramMap.get("filterType") instanceof String s ? s : SpectralNoiseKernelFactory.FILTER_SG,
                paramMap.get("kernelSize") instanceof Number n ? n.intValue() : 11,
                paramMap.get("gaussianSigma") instanceof Number n ? n.doubleValue() : 1.0,
                paramMap.get("sgPolynomialOrder") instanceof Number n ? n.intValue() : 3,
                paramMap.get("rickerWidth") instanceof Number n ? n.doubleValue() : 1.0,
                paramMap.get("trapezoidWidth") instanceof Number n ? n.doubleValue() : 3.0,
                paramMap.get("trapezoidSlope") instanceof Number n ? n.doubleValue() : 1.0
        ));
    }

    public void updateParameterMap(Map<String, Object> paramMap) {
        SpectralNoiseSettings s = getSettings();
        paramMap.put("filterType", s.filterType());
        paramMap.put("kernelSize", s.kernelSize());
        paramMap.put("gaussianSigma", s.gaussianSigma());
        paramMap.put("sgPolynomialOrder", s.sgPolynomialOrder());
        paramMap.put("rickerWidth", s.rickerWidth());
        paramMap.put("trapezoidWidth", s.trapezoidWidth());
        paramMap.put("trapezoidSlope", s.trapezoidSlope());
    }

    public String validateParameters() {
        try {
            getSettings().validate();
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private void updateFilterSpecificControls() {
        String filterMethod = (String) filterMethodCombo.getSelectedItem();
        boolean gaussian = SpectralNoiseKernelFactory.FILTER_GAUSSIAN.equals(filterMethod);
        boolean sg = SpectralNoiseKernelFactory.FILTER_SG.equals(filterMethod);
        boolean ricker = SpectralNoiseKernelFactory.FILTER_RICKER_WAVELET.equals(filterMethod);
        boolean trapezoid = SpectralNoiseKernelFactory.FILTER_TRAPEZOID.equals(filterMethod);
        boolean usesKernelSize = !ricker && !trapezoid;

        kernelSizeSpinner.setEnabled(usesKernelSize);
        gaussianSigmaLabel.setEnabled(gaussian);
        gaussianSigmaSpinner.setEnabled(gaussian);
        sgPolynomialOrderLabel.setEnabled(sg);
        sgPolynomialOrderSpinner.setEnabled(sg);
        rickerWidthLabel.setEnabled(ricker);
        rickerWidthSpinner.setEnabled(ricker);
        trapezoidWidthLabel.setEnabled(trapezoid);
        trapezoidWidthSpinner.setEnabled(trapezoid);
        trapezoidSlopeLabel.setEnabled(trapezoid);
        trapezoidSlopeSpinner.setEnabled(trapezoid);
    }

    private static GridBagConstraints createConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        return gbc;
    }

    private static void addRow(JPanel panel, GridBagConstraints gbc, int row, JComponent label, JComponent component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, gbc);
    }
}
