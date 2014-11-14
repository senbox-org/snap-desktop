package org.esa.snap.gui.action;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.snap.gui.view.BandImagePanel;
import org.esa.snap.gui.window.WorkspaceTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

import javax.swing.AbstractAction;
import javax.swing.JScrollPane;
import java.awt.event.ActionEvent;

@ActionID(
        category = "View",
        id = "org.snap.gui.action.OpenBandImageAction"
)
@ActionRegistration(
        displayName = "Open Band View"
)
@ActionReference(path = "Menu/View", position = 150, separatorAfter = 175)

/**
* @author Norman Fomferra
*/
public class OpenBandImageAction extends AbstractAction {

    RasterDataNode band;

    public OpenBandImageAction(RasterDataNode band) {
        this.band = band;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        /*
        Mode editor = WindowManager.getDefault().findMode("editor");
        BandImageTopComponent topComponent = new BandImageTopComponent(band);
        editor.dockInto(topComponent);
        topComponent.open();
        */
        WorkspaceTopComponent.getInstance().addComponent(band.getName(),
                                                         new JScrollPane(new BandImagePanel(band)));
    }
}
