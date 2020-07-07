package org.esa.snap.ui.loading;

import javax.swing.*;
import java.awt.*;

public class CustomButton extends JButton {

    private final int preferredHeight;

    public CustomButton(String text, int preferredHeight) {
        super(text);

        if (preferredHeight <= 0) {
            throw new IllegalArgumentException("The preferred size " + preferredHeight + " must be > 0.");
        }

        this.preferredHeight = preferredHeight;
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
