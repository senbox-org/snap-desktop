package org.esa.snap.rcp.colormanip;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
//public class ScatterPlot3DFormModel implements FormModelInterface {
public class ScatterPlot3DFormModel extends ColorFormModel {

    private RasterDataNode raster;
    private ImageInfo modifiedInfo;
    private final List<PropertyChangeListener> propertyChangeListeners;

    public ScatterPlot3DFormModel() {
        propertyChangeListeners = new ArrayList<>();
    }

    @Override
    public RasterDataNode getRaster() {
        return raster;
    }

    void setRaster(RasterDataNode raster) {
        this.raster = raster;
        modifiedInfo = null;
    }

    @Override
    public RasterDataNode[] getRasters() {
        return new RasterDataNode[]{raster};
    }

    @Override
    public ImageInfo getOriginalImageInfo() {
        ImageInfo originalInfo = null;
        if (raster != null) {
            originalInfo = raster.getImageInfo();
            if (originalInfo == null) {
                originalInfo = raster.createDefaultImageInfo(null, ProgressMonitor.NULL);
            }
        }
        return originalInfo;
    }

    @Override
    public ImageInfo getModifiedImageInfo() {
        if (raster == null) {
            modifiedInfo = null;
        } else if (modifiedInfo == null) {
            modifiedInfo = getOriginalImageInfo();
        }
        return modifiedInfo;
    }

    @Override
    public void setModifiedImageInfo(ImageInfo imageInfo) {
        modifiedInfo = imageInfo;
    }

    @Override
    public void applyModifiedImageInfo() {
        final PropertyChangeEvent event = new PropertyChangeEvent(this, "imageInfo", null, modifiedInfo);
        firePropertyChange(event);
    }

    private void firePropertyChange(PropertyChangeEvent event) {
        for (PropertyChangeListener propertyChangeListener : propertyChangeListeners)  {
            propertyChangeListener.propertyChange(event);
        }
    }

    @Override
    public Product getProduct() {
        if (raster != null) {
            return raster.getProduct();
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return getRaster() != null;
    }

    @Override
    public boolean isContinuous1BandImage() {
        return isValid() && getRaster() instanceof Band && !((Band) getRaster()).isIndexBand();
    }

    @Override
    public boolean isDiscrete1BandImage() {
        return isValid() && getRaster() instanceof Band && ((Band) getRaster()).isIndexBand();
    }

    @Override
    public boolean canUseHistogramMatching() {
        return true;
    }

    @Override
    public void modifyMoreOptionsForm(MoreOptionsForm moreOptionsForm) {
        //do nothing
        //todo get rid of this
    }

    @Override
    public void updateMoreOptionsFromImageInfo(MoreOptionsForm moreOptionsForm) {
        //do nothing
    }

    @Override
    public void updateImageInfoFromMoreOptions(MoreOptionsForm moreOptionsForm) {
        //do nothing
    }

    @Override
    public Component createEmptyContentPanel() {
        return new JLabel("<html>Select a band to display as colour.", SwingConstants.CENTER);
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertyChangeListeners.add(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertyChangeListeners.remove(propertyChangeListener);
    }
    
}
