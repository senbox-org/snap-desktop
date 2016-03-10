package org.esa.snap.core.gpf.ui.resample;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.Resampler;
import org.esa.snap.core.gpf.common.resample.ResamplingOp;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
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
 * Action to access the Resampling2 Op.
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
        resample(false);
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
        return resample(true);
    }

    public Product resample(boolean modal) {
        final AppContext appContext = SnapApp.getDefault().getAppContext();
        final ResamplingDialog resamplingDialog = new ResamplingDialog(appContext, modal);
        resamplingDialog.show();
        return resamplingDialog.getTargetProduct();
    }

    private class ResamplingDialog extends DefaultSingleTargetProductDialog {

        Product targetProduct;

        public ResamplingDialog(AppContext appContext, boolean modal) {
            super("Resample", appContext, "Resampling", "resampleAction");
            setTargetProductNameSuffix("_resampled");
            getTargetProductSelector().getModel().setSaveToFileSelected(false);
            getJDialog().setModal(modal);
        }

        @Override
        protected Product createTargetProduct() throws Exception {
            targetProduct = super.createTargetProduct();
            return targetProduct;
        }

        @Override
        protected void onApply() {
            super.onApply();
            if (getJDialog().isModal()) {
                close();
            }
        }

        Product getTargetProduct() {
            return targetProduct;
        }
    }
}

