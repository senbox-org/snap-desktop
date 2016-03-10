package org.esa.snap.core.gpf.ui.resample;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.Resampler;
import org.esa.snap.core.gpf.common.resample.ResamplingOp;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

import java.awt.event.ActionEvent;

/**
 * Action to access the Resampling Op.
 *
 * @author Tonio Fincke
 */
@ActionID(category = "Operators", id = "org.esa.snap.core.gpf.ui.resample.ResamplingAction")
@ActionRegistration(displayName = "#CTL_ResamplingAction_Name")
@ActionReferences({@ActionReference(path = "Menu/Raster", position = 60)})
@NbBundle.Messages({"CTL_ResamplingAction_Name=Resampling",
        "CTL_ResamplingAction_Description=Uses the SNAP resampling op to resample all bands of a product to the same size.",
        "CTL_ResamplingAction_OpName=Resampling Operator",
        "CTL_ResamplingAction_Help=resampleAction"})
@ServiceProvider(service = Resampler.class)
public class ResamplingAction extends AbstractSnapAction implements Resampler {

    @Override
    public void actionPerformed(ActionEvent e) {
        final Product product = SnapApp.getDefault().getSelectedProduct(SnapApp.SelectionSourceHint.AUTO);
        resample(product, false);
    }

    @Override
    public String getName() {
        return Bundle.CTL_ResamplingAction_OpName();
    }

    @Override
    public String getDescription() {
        return Bundle.CTL_ResamplingAction_Description();
    }

    @Override
    public boolean canResample(Product multiSizeProduct) {
        return ResamplingOp.canBeApplied(multiSizeProduct);
    }

    @Override
    public Product resample(Product multiSizeProduct) {
        return resample(multiSizeProduct, true);
    }

    public Product resample(Product product, boolean modal) {
        final AppContext appContext = SnapApp.getDefault().getAppContext();
        final ResamplingDialog resamplingDialog = new ResamplingDialog(appContext, product, modal);
        resamplingDialog.show();
        return resamplingDialog.getTargetProduct();
    }
}

