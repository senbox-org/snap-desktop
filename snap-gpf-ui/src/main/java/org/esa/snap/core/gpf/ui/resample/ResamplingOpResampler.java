package org.esa.snap.core.gpf.ui.resample;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.Resampler;
import org.esa.snap.core.gpf.common.resample.ResamplingOp;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.SnapApp;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author Tonio Fincke
 */
@ServiceProvider(service = Resampler.class)
public class ResamplingOpResampler implements Resampler {

    @Override
    public String getName() {
        return "Resampling Operator";
    }

    @Override
    public String getDescription() {
        return "Uses the SNAP resampling op to resample all bands of a product to the same size";
    }

    @Override
    public boolean canResample(Product multiSizeProduct) {
        return ResamplingOp.canBeApplied(multiSizeProduct);
    }

    @Override
    public void resample(Product multiSizeProduct) {
        //todo create dedicated resampling op ui - tf 20160224
        final DefaultSingleTargetProductDialog resamplingDialog =
                new DefaultSingleTargetProductDialog("Resample", SnapApp.getDefault().getAppContext(), "Resampling", "resampleAction");
        resamplingDialog.setTargetProductNameSuffix("_resampled");
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

    public static void create() {
        new ResamplingOpResampler();
    }
}
