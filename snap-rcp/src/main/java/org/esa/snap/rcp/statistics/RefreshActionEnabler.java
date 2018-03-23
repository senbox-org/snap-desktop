package org.esa.snap.rcp.statistics;

import com.bc.ceres.binding.PropertyContainer;

import javax.swing.AbstractButton;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created with IntelliJ IDEA.
 * User: tonio
 * Date: 23.04.12
 * Time: 11:07
 * To change this template use File | Settings | File Templates.
 */
class RefreshActionEnabler implements PropertyChangeListener {

    private final static String PROPERTY_NAME_AUTO_MIN_MAX = "autoMinMax";
    private final static String PROPERTY_NAME_MIN = "min";
    private final static String PROPERTY_NAME_MAX = "max";
    private static final String PROPERTY_NAME_NUM_BINS = "numBins";
    private final static String PROPERTY_NAME_USE_ROI_MASK = "useRoiMask";
    private final static String PROPERTY_NAME_ROI_MASK = "roiMask";
    private final static String PROPERTY_NAME_X_PRODUCT = "xProduct";
    private final static String PROPERTY_NAME_Y_PRODUCT = "yProduct";
    private final static String PROPERTY_NAME_Z_PRODUCT = "zProduct";
    private final static String PROPERTY_NAME_X_BAND = "xBand";
    private final static String PROPERTY_NAME_Y_BAND = "yBand";
    private final static String PROPERTY_NAME_Z_BAND = "zBand";

    private HashSet<String> names = new HashSet<>();
    private AbstractButton refreshButton;

    RefreshActionEnabler(AbstractButton rb, String... componentNames) {
        names.addAll(Arrays.asList(componentNames));
        refreshButton = rb;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (names.contains(evt.getPropertyName())) {
            final PropertyContainer container = (PropertyContainer) evt.getSource();
            boolean enableRefreshButton = true;
            if (evt.getPropertyName().equals(PROPERTY_NAME_USE_ROI_MASK) && evt.getNewValue().equals(true) &&
                    container.getProperty(PROPERTY_NAME_ROI_MASK).getValue() == null) {
                return;
            } else if (evt.getPropertyName().equals(PROPERTY_NAME_NUM_BINS) && evt.getOldValue().equals(evt.getNewValue())) {
                return;
            } else if (evt.getPropertyName().equals(PROPERTY_NAME_AUTO_MIN_MAX) && evt.getNewValue().equals(false)) {
                return;
            } else if (evt.getPropertyName().equals(PROPERTY_NAME_MIN) && (evt.getOldValue().equals(evt.getNewValue()) ||
                    container.getProperty(PROPERTY_NAME_AUTO_MIN_MAX).getValue().equals(true))) {
                return;
            } else if (evt.getPropertyName().equals(PROPERTY_NAME_MAX) && (evt.getOldValue().equals(evt.getNewValue()) ||
                    container.getProperty(PROPERTY_NAME_AUTO_MIN_MAX).getValue().equals(true))) {
                return;
            } else if (evt.getPropertyName().equals(PROPERTY_NAME_X_PRODUCT) ||
                    evt.getPropertyName().equals(PROPERTY_NAME_Y_PRODUCT) ||
                    evt.getPropertyName().equals(PROPERTY_NAME_Z_PRODUCT)) {
                enableRefreshButton = false; // a product change will always cause an unselection of a raster
            } else if (evt.getPropertyName().equals(PROPERTY_NAME_X_BAND) &&
                    (container.getProperty(PROPERTY_NAME_Y_BAND).getValue() == null ||
                            (names.contains(PROPERTY_NAME_Z_BAND) &&
                                    (container.getProperty(PROPERTY_NAME_Z_BAND).getValue() == null)))) {
                enableRefreshButton = false;
            } else if (evt.getPropertyName().equals(PROPERTY_NAME_Y_BAND) &&
                    (container.getProperty(PROPERTY_NAME_X_BAND).getValue() == null ||
                            (names.contains(PROPERTY_NAME_Z_BAND) &&
                                    (container.getProperty(PROPERTY_NAME_Z_BAND).getValue() == null)))) {
                enableRefreshButton = false;
            } else if (evt.getPropertyName().equals(PROPERTY_NAME_Z_BAND) &&
                    (container.getProperty(PROPERTY_NAME_X_BAND).getValue() == null ||
                            container.getProperty(PROPERTY_NAME_Y_BAND).getValue() == null)) {
                enableRefreshButton = false;
            }
            refreshButton.setEnabled(enableRefreshButton);
        }
    }

}
