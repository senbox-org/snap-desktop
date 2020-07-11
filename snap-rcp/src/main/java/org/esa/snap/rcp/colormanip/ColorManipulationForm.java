package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Stx;

import javax.swing.JPanel;
import java.awt.event.ActionListener;
import java.nio.file.Path;

/**
 * @author Tonio Fincke
 */
public interface ColorManipulationForm {

    ColorFormModel getFormModel();

    void installToolButtons(boolean installAllButtons);

    void installMoreOptions();

    void revalidateToolViewPaneControl();

    Stx getStx(RasterDataNode raster);

    void applyChanges();

    Path getIODir();

    JPanel getContentPanel();

    ActionListener wrapWithAutoApplyActionListener(final ActionListener actionListener);

}
