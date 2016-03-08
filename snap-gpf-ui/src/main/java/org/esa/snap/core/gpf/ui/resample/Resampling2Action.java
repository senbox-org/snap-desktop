package org.esa.snap.core.gpf.ui.resample;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.Resampler;
import org.esa.snap.core.gpf.common.resample.ResamplingOp2;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

import java.awt.event.ActionEvent;

/**
 * Action to access the Resampling2 Op.
 *
 * @author Tonio Fincke
 */
@ActionID(category = "Operators", id = "org.esa.snap.core.gpf.ui.resample.ResamplingAction")
@ActionRegistration(displayName = "#CTL_Resampling2Action_Name")
@ActionReferences({@ActionReference(path = "Menu/Raster", position = 70)})
@NbBundle.Messages({"CTL_Resampling2Action_Name=Resampling2",
        "CTL_Resampling2Action_Description=Uses the SNAP resampling op to resample all bands of a product to the same size.",
        "CTL_Resampling2Action_OpName=Resampling Operator 2",
        "CTL_Resampling2Action_Help=resampleAction"})
@ServiceProvider(service = Resampler.class)
public class Resampling2Action extends AbstractSnapAction implements Resampler {

    @Override
    public void actionPerformed(ActionEvent e) {
        final Product selectedProduct = SnapApp.getDefault().getSelectedProduct(SnapApp.SelectionSourceHint.AUTO);
        resample(selectedProduct);
    }

    @Override
    public String getName() {
        return Bundle.CTL_Resampling2Action_OpName();
    }

    @Override
    public String getDescription() {
        return Bundle.CTL_Resampling2Action_Description();
    }

    @Override
    public boolean canResample(Product multiSizeProduct) {
        return ResamplingOp2.canBeApplied(multiSizeProduct);
    }

    @Override
    public void resample(Product multiSizeProduct) {
        final DefaultSingleTargetProductDialog resamplingDialog =
                new DefaultSingleTargetProductDialog("Resample2", SnapApp.getDefault().getAppContext(), "Resampling2", "resampleAction");
        resamplingDialog.setTargetProductNameSuffix("_resampled");
        resamplingDialog.getTargetProductSelector().getModel().setSaveToFileSelected(false);
        resamplingDialog.show();
    }
}

