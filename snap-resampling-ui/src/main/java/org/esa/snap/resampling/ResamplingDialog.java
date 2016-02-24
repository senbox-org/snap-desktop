package org.esa.snap.resampling;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.common.resample.ResamplingOp;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.core.datamodel.Resampler;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author Tonio Fincke
 */
@ServiceProvider(service = Resampler.class)
public class ResamplingDialog extends DefaultSingleTargetProductDialog implements Resampler {

    public ResamplingDialog() {
        super("Resample", SnapApp.getDefault().getAppContext(), "", "");
        setTargetProductNameSuffix("_resampled");
    }

    @Override
    public String getName() {
        return "ResamplingOp";
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
        final ResamplingDialog resamplingDialog = new ResamplingDialog();
        resamplingDialog.show();
    }

    public static void create() {
        new ResamplingDialog();
    }
}
