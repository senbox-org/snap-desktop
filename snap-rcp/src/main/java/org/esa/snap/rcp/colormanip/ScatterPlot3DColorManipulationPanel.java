package org.esa.snap.rcp.colormanip;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.GridBagUtils;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Tonio Fincke
 */
public class ScatterPlot3DColorManipulationPanel implements ColorManipulationForm {

    private final static String PREFERENCES_KEY_IO_DIR = "snap.color_palettes.dir";

    private final ScatterPlot3DFormModel formModel;
    private final JPanel container;
    private boolean defaultColorPalettesInstalled;
    private ColorManipulationChildForm childForm;
    private ColorManipulationChildForm continuous1BandSwitcherForm;
    private ColorManipulationChildForm discrete1BandTabularForm;
    private JPanel editorPanel;
    private ColorManipulationChildForm emptyForm;
    private Path ioDir;

    public ScatterPlot3DColorManipulationPanel(JPanel container, ScatterPlot3DFormModel formModel) {
        this.container = container;
        this.formModel = formModel;
//        formModel = new ScatterPlot3DFormModel(null);
        emptyForm = new EmptyImageInfoForm(this);
        initContentPanel();
    }

    public void setRasterDataNode(RasterDataNode raster) {
        formModel.setRaster(raster);
        installChildForm();
    }

    @Override
    public FormModel getFormModel() {
        return formModel;
    }

    @Override
    public void installToolButtons() {
        //do nothing
    }

    @Override
    public void installMoreOptions() {
        //do nothing
    }

    @Override
    public void revalidateToolViewPaneControl() {
        container.repaint();
    }

    @Override
    public Stx getStx(RasterDataNode raster) {
        return raster.getStx(false, ProgressMonitor.NULL);
    }

    private void initContentPanel() {
        if (editorPanel == null) {
            editorPanel = new JPanel(new BorderLayout(4, 4));
            installChildForm();
        }
        if (!defaultColorPalettesInstalled) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(new InstallDefaultColorPalettes());
        }
    }

    @Override
    public JPanel getContentPanel() {
        initContentPanel();
        return editorPanel;
    }

    @Override
    public ActionListener wrapWithAutoApplyActionListener(ActionListener actionListener) {
        return null;
    }

    private void installChildForm() {
        final ColorManipulationChildForm oldForm = childForm;
        ColorManipulationChildForm newForm = emptyForm;
        if (getFormModel().isValid()) {
            if (getFormModel().isContinuous1BandImage()) {
                if (oldForm instanceof Continuous1BandSwitcherForm) {
                    newForm = oldForm;
                } else {
                    newForm = getContinuous1BandSwitcherForm();
                }
            } else if (getFormModel().isDiscrete1BandImage()) {
                if (oldForm instanceof Discrete1BandTabularForm) {
                    newForm = oldForm;
                } else {
                    newForm = getDiscrete1BandTabularForm();
                }
            } else {
                if (oldForm instanceof Continuous1BandSwitcherForm) {
                    newForm = oldForm;
                } else {
                    newForm = getContinuous1BandSwitcherForm();
                }
            }
        }
        if (newForm != oldForm) {
            childForm = newForm;
            editorPanel.removeAll();
            editorPanel.add(childForm.getContentPanel(), BorderLayout.CENTER);
            if (oldForm != null) {
                oldForm.handleFormHidden(getFormModel());
            }
            childForm.handleFormShown(getFormModel());
        } else {
            childForm.updateFormModel(getFormModel());
        }
    }

    private ColorManipulationChildForm getContinuous1BandSwitcherForm() {
        if (continuous1BandSwitcherForm == null) {
            continuous1BandSwitcherForm = new Continuous1BandSwitcherForm(this);
        }
        return continuous1BandSwitcherForm;
    }

    private ColorManipulationChildForm getDiscrete1BandTabularForm() {
        if (discrete1BandTabularForm == null) {
            discrete1BandTabularForm = new Discrete1BandTabularForm(this);
        }
        return discrete1BandTabularForm;
    }

    @Override
    public void applyChanges() {
        if (getFormModel().isValid()) {
            getFormModel().applyModifiedImageInfo();
        }
    }

    @Override
    public Path getIODir() {
        if (ioDir == null) {
            ioDir = Paths.get(Config.instance().preferences().get(PREFERENCES_KEY_IO_DIR, getColorPalettesDir().toString()));
        }
        return ioDir;
    }

    private class InstallDefaultColorPalettes implements Runnable {

        private InstallDefaultColorPalettes() {
        }

        @Override
        public void run() {
            try {
                Path sourceBasePath = ResourceInstaller.findModuleCodeBasePath(GridBagUtils.class);
                Path auxdataDir = getColorPalettesDir();
                Path sourceDirPath = sourceBasePath.resolve("auxdata/color_palettes");
                final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDir);

                resourceInstaller.install(".*.cpd", ProgressMonitor.NULL);
                defaultColorPalettesInstalled = true;
            } catch (IOException e) {
                SnapApp.getDefault().handleError("Unable to install colour palettes", e);
            }
        }
    }

    private Path getColorPalettesDir() {
        return SystemUtils.getAuxDataPath().resolve("color_palettes");
    }

}
