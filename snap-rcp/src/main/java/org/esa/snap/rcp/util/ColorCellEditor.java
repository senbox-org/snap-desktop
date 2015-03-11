package org.esa.snap.rcp.util;

import javax.swing.*;

/**
 * Created by Norman on 11.03.2015.
 */
public class ColorCellEditor extends DefaultCellEditor {

    public ColorCellEditor() {
        super(new ColorComboBox());
    }

}
