package org.esa.snap.graphbuilder.gpf.ui;

import javax.swing.*;
import java.awt.*;

public class StatusLabel extends JLabel {

    public StatusLabel() {
        super();
        setText("");
        setForeground(Color.RED);
    }

    public void setOkMessage(String message) {
        setForeground(Color.GREEN);
        setText(message);
    }

    public void setErrorMessage(String message) {
        setForeground(Color.RED);
        setText(message);
    }

    public void setWarningMessage(String message) {
        setForeground(Color.ORANGE);
        setText(message);
    }
}
