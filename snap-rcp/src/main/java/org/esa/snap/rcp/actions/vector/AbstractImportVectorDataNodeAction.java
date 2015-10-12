package org.esa.snap.rcp.actions.vector;

import org.esa.snap.core.dataio.geometry.VectorDataNodeReader;
import org.esa.snap.core.datamodel.GeometryDescriptor;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.PlacemarkDescriptorRegistry;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.ModalDialog;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * @author olafd
 * @author Thomas Storm
 */
abstract class AbstractImportVectorDataNodeAction extends AbstractSnapAction {

    protected FeatureUtils.FeatureCrsProvider crsProvider;
    protected VectorDataNodeReader.PlacemarkDescriptorProvider placemarkDescriptorProvider;

    private int featureCrsDialogResult;

    protected AbstractImportVectorDataNodeAction() {
        crsProvider = new MyFeatureCrsProvider();
        placemarkDescriptorProvider = new MyPlacemarkDescriptorProvider();
    }

    private class MyPlacemarkDescriptorProvider implements VectorDataNodeReader.PlacemarkDescriptorProvider {

        @Override
        public PlacemarkDescriptor getPlacemarkDescriptor(SimpleFeatureType simpleFeatureType) {
            PlacemarkDescriptorRegistry placemarkDescriptorRegistry = PlacemarkDescriptorRegistry.getInstance();
            if (simpleFeatureType.getUserData().containsKey(PlacemarkDescriptorRegistry.PROPERTY_NAME_PLACEMARK_DESCRIPTOR)) {
                String placemarkDescriptorClass = simpleFeatureType.getUserData().get(PlacemarkDescriptorRegistry.PROPERTY_NAME_PLACEMARK_DESCRIPTOR).toString();
                PlacemarkDescriptor placemarkDescriptor = placemarkDescriptorRegistry.getPlacemarkDescriptor(placemarkDescriptorClass);
                if (placemarkDescriptor != null) {
                    return placemarkDescriptor;
                }
            }

            List<PlacemarkDescriptor> validPlacemarkDescriptors = placemarkDescriptorRegistry.getPlacemarkDescriptors(simpleFeatureType);
            if (validPlacemarkDescriptors.size() == 1) {
                return validPlacemarkDescriptors.get(0);
            }

            if (featureCrsDialogResult == ModalDialog.ID_OK) {
                TypeDialog typeDialog = new TypeDialog(SnapApp.getDefault().getMainFrame(), simpleFeatureType);
                final int dialogResult = typeDialog.show();
                if (dialogResult == ModalDialog.ID_OK) {
                    return typeDialog.getPlacemarkDescriptor();
                } else if (dialogResult == ModalDialog.ID_CANCEL) {
                    typeDialog.close();
                    return null;
                }
            } else {
                return null;
            }

            return PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(GeometryDescriptor.class);
        }
    }

    private class MyFeatureCrsProvider implements FeatureUtils.FeatureCrsProvider {

        @Override
        public CoordinateReferenceSystem getFeatureCrs(final Product product) {
            if (product.getModelCRS() == Product.DEFAULT_IMAGE_CRS) {
                return Product.DEFAULT_IMAGE_CRS;
            }

            final CoordinateReferenceSystem[] featureCrsBuffer = new CoordinateReferenceSystem[1];
            Runnable runnable = () -> featureCrsBuffer[0] = promptForFeatureCrs(product);
            if (!SwingUtilities.isEventDispatchThread()) {
                try {
                    SwingUtilities.invokeAndWait(runnable);
                } catch (InterruptedException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            } else {
                runnable.run();
            }
            CoordinateReferenceSystem featureCrs = featureCrsBuffer[0];
            return featureCrs != null ? featureCrs : DefaultGeographicCRS.WGS84;
        }

        @Override
        public boolean clipToProductBounds() {
            return true;
        }

        private CoordinateReferenceSystem promptForFeatureCrs(Product product) {
            final FeatureCrsDialog dialog = new FeatureCrsDialog(product, "Import " + getVectorDataType() + " Data");

            featureCrsDialogResult = dialog.show();
            if (featureCrsDialogResult == ModalDialog.ID_OK) {
                return dialog.getFeatureCrs();
            }

            return DefaultGeographicCRS.WGS84;
        }

    }

    protected abstract String getDialogTitle();

    protected abstract String getVectorDataType();

}
