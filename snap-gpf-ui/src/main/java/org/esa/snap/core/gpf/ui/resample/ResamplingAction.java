package org.esa.snap.core.gpf.ui.resample;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.Resampler;
import org.esa.snap.core.gpf.common.resample.ResamplingOp;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
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
@ActionReference(path = "Menu/Raster", position = 60)
@NbBundle.Messages({"CTL_ResamplingAction_Name=Resampling",
        "CTL_ResamplingAction_Description=Uses the SNAP resampling op to resample all bands of a product to the same size.",
        "CTL_ResamplingAction_OpName=Resampling Operator"})
@ServiceProvider(service = Resampler.class)
public class ResamplingAction extends AbstractSnapAction implements Resampler {

    @Override
    public void actionPerformed(ActionEvent e) {
        final Product selectedProduct = SnapApp.getDefault().getSelectedProduct(SnapApp.SelectionSourceHint.AUTO);
        resample(selectedProduct);
        SnapApp.getDefault().getProductManager().addListener(new PMListener());
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
    public void resample(Product multiSizeProduct) {
        final DefaultSingleTargetProductDialog resamplingDialog =
                new DefaultSingleTargetProductDialog("Resample", SnapApp.getDefault().getAppContext(), "Resampling", "resampleAction");
        resamplingDialog.setTargetProductNameSuffix("_resampled");
        resamplingDialog.getTargetProductSelector().getModel().setSaveToFileSelected(false);
        final Property referenceNodeNameProperty = resamplingDialog.getBindingContext().getPropertySet().getProperty("referenceBandName");
        final String[] bandNames = multiSizeProduct.getBandNames();
        if (bandNames.length > 0) {
            referenceNodeNameProperty.getDescriptor().setAttribute("valueSet", new ValueSet(bandNames));
            referenceNodeNameProperty.getDescriptor().setAttribute("defaultValue", bandNames[0]);
            referenceNodeNameProperty.getDescriptor().setDefaultValue(bandNames[0]);
            try {
                referenceNodeNameProperty.setValue(bandNames[0]);
            } catch (ValidationException e) {
                //don't set it then
            }
        }
        resamplingDialog.show();
    }

    private class PMListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            updateEnableState();
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            updateEnableState();
        }

        private void updateEnableState() {
            setEnabled(SnapApp.getDefault().getSelectedProduct(SnapApp.SelectionSourceHint.AUTO).isMultiSizeProduct());
        }

    }
}
