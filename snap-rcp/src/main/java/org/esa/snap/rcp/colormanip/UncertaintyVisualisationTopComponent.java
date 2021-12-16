/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.rcp.colormanip;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.ui.PackageDefaults;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;


/**
 * The uncertainty visualisation manipulation tool window.
 */
@TopComponent.Description(
        preferredID = "UncertaintyVisualisationTopComponent",
        iconBase = "org/esa/snap/rcp/icons/UncertaintyStretch.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = PackageDefaults.UNCERTAINTY_MODE,
        openAtStartup = PackageDefaults.UNCERTAINTY_OPEN,
        position = PackageDefaults.UNCERTAINTY_POSITION
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.colormanip.UncertaintyVisualisationTopComponent")
@ActionReference(path = "Menu/View/Tool Windows")
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_UncertaintyVisualisationTopComponent_Name",
        preferredID = "UncertaintyVisualisationTopComponent"
)
@NbBundle.Messages({
        "CTL_UncertaintyVisualisationTopComponent_Name=" + PackageDefaults.UNCERTAINTY_NAME,
        "CTL_UncertaintyVisualisationTopComponent_ComponentName=Uncertainty_Visualisation"
})
public class UncertaintyVisualisationTopComponent extends TopComponent {

    public static final String UNCERTAINTY_MODE_PROPERTY = "uncertaintyMode";


    public UncertaintyVisualisationTopComponent() {
        setName(Bundle.CTL_UncertaintyVisualisationTopComponent_ComponentName());
        ColorManipulationForm cmf = new ColorManipulationFormImpl(this, new UncertaintyFormModel());
        setLayout(new BorderLayout());
        add(cmf.getContentPanel(), BorderLayout.CENTER);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("showUncertaintyManipulationWnd");
    }

    private static class UncertaintyFormModel extends ColorFormModel {
        @Override
        public String getTitlePrefix() {
            return Bundle.CTL_UncertaintyVisualisationTopComponent_Name();
        }

        @Override
        public boolean isValid() {
            return super.isValid() && !getProductSceneView().isRGB() && getRaster() != null;
        }

        @Override
        public RasterDataNode getRaster() {
            RasterDataNode raster = getProductSceneView().getRaster();
            return ImageManager.getUncertaintyBand(raster);
        }

        @Override
        public RasterDataNode[] getRasters() {
            RasterDataNode raster = getRaster();
            if (raster != null) {
                return new RasterDataNode[]{raster};
            }
            return null;
        }

        @Override
        public void setRasters(RasterDataNode[] rasters) {
            // not applicable
        }

        @Override
        public ImageInfo getOriginalImageInfo() {
            return getRaster().getImageInfo(ProgressMonitor.NULL);
        }

        @Override
        public void applyModifiedImageInfo() {
            getProductSceneView().updateImage();
        }

        @Override
        public boolean canUseHistogramMatching() {
            return false;
        }

        @Override
        public boolean isMoreOptionsFormCollapsedOnInit() {
            return false;
        }

        @Override
        public void modifyMoreOptionsForm(MoreOptionsForm moreOptionsForm) {

            JComboBox<ImageInfo.UncertaintyVisualisationMode> modeBox = new JComboBox<>(ImageInfo.UncertaintyVisualisationMode.values());
            modeBox.setEditable(false);

            moreOptionsForm.insertRow(0, new JLabel("Visualisation mode: "), modeBox);

            Property modeProperty = Property.create(UNCERTAINTY_MODE_PROPERTY, ImageInfo.UncertaintyVisualisationMode.class);
            RasterDataNode uncertaintyBand = getRaster();
            try {
                if (uncertaintyBand != null) {
                    modeProperty.setValue(uncertaintyBand.getImageInfo(ProgressMonitor.NULL).getUncertaintyVisualisationMode());
                } else {
                    modeProperty.setValue(ImageInfo.UncertaintyVisualisationMode.None);
                }
            } catch (ValidationException e) {
                // ok
            }
            moreOptionsForm.getBindingContext().getPropertySet().addProperty(modeProperty);
            moreOptionsForm.getBindingContext().bind(modeProperty.getName(), modeBox);

            moreOptionsForm.getBindingContext().addPropertyChangeListener(modeProperty.getName(), evt -> {
                RasterDataNode uncertainBand = getRaster();
                if (uncertainBand != null) {
                    ImageInfo.UncertaintyVisualisationMode uvMode = (ImageInfo.UncertaintyVisualisationMode) evt.getNewValue();
                    ImageInfo imageInfo = uncertainBand.getImageInfo();
                    imageInfo.setUncertaintyVisualisationMode(uvMode);
                    setModifiedImageInfo(imageInfo);
                    //uncertainBand.fireImageInfoChanged();
                    applyModifiedImageInfo();

                    moreOptionsForm.getChildForm().updateFormModel(moreOptionsForm.getParentForm().getFormModel());
                }
            });
        }

        @Override
        public void updateMoreOptionsFromImageInfo(MoreOptionsForm moreOptionsForm) {
            super.updateMoreOptionsFromImageInfo(moreOptionsForm);
            BindingContext bindingContext = moreOptionsForm.getBindingContext();
            ImageInfo.UncertaintyVisualisationMode mode = getModifiedImageInfo().getUncertaintyVisualisationMode();
            bindingContext.getBinding(UNCERTAINTY_MODE_PROPERTY).setPropertyValue(mode);
        }

        @Override
        public void updateImageInfoFromMoreOptions(MoreOptionsForm moreOptionsForm) {
            super.updateImageInfoFromMoreOptions(moreOptionsForm);
            BindingContext bindingContext = moreOptionsForm.getBindingContext();
            ImageInfo.UncertaintyVisualisationMode mode = (ImageInfo.UncertaintyVisualisationMode) bindingContext.getBinding(UNCERTAINTY_MODE_PROPERTY).getPropertyValue();
            getModifiedImageInfo().setUncertaintyVisualisationMode(mode);
        }

        @Override
        public Component createEmptyContentPanel() {
            return new JLabel("<html>This tool window is used to visualise the<br>" +
                                      "<b>uncertainty information</b> associated<br>" +
                                      "with a band shown in an image view.<br>" +
                                      "Right now, there is no selected image view or<br>" +
                                      "uncertainty information is unavailable.", SwingConstants.CENTER);
        }
    }
}
