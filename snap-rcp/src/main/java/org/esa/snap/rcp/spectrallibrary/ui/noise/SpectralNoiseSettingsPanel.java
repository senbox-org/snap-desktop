package org.esa.snap.rcp.spectrallibrary.ui.noise;

import org.esa.snap.speclib.util.noise.SpectralNoiseKernelFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Map;


public class SpectralNoiseSettingsPanel extends JPanel {


    private final JComboBox<String> filterMethodCombo = new JComboBox<>(new String[]{
            SpectralNoiseKernelFactory.FILTER_SG,
            SpectralNoiseKernelFactory.FILTER_GAUSSIAN,
            SpectralNoiseKernelFactory.FILTER_BOX
    });

    private final JSpinner kernelSizeSpinner = new JSpinner(new SpinnerNumberModel(11, 3, 9999, 1));
    private final JSpinner gaussianSigmaSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.000001, 9999.0, 0.1));
    private final JSpinner sgPolynomialOrderSpinner = new JSpinner(new SpinnerNumberModel(3, 0, 9999, 1));

    private final JLabel gaussianSigmaLabel = new JLabel("Gaussian Sigma:");
    private final JLabel sgPolynomialOrderLabel = new JLabel("Polynomial Order:");


    public SpectralNoiseSettingsPanel() {
        super(new GridBagLayout());

        filterMethodCombo.addActionListener(e -> updateFilterSpecificControls());

        GridBagConstraints gbc = createConstraints();
        addRow(this, gbc, 0, new JLabel("Filter Method:"), filterMethodCombo);
        addRow(this, gbc, 1, new JLabel("Kernel Size:"), kernelSizeSpinner);
        addRow(this, gbc, 2, gaussianSigmaLabel, gaussianSigmaSpinner);
        addRow(this, gbc, 3, sgPolynomialOrderLabel, sgPolynomialOrderSpinner);

        setSettings(SpectralNoiseSettings.defaults());
        updateFilterSpecificControls();
    }


    public SpectralNoiseSettings getSettings() {
        return new SpectralNoiseSettings(
                (String) filterMethodCombo.getSelectedItem(),
                ((Number) kernelSizeSpinner.getValue()).intValue(),
                ((Number) gaussianSigmaSpinner.getValue()).doubleValue(),
                ((Number) sgPolynomialOrderSpinner.getValue()).intValue()
        );
    }

    public void setSettings(SpectralNoiseSettings settings) {
        SpectralNoiseSettings s = settings != null ? settings : SpectralNoiseSettings.defaults();
        filterMethodCombo.setSelectedItem(s.filterType());
        kernelSizeSpinner.setValue(s.kernelSize());
        gaussianSigmaSpinner.setValue(s.gaussianSigma());
        sgPolynomialOrderSpinner.setValue(s.sgPolynomialOrder());
        updateFilterSpecificControls();
    }

    public void applyParameterMap(Map<String, Object> paramMap) {
        setSettings(new SpectralNoiseSettings(
                paramMap.get("filterType") instanceof String s ? s : SpectralNoiseKernelFactory.FILTER_SG,
                paramMap.get("kernelSize") instanceof Number n ? n.intValue() : 11,
                paramMap.get("gaussianSigma") instanceof Number n ? n.doubleValue() : 1.0,
                paramMap.get("sgPolynomialOrder") instanceof Number n ? n.intValue() : 3
        ));
    }

    public void updateParameterMap(Map<String, Object> paramMap) {
        SpectralNoiseSettings s = getSettings();
        paramMap.put("filterType", s.filterType());
        paramMap.put("kernelSize", s.kernelSize());
        paramMap.put("gaussianSigma", s.gaussianSigma());
        paramMap.put("sgPolynomialOrder", s.sgPolynomialOrder());
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

        gaussianSigmaLabel.setEnabled(gaussian);
        gaussianSigmaSpinner.setEnabled(gaussian);
        sgPolynomialOrderLabel.setEnabled(sg);
        sgPolynomialOrderSpinner.setEnabled(sg);
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
