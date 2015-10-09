package org.esa.snap.ui.color;

import java.awt.Color;

/**
 * A factory for color chooser panels.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @since SNAP 2.0
 */
public interface ColorChooserPanelFactory {
    ColorChooserPanel create(Color selectedColor);
}
