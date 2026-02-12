package org.esa.snap.rcp.spectrallibrary;

import org.esa.snap.rcp.spectrallibrary.util.SpectralLibraryActionBinder;
import org.esa.snap.rcp.spectrallibrary.util.ViewModelBinder;
import org.esa.snap.rcp.spectrallibrary.controller.SpectralLibraryController;
import org.esa.snap.rcp.spectrallibrary.model.SpectralLibraryViewModel;
import org.esa.snap.rcp.spectrallibrary.ui.SpectralLibraryPanel;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;


@TopComponent.Description(
        preferredID = "SpectralLibraryTopComponent",
        iconBase = "org/esa/snap/rcp/icons/Spectrum.gif"
)
@TopComponent.Registration(mode = "Spectrum", openAtStartup = false, position = 85)
@ActionID(category = "Window", id = "org.esa.snap.rcp.spectrallibrary.SpectralLibraryTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Optical", position = 1),
        @ActionReference(path = "Menu/View/Tool Windows"),
        @ActionReference(path = "Toolbars/Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_SpectralLibraryTopComponent_Name",
        preferredID = "SpectralLibraryTopComponent"
)
@NbBundle.Messages({"CTL_SpectralLibraryTopComponent_Name=Spectral Library"})
public class SpectralLibraryTopComponent extends ToolTopComponent {


    private final SpectralLibraryViewModel vm = new SpectralLibraryViewModel();
    private final SpectralLibraryController controller = new SpectralLibraryController(vm);

    private final SpectralLibraryPanel panel = new SpectralLibraryPanel();

    private final ViewModelBinder vmBinder = new ViewModelBinder(vm, panel);
    private final SpectralLibraryActionBinder actionBinder = new SpectralLibraryActionBinder(vm, controller, panel, this::getSelectedProductSceneView);


    public SpectralLibraryTopComponent() {
        setDisplayName(Bundle.CTL_SpectralLibraryTopComponent_Name());
        setLayout(new BorderLayout(4, 4));
        add(panel, BorderLayout.CENTER);

        panel.getLibraryCombo().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof org.esa.snap.speclib.model.SpectralLibrary lib) {
                    setText(lib.getName());
                }
                return this;
            }
        });

        vmBinder.bind();
        actionBinder.bind();
    }


    @Override
    protected void componentOpened() {
        controller.init();
    }

}
