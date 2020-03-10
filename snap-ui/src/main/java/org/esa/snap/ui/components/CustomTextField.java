package org.esa.snap.ui.components;

import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.*;
import java.awt.*;

public class CustomTextField extends JTextField {

    private final int preferredHeight;

    public CustomTextField(int preferredHeight, Color backgroundColor) {
        super();

        if (preferredHeight <= 0) {
            throw new IllegalArgumentException("The preferred size " + preferredHeight + " must be > 0.");
        }
        if (backgroundColor == null) {
            throw new NullPointerException("The background color is null.");
        }

        this.preferredHeight = preferredHeight;

        setBackground(backgroundColor);
        setBorder(SwingUtils.LINE_BORDER);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, this.preferredHeight);
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension size = super.getMinimumSize();

        size.height = this.preferredHeight;
        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension size = super.getMaximumSize();

        size.height = this.preferredHeight;
        return size;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();

        size.height = this.preferredHeight;
        return size;
    }
}
