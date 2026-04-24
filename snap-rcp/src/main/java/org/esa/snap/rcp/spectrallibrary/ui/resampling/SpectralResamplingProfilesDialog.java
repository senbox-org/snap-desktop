package org.esa.snap.rcp.spectrallibrary.ui.resampling;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;


public class SpectralResamplingProfilesDialog extends JDialog {


    public enum SaveMode {
        ACTIVE_LIBRARY,
        NEW_LIBRARY
    }

    public record Result(
            SpectralResamplingSettings settings,
            SaveMode saveMode,
            String nameSuffix,
            String newLibraryName
    ) {}

    private final SpectralResamplingSettingsPanel settingsPanel = new SpectralResamplingSettingsPanel();

    private final JRadioButton saveToNewLibraryButton = new JRadioButton("Save in new library", true);

    private final JTextField suffixField = new JTextField("_resampled", 18);
    private final JTextField newLibraryNameField = new JTextField(24);

    private final JLabel statusLabel = new JLabel(" ");

    private Result result;


    private SpectralResamplingProfilesDialog(Window owner,
                                             String activeLibraryName,
                                             int selectedProfileCount) {
        super(owner, "Spectral Resampling", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setLayout(new BorderLayout(8, 8));
        add(buildContent(activeLibraryName, selectedProfileCount), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        prefillNewLibraryName(activeLibraryName);
        installListeners();
        updateTargetControls();

        pack();
        setMinimumSize(new Dimension(560, getPreferredSize().height));
        setLocationRelativeTo(owner);
    }

    public static Optional<Result> showDialog(Component parent,
                                              String activeLibraryName,
                                              int selectedProfileCount) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        SpectralResamplingProfilesDialog dialog =
                new SpectralResamplingProfilesDialog(owner, activeLibraryName, selectedProfileCount);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.result);
    }

    private JComponent buildContent(String activeLibraryName, int selectedProfileCount) {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Selection"));

        GridBagConstraints gbc = createConstraints();
        addRow(infoPanel, gbc, 0, new JLabel("Selected profiles:"), new JLabel(String.valueOf(selectedProfileCount)));
        addRow(infoPanel, gbc, 1, new JLabel("Active library:"), new JLabel(
                activeLibraryName != null && !activeLibraryName.isBlank() ? activeLibraryName : "<none>"
        ));

        JPanel targetPanel = new JPanel(new GridBagLayout());
        targetPanel.setBorder(BorderFactory.createTitledBorder("Output"));

        ButtonGroup group = new ButtonGroup();
        group.add(saveToNewLibraryButton);

        gbc = createConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        targetPanel.add(saveToNewLibraryButton, gbc);

        gbc.gridwidth = 1;
        addRow(targetPanel, gbc, 2, new JLabel("Suffix for smoothed profiles:"), suffixField);
        addRow(targetPanel, gbc, 3, new JLabel("New library name:"), newLibraryNameField);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(infoPanel);
        top.add(Box.createVerticalStrut(8));
        top.add(targetPanel);

        root.add(top, BorderLayout.NORTH);
        root.add(settingsPanel, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);

        return root;
    }

    private JComponent buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> onOk());
        cancelButton.addActionListener(e -> onCancel());

        buttons.add(okButton);
        buttons.add(cancelButton);
        getRootPane().setDefaultButton(okButton);

        return buttons;
    }

    private void installListeners() {
        saveToNewLibraryButton.addActionListener(e -> updateTargetControls());
    }

    private void prefillNewLibraryName(String activeLibraryName) {
        String baseName = (activeLibraryName == null || activeLibraryName.isBlank())
                ? "Library"
                : activeLibraryName.trim();
        newLibraryNameField.setText(baseName + "_resampled");
    }

    private void updateTargetControls() {
        boolean saveToNewLibrary = saveToNewLibraryButton.isSelected();
        newLibraryNameField.setEnabled(saveToNewLibrary);
    }

    private void onOk() {
        String validationError = settingsPanel.validateParameters();
        if (validationError != null) {
            setStatus(validationError);
            return;
        }

        String suffix = suffixField.getText() != null ? suffixField.getText().trim() : "";
        if (suffix.isEmpty()) {
            setStatus("Please provide a suffix for the smoothed profiles.");
            return;
        }

        SaveMode saveMode = saveToNewLibraryButton.isSelected()
                ? SaveMode.NEW_LIBRARY
                : SaveMode.ACTIVE_LIBRARY;

        String newLibraryName = null;
        if (saveMode == SaveMode.NEW_LIBRARY) {
            newLibraryName = newLibraryNameField.getText() != null ? newLibraryNameField.getText().trim() : "";
            if (newLibraryName.isEmpty()) {
                setStatus("Please provide a name for the new library.");
                return;
            }
        }

        result = new Result(
                settingsPanel.getSettings(),
                saveMode,
                suffix,
                newLibraryName
        );
        dispose();
    }

    private void onCancel() {
        result = null;
        dispose();
    }

    private void setStatus(String text) {
        statusLabel.setText(text == null || text.isBlank() ? " " : text);
    }

    private static GridBagConstraints createConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        return gbc;
    }

    private static void addRow(JPanel panel,
                               GridBagConstraints gbc,
                               int row,
                               JComponent label,
                               JComponent component) {
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
