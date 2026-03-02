package org.esa.snap.rcp.spectrallibrary.util;

import org.esa.snap.speclib.model.AttributeType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.EnumSet;

public class AttributDialogUtils {

    public static final EnumSet<AttributeType> UI_DENIED_TYPES = EnumSet.of(
            AttributeType.INSTANT,
            AttributeType.STRING_LIST,
            AttributeType.DOUBLE_ARRAY,
            AttributeType.INT_ARRAY,
            AttributeType.STRING_MAP,
            AttributeType.EMBEDDED_SPECTRUM
    );

    public static final AttributeType[] ALLOWED_TYPES = Arrays.stream(AttributeType.values())
            .filter(t -> !UI_DENIED_TYPES.contains(t))
            .toArray(AttributeType[]::new);

    public static String exampleFor(AttributeType t) {
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

    public static void installPlaceholder(JTextField field, String placeholder) {
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

    public static boolean isShowingPlaceholder(JTextField field) {
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
