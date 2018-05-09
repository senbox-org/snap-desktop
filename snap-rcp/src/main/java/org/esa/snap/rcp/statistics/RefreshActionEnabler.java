package org.esa.snap.rcp.statistics;

import com.bc.ceres.binding.PropertyContainer;

import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashSet;

/**
 * @author Tonio Fincke
 */
public class RefreshActionEnabler implements PropertyChangeListener {

    private final static String PROPERTY_NAME_AUTO_MIN_MAX = "autoMinMax";
    private final static String PROPERTY_NAME_MIN = "min";
    private final static String PROPERTY_NAME_MAX = "max";
    private final static String PROPERTY_NAME_USE_ROI_MASK = "useRoiMask";
    private final static String PROPERTY_NAME_ROI_MASK = "roiMask";

    private HashSet<String> names = new HashSet<>();
    private List<ProductBandEnablement> productBandEnablements = new ArrayList<>();
    private AbstractButton refreshButton;

    public RefreshActionEnabler(AbstractButton rb, String... componentNames) {
        names.addAll(Arrays.asList(componentNames));
        refreshButton = rb;
    }

    public void addProductBandEnablement(String productPropertyName, String bandPropertyName) {
        addProductBandEnablement(productPropertyName, bandPropertyName, false);
    }

    public void addProductBandEnablement(String productPropertyName, String bandPropertyName, boolean isOptional) {
        productBandEnablements.add(new ProductBandEnablement(productPropertyName, bandPropertyName, isOptional));
        names.add(productPropertyName);
        names.add(bandPropertyName);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (names.contains(evt.getPropertyName())) {
            final PropertyContainer container = (PropertyContainer) evt.getSource();
            boolean enableRefreshButton = true;
            if (evt.getPropertyName().equals(PROPERTY_NAME_USE_ROI_MASK) && evt.getNewValue().equals(true) &&
                    container.getProperty(PROPERTY_NAME_ROI_MASK).getValue() == null) {
                return;
            } else if (evt.getPropertyName().equals(PROPERTY_NAME_AUTO_MIN_MAX) && evt.getNewValue().equals(false)) {
                return;
            } else if (evt.getPropertyName().equals(PROPERTY_NAME_MIN) && (evt.getOldValue().equals(evt.getNewValue()) ||
                    container.getProperty(PROPERTY_NAME_AUTO_MIN_MAX).getValue().equals(true))) {
                return;
            } else if (evt.getPropertyName().equals(PROPERTY_NAME_MAX) && (evt.getOldValue().equals(evt.getNewValue()) ||
                    container.getProperty(PROPERTY_NAME_AUTO_MIN_MAX).getValue().equals(true))) {
                return;
            } else if (isFromMandatoryProduct(evt.getPropertyName()) && ((evt.getOldValue() == null && evt.getNewValue() != null) ||
                    (evt.getOldValue() != null && evt.getNewValue() == null) ||
                    !evt.getOldValue().equals(evt.getNewValue()))) {
                enableRefreshButton = false;
            } else if (isFromOptionalProduct(evt.getPropertyName()) && ((evt.getOldValue() == null && evt.getNewValue() != null) ||
                    (evt.getOldValue() != null && evt.getNewValue() == null) ||
                    !evt.getOldValue().equals(evt.getNewValue()))) {
                enableRefreshButton = true;
            } else if (isFromBand(evt.getPropertyName())) {
                if (notAllMandatoryBandsAreValid(container)) {
                    enableRefreshButton = false;
                } else if ((evt.getOldValue() == null && evt.getNewValue() != null) ||
                        (evt.getOldValue() != null && evt.getNewValue() == null) ||
                        !evt.getOldValue().equals(evt.getNewValue())) {
                    enableRefreshButton = true;
                }
            } else if ((evt.getOldValue() == null && evt.getNewValue() == null) ||
                    (evt.getOldValue() != null && evt.getNewValue() != null &&
                            evt.getOldValue().equals(evt.getNewValue()))) {
                return;
            }
            refreshButton.setEnabled(enableRefreshButton);
        }
    }

    private boolean notAllMandatoryBandsAreValid(PropertyContainer container) {
        for (ProductBandEnablement enablement : productBandEnablements) {
            if (container.getProperty(enablement.bandName).getValue() == null && !enablement.isOptional) {
                return true;
            }
        }
        return false;
    }

    private boolean isFromBand(String propertyName) {
        for (ProductBandEnablement enablement : productBandEnablements) {
            if (propertyName.equals(enablement.bandName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFromMandatoryProduct(String propertyName) {
        for (ProductBandEnablement enablement : productBandEnablements) {
            if (propertyName.equals(enablement.productName) && !enablement.isOptional) {
                return true;
            }
        }
        return false;
    }

    private boolean isFromOptionalProduct(String propertyName) {
        for (ProductBandEnablement enablement : productBandEnablements) {
            if (propertyName.equals(enablement.productName) && enablement.isOptional) {
                return true;
            }
        }
        return false;
    }

    private static class ProductBandEnablement {

        String productName;
        String bandName;
        boolean isOptional;

        ProductBandEnablement(String productName, String bandName, boolean isOptional) {
            this.productName = productName;
            this.bandName = bandName;
            this.isOptional = isOptional;
        }

    }

}
