package org.esa.snap.rcp.spectrallibrary.ui.resampling;

import org.esa.snap.speclib.util.resampling.SpectralResamplingSensor;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class SpectralResamplingSettingsPanel extends JPanel {
    private final JComboBox<String> targetSensorCombo = new JComboBox<>(new String[]{
            SpectralResamplingSensor.ENMAP.getName(),
            SpectralResamplingSensor.PRISMA.getName(),
            SpectralResamplingSensor.OLCI.getName()
    });


    public SpectralResamplingSettingsPanel() {
        super(new GridBagLayout());

        targetSensorCombo.addActionListener(e -> updateTargetSensorSpecificControls());

        GridBagConstraints gbc = createConstraints();
        addRow(this, gbc, 0, new JLabel("Target Sensor:"), targetSensorCombo);

        setSettings(SpectralResamplingSettings.defaults());
        updateTargetSensorSpecificControls();
    }


    public SpectralResamplingSettings getSettings() {
        return new SpectralResamplingSettings(
                (String) targetSensorCombo.getSelectedItem()
        );
    }

    public void setSettings(SpectralResamplingSettings settings) {
        SpectralResamplingSettings s = settings != null ? settings : SpectralResamplingSettings.defaults();
        targetSensorCombo.setSelectedItem(s.targetSensorName());
        updateTargetSensorSpecificControls();
    }

    public void applyParameterMap(Map<String, Object> paramMap) {
        setSettings(new SpectralResamplingSettings(
                paramMap.get("targetSensorName") instanceof String s ? s : SpectralResamplingSensor.ENMAP.getName()
        ));
    }

    public void updateParameterMap(Map<String, Object> paramMap) {
        SpectralResamplingSettings s = getSettings();
        paramMap.put("targetSensorName", s.targetSensorName());
    }

    public String validateParameters() {
        // nothing to do yet
//        try {
//            getSettings().validate();
//            return null;
//        } catch (IllegalArgumentException e) {
//            return e.getMessage();
//        }
        return null;
    }

    private void updateTargetSensorSpecificControls() {
        // nothing to do yet
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
