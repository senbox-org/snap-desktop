package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.ui.AbstractDialog;

import javax.swing.AbstractButton;
import java.awt.Component;

class LogDisplay {

    static AbstractButton createButton() {
        final AbstractButton logDisplayButton = ImageInfoEditorSupport.createToggleButton("org/esa/snap/rcp/icons/LogDisplay24.png");
        logDisplayButton.setName("logDisplayButton");
        logDisplayButton.setToolTipText("Switch to logarithmic display"); /*I18N*/
        return logDisplayButton;
    }

    static void showNotApplicableInfo(Component parent) {
        AbstractDialog.showInformationDialog(parent, "Log display is not applicable!\nThe color palette must contain only positive slider values.", "Information");
    }

    static boolean checkApplicability(ColorPaletteDef cpd) {
        final ColorPaletteDef.Point[] points = cpd.getPoints();
        for (ColorPaletteDef.Point point : points) {
            if (point.getSample() <= 0.0) {
                return false;
            }
        }
        return true;
    }
}
