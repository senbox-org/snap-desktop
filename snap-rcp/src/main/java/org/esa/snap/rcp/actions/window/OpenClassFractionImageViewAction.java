package org.esa.snap.rcp.actions.window;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import eu.esa.snap.core.renderer.classfraction.ClassFractionStatistics;
import eu.esa.snap.core.renderer.classfraction.ClassFractionStatisticsResult;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.util.PreferencesPropertyMap;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.classfraction.ClassFractionRendererController;
import org.esa.snap.ui.classfraction.ClassFractionRendererDialog;
import org.esa.snap.ui.classfraction.ClassFractionRendererModel;
import org.esa.snap.ui.classfraction.ClassFractionStatisticsController;
import org.esa.snap.ui.classfraction.ClassFractionStatisticsDialog;
import org.esa.snap.ui.product.ProductSceneImage;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import javax.swing.SwingWorker;
import java.awt.Cursor;
import java.awt.event.ActionEvent;


/**
 * Opens a display-only Class Fraction / Probability scene for the selected product.
 */
@ActionID(category = "View", id = "OpenClassFractionImageViewAction")
@ActionRegistration(displayName = "#CTL_OpenClassFractionImageViewAction_MenuText", lazy = true)
@ActionReferences({
        @ActionReference(path = "Menu/Window", position = 116),
        @ActionReference(path = "Context/Product/Product", position = 51)
})
@NbBundle.Messages({
        "CTL_OpenClassFractionImageViewAction_MenuText=Open Class Fraction / Probability View",
        "CTL_OpenClassFractionImageViewAction_ShortDescription=Configure and open a Class Fraction / Probability display"
})
public final class OpenClassFractionImageViewAction extends AbstractAction implements HelpCtx.Provider {


    private final Product product;


    public OpenClassFractionImageViewAction(ProductNode node) {
        super(Bundle.CTL_OpenClassFractionImageViewAction_MenuText());
        product = node.getProduct();
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OpenClassFractionImageViewAction_ShortDescription());
    }


    @Override
    public void actionPerformed(ActionEvent event) {
        if (product == null) {
            return;
        }
        ClassFractionRendererController controller = new ClassFractionRendererController(new ClassFractionRendererModel(product));
        ClassFractionRendererDialog dialog = new ClassFractionRendererDialog(SnapApp.getDefault().getMainFrame(), controller);

        int result = dialog.show();
        dialog.getJDialog().dispose();
        if (result == ClassFractionRendererDialog.ID_OK) {
            openProductSceneView(controller);
        } else if (result == ClassFractionRendererDialog.ID_SHOW_STATISTICS) {
            openStatistics(new ClassFractionStatisticsController(controller.getModel()));
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    private static void openProductSceneView(ClassFractionRendererController controller) {
        String name = controller.createConfiguration().getDisplayName();
        SwingWorker<ProductSceneImage, Object> worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(
                SnapApp.getDefault().getMainFrame(), SnapApp.getDefault().getInstanceName() + " - Creating image for '" + name + "'") {
            @Override
            protected ProductSceneImage doInBackground(ProgressMonitor progressMonitor) {
                return controller.createSceneImage(new PreferencesPropertyMap(SnapApp.getDefault().getPreferences()));
            }

            @Override
            protected void done() {
                SnapApp.getDefault().getMainFrame().setCursor(Cursor.getDefaultCursor());
                try {
                    OpenRGBImageViewAction.openDocumentWindow(new ProductSceneView(get()));
                    SnapApp.getDefault().setStatusBarMessage("");
                } catch (Exception e) {
                    SnapApp.getDefault().handleError("The Class Fraction / Probability image view could not be created.", e);
                }
            }
        };
        SnapApp.getDefault().setStatusBarMessage("Creating Class Fraction / Probability image view...");
        UIUtils.setRootFrameWaitCursor(SnapApp.getDefault().getMainFrame());
        worker.execute();
    }

    private static void openStatistics(ClassFractionStatisticsController controller) {
        SwingWorker<ClassFractionStatisticsResult, Object> worker = new ProgressMonitorSwingWorker<ClassFractionStatisticsResult, Object>(
                SnapApp.getDefault().getMainFrame(), "Computing Class Fraction / Probability Statistics") {
            @Override
            protected ClassFractionStatisticsResult doInBackground(ProgressMonitor progressMonitor) {
                return controller.compute(null, ClassFractionStatistics.DEFAULT_HISTOGRAM_BIN_COUNT, progressMonitor);
            }

            @Override
            protected void done() {
                try {
                    ClassFractionStatisticsDialog dialog = new ClassFractionStatisticsDialog(SnapApp.getDefault().getMainFrame(), controller, get());
                    dialog.setVisible(true);
                } catch (Exception e) {
                    SnapApp.getDefault().handleError("The Class Fraction / Probability statistics could not be calculated.", e);
                }
            }
        };
        worker.execute();
    }
}
