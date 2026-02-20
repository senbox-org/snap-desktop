package org.esa.snap.rcp.spectrallibrary.ui;

import org.esa.snap.rcp.spectrallibrary.util.AttributeDialogResult;
import org.esa.snap.speclib.model.AttributeType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;


public class AddAttributeDialog {


    private static final EnumSet<AttributeType> UI_DENIED_TYPES = EnumSet.of(
            AttributeType.INSTANT,
            AttributeType.STRING_LIST,
            AttributeType.DOUBLE_ARRAY,
            AttributeType.INT_ARRAY,
            AttributeType.STRING_MAP,
            AttributeType.EMBEDDED_SPECTRUM
    );

    public static Optional<AttributeDialogResult> show(Component parent) {
        JTextField keyField = new JTextField(20);

        AttributeType[] allowed = Arrays.stream(AttributeType.values())
                .filter(t -> !UI_DENIED_TYPES.contains(t))
                .toArray(AttributeType[]::new);
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

            String ex = exampleFor(t);
            defaultHint.setText(ex.isBlank() ? " " : ("Example: " + ex));

            defaultField.setEnabled(true);
            installPlaceholder(defaultField, ex);

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
        if (isShowingPlaceholder(defaultField)) {
            rawDefault = (String) defaultField.getClientProperty("placeholderText");
        }

        return Optional.of(new AttributeDialogResult(
                key,
                type,
                rawDefault
        ));
    }

    private static String exampleFor(AttributeType t) {
        if (t == null) {
            return "";
        }
        return switch (t) {
            case STRING -> "any_string";
            case INT -> "42";
            case LONG -> "12345678900";
            case DOUBLE -> "0.123";
            case BOOLEAN -> "true";
            case STRING_LIST -> "a,b,c";
            case DOUBLE_ARRAY -> "0.1,0.2,0.3";
            case INT_ARRAY -> "1,2,3";
            case STRING_MAP -> "k1=v1,k2=v2";
            default -> "";
        };
    }

    private static void installPlaceholder(JTextField field, String placeholder) {
        field.putClientProperty("placeholderText", placeholder == null ? "" : placeholder);

        if (field.getText().trim().isEmpty() && !placeholder.isBlank()) {
            field.setForeground(Color.GRAY);
            field.setText(placeholder);
        }

        for (var l : field.getFocusListeners()) {
            if (l.getClass().getName().equals(PlaceholderFocusListener.class.getName())) {
                return;
            }
        }
        field.addFocusListener(new PlaceholderFocusListener());
    }

    private static boolean isShowingPlaceholder(JTextField field) {
        String ph = (String) field.getClientProperty("placeholderText");
        return ph != null
                && !ph.isBlank()
                && Color.GRAY.equals(field.getForeground())
                && ph.equals(field.getText());
    }



    private static final class PlaceholderFocusListener extends FocusAdapter {
        @Override
        public void focusGained(FocusEvent e) {
            JTextField field = (JTextField) e.getComponent();
            if (isShowingPlaceholder(field)) {
                field.setText("");
                field.setForeground(UIManager.getColor("TextField.foreground"));
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            JTextField field = (JTextField) e.getComponent();
            String ph = (String) field.getClientProperty("placeholderText");
            if (ph == null || ph.isBlank()) {
                return;
            }

            if (field.getText().trim().isEmpty()) {
                field.setForeground(Color.GRAY);
                field.setText(ph);
            }
        }
    }
}
