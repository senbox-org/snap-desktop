package org.esa.snap.ui.components;

import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import java.awt.*;

public class CustomTextField extends JTextField {

    private final int preferredHeight;

    public CustomTextField(int preferredHeight, Color backgroundColor) {
        super();

        this.preferredHeight = preferredHeight;

        setBackground(backgroundColor);
        setBorder(SwingUtils.LINE_BORDER);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();

        size.height = this.preferredHeight;
        return size;
    }
}
