package org.esa.snap.rcp.spectrallibrary.ui;

import org.esa.snap.rcp.spectrallibrary.util.AttributDialogUtils;
import org.esa.snap.rcp.spectrallibrary.util.AttributeDialogResult;
import org.esa.snap.speclib.model.AttributeType;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;


public class AddAttributeDialog {


    public static Optional<AttributeDialogResult> show(Component parent) {
        JTextField keyField = new JTextField(20);

        AttributeType[] allowed = AttributDialogUtils.ALLOWED_TYPES;
        JComboBox<AttributeType> typeCombo = new JComboBox<>(allowed);
        typeCombo.setSelectedItem(AttributeType.STRING);

        JTextField defaultField = new JTextField(20);
        JLabel defaultHint = new JLabel(" ");
        defaultHint.setForeground(Color.GRAY);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        int r = 0;
        gc.gridx = 0; gc.gridy = r; gc.weightx = 0;
        form.add(new JLabel("Key:"), gc);
        gc.gridx = 1; gc.gridy = r; gc.weightx = 1;
        form.add(keyField, gc);

        r++;
        gc.gridx = 0; gc.gridy = r; gc.weightx = 0;
        form.add(new JLabel("Type:"), gc);
        gc.gridx = 1; gc.gridy = r; gc.weightx = 1;
        form.add(typeCombo, gc);

        r++;
        gc.gridx = 0; gc.gridy = r; gc.weightx = 0;
        form.add(new JLabel("Default value:"), gc);
        gc.gridx = 1; gc.gridy = r; gc.weightx = 1;
        form.add(defaultField, gc);

        r++;
        gc.gridx = 1; gc.gridy = r; gc.weightx = 1;
        form.add(defaultHint, gc);

        Runnable updateHint = () -> {
            AttributeType t = (AttributeType) typeCombo.getSelectedItem();
            if (t == null) {
                t = AttributeType.STRING;
            }

            String ex = AttributDialogUtils.exampleFor(t);
            defaultHint.setText(ex.isBlank() ? " " : ("Example: " + ex));

            defaultField.setEnabled(true);
            AttributDialogUtils.installPlaceholder(defaultField, ex);

            defaultField.setForeground(Color.GRAY);
            defaultField.setText(ex);
        };
        typeCombo.addActionListener(e -> updateHint.run());
        updateHint.run();

        int rc = JOptionPane.showConfirmDialog(
                parent,
                form,
                "Add Attribute to Library",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (rc != JOptionPane.OK_OPTION) {
            return Optional.empty();
        }

        String key = keyField.getText();
        AttributeType type = (AttributeType) typeCombo.getSelectedItem();
        if (type == null) {
            type = AttributeType.STRING;
        }

        String rawDefault = defaultField.getText();
        if (AttributDialogUtils.isShowingPlaceholder(defaultField)) {
            rawDefault = (String) defaultField.getClientProperty("placeholderText");
        }

        return Optional.of(new AttributeDialogResult(
                key,
                type,
                rawDefault
        ));
    }
}
