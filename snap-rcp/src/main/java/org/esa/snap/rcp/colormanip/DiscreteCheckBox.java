package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.util.NamingConvention;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class DiscreteCheckBox extends JCheckBox {

    private boolean shouldFireDiscreteEvent;

    DiscreteCheckBox(final ColorManipulationForm parentForm) {
        super("Discrete " + NamingConvention.COLOR_LOWER_CASE + "s");

        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (shouldFireDiscreteEvent) {
                    parentForm.getFormModel().getModifiedImageInfo().getColorPaletteDef().setDiscrete(isSelected());
                    parentForm.applyChanges();
                }
            }
        });
    }

    void setDiscreteColorsMode(boolean discrete) {
        shouldFireDiscreteEvent = false;
        setSelected(discrete);
        shouldFireDiscreteEvent = true;
    }
}
