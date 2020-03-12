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

import org.esa.snap.core.datamodel.ColorManipulationDefaults;
import org.esa.snap.core.datamodel.ColorSchemeInfo;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.util.ImageUtilities;

import javax.swing.AbstractButton;

/**
 * @author Brockmann Consult
 * @author Daniel Knowles (NASA)
 */
// FEB 2020 - Knowles
//          - Added additional autoStretch buttons
//          - Modified handling of the horizontal zoom with a single toggle button
//          - Added method and call to resetSchemeSelector()
//          - Added button for setting range (0,1) useful for in RGB window for reflectance bands


class ImageInfoEditorSupport {

    final AbstractButton autoStretch1SigmaButton;
    final AbstractButton autoStretch2SigmaButton;
    final AbstractButton autoStretch3SigmaButton;
    final AbstractButton autoStretch95Button;
    final AbstractButton autoStretch100Button;
    final AbstractButton zoomInVButton;
    final AbstractButton zoomOutVButton;
    final AbstractButton zoomInHButton;
    final AbstractButton zoomOutHButton;
    final AbstractButton showExtraInfoButton;
    final AbstractButton setRGBminmax;


    final AbstractButton zoomHorizontalButton;
    final AbstractButton zoomVerticalButton;

    final ImageInfoEditor2 imageInfoEditor;
    ColorManipulationForm form;

    final Boolean[] horizontalZoomButtonEnabled = {true};


    protected ImageInfoEditorSupport(ImageInfoEditor2 imageInfoEditor, boolean zoomDefault) {
        this.imageInfoEditor = imageInfoEditor;
        this.form = imageInfoEditor.getParentForm();

        autoStretch1SigmaButton = createButton("org/esa/snap/rcp/icons/Auto1SigmaPercent24.gif");
        autoStretch1SigmaButton.setName("AutoStretch1SigmaButton");
        autoStretch1SigmaButton.setToolTipText("Auto-adjust to 1 sigma (68.27%) of all pixels");
        autoStretch1SigmaButton.addActionListener(form.wrapWithAutoApplyActionListener(e -> compute1SigmaPercent()));

        autoStretch2SigmaButton = createButton("org/esa/snap/rcp/icons/Auto2SigmaPercent24.gif");
        autoStretch2SigmaButton.setName("AutoStretch2SigmaButton");
        autoStretch2SigmaButton.setToolTipText("Auto-adjust to 2 sigma (95.45%) of all pixels");
        autoStretch2SigmaButton.addActionListener(form.wrapWithAutoApplyActionListener(e -> compute2SigmaPercent()));

        autoStretch3SigmaButton = createButton("org/esa/snap/rcp/icons/Auto3SigmaPercent24.gif");
        autoStretch3SigmaButton.setName("AutoStretch3SigmaButton");
        autoStretch3SigmaButton.setToolTipText("Auto-adjust to 3 sigma (99.73%) of all pixels");
        autoStretch3SigmaButton.addActionListener(form.wrapWithAutoApplyActionListener(e -> compute3SigmaPercent()));

        autoStretch95Button = createButton("org/esa/snap/rcp/icons/Auto95Percent24.gif");
        autoStretch95Button.setName("AutoStretch95Button");
        autoStretch95Button.setToolTipText("Auto-adjust to 95% of all pixels");
        autoStretch95Button.addActionListener(form.wrapWithAutoApplyActionListener(e -> compute95Percent()));

        autoStretch100Button = createButton("org/esa/snap/rcp/icons/Auto100Percent24.gif");
        autoStretch100Button.setName("AutoStretch100Button");
        autoStretch100Button.setToolTipText("Auto-adjust to 100% of all pixels");
        autoStretch100Button.addActionListener(form.wrapWithAutoApplyActionListener(e -> compute100Percent()));

        setRGBminmax = createButton("org/esa/snap/rcp/icons/AutoPresetRange24.png");
        setRGBminmax.setName("setRGBminmax");
        setRGBminmax.setToolTipText("<html>Set channel range with pre-set values (see preferences)</html>"); /*I18N*/
        setRGBminmax.addActionListener(form.wrapWithAutoApplyActionListener(e -> setRGBminmax()));



        zoomInVButton = createButton("org/esa/snap/rcp/icons/ZoomIn24V.gif");
        zoomInVButton.setName("zoomInVButton");
        zoomInVButton.setToolTipText("Stretch histogram vertically");
        zoomInVButton.addActionListener(e -> imageInfoEditor.computeZoomInVertical());

        zoomOutVButton = createButton("org/esa/snap/rcp/icons/ZoomOut24V.gif");
        zoomOutVButton.setName("zoomOutVButton");
        zoomOutVButton.setToolTipText("Shrink histogram vertically");
        zoomOutVButton.addActionListener(e -> imageInfoEditor.computeZoomOutVertical());

        zoomInHButton = createButton("org/esa/snap/rcp/icons/ZoomIn24H.gif");
        zoomInHButton.setName("zoomInHButton");
        zoomInHButton.setToolTipText("Stretch histogram horizontally");
        zoomInHButton.addActionListener(e -> imageInfoEditor.computeZoomInToSliderLimits());

        zoomOutHButton = createButton("org/esa/snap/rcp/icons/ZoomOut24H.gif");
        zoomOutHButton.setName("zoomOutHButton");
        zoomOutHButton.setToolTipText("Shrink histogram horizontally");
        zoomOutHButton.addActionListener(e -> imageInfoEditor.computeZoomOutToFullHistogramm());


        zoomHorizontalButton = createToggleButton("org/esa/snap/rcp/icons/ZoomHorizontal24.gif");
        zoomHorizontalButton.setName("zoomHorizontalButton");
        zoomHorizontalButton.setToolTipText("Expand and shrink histogram horizontally");
        zoomHorizontalButton.setSelected(zoomDefault);
        zoomHorizontalButton.addActionListener(e -> handleHorizontalZoomButton());

        zoomVerticalButton = createToggleButton("org/esa/snap/rcp/icons/ZoomVertical24.gif");
        zoomVerticalButton.setName("zoomVerticalButton");
        zoomVerticalButton.setToolTipText("Expand and shrink histogram vertically");
        zoomVerticalButton.addActionListener(e -> handleVerticalZoom());


        showExtraInfoButton = createToggleButton("org/esa/snap/rcp/icons/Information24.gif");
        showExtraInfoButton.setName("ShowExtraInfoButton");
        showExtraInfoButton.setToolTipText("Show extra information");
        showExtraInfoButton.setSelected(imageInfoEditor.getShowExtraInfo());
        showExtraInfoButton.addActionListener(e -> imageInfoEditor.setShowExtraInfo(showExtraInfoButton.isSelected()));
    }


    private void resetSchemeSelector() {
        ColorSchemeInfo colorSchemeNoneInfo = ColorSchemeManager.getDefault().getNoneColorSchemeInfo();
        form.getFormModel().getProductSceneView().getImageInfo().setColorSchemeInfo(colorSchemeNoneInfo);
        form.getFormModel().getModifiedImageInfo().setColorSchemeInfo(colorSchemeNoneInfo);
    }



    public void handleVerticalZoom() {
        if (zoomVerticalButton.isSelected()) {
            imageInfoEditor.computeZoomInVertical();
        } else {
            imageInfoEditor.computeZoomOutVertical();
        }
    }


    public void setHorizontalZoomButtonAndCompute(boolean zoomToHistLimits) {
        if (zoomToHistLimits != zoomHorizontalButton.isSelected()) {
            boolean tmp = horizontalZoomButtonEnabled[0];
            horizontalZoomButtonEnabled[0] = false;
            zoomHorizontalButton.setSelected(zoomToHistLimits);
            horizontalZoomButtonEnabled[0] = tmp;;
        }

        computeHorizontalZoom(zoomToHistLimits);
    }

    private void computeHorizontalZoom(boolean zoomToHistLimits) {
        if (zoomToHistLimits) {
            imageInfoEditor.computeZoomInToSliderLimits();
        } else {
            imageInfoEditor.computeZoomOutToFullHistogramm();
        }
    }


    public void handleHorizontalZoomButton() {
        if (horizontalZoomButtonEnabled[0]) {
            form.getFormModel().getProductSceneView().getImageInfo().setZoomToHistLimits(zoomHorizontalButton.isSelected());
            form.getFormModel().getModifiedImageInfo().setZoomToHistLimits(zoomHorizontalButton.isSelected());

            computeHorizontalZoom(zoomHorizontalButton.isSelected());
        }
    }


    private void compute1SigmaPercent() {
        computePercent(68.27);
    }
    private void compute2SigmaPercent() {
        computePercent(95.45);
    }
    private void compute3SigmaPercent() {
        computePercent(99.73);
    }

    private void compute95Percent() {
        computePercent(95.0);
    }

    private void computePercent(double threshold) {
        resetSchemeSelector();

        if (!imageInfoEditor.computePercent(form.getFormModel().getProductSceneView().getImageInfo().isLogScaled(), threshold)) {
            ColorUtils.showErrorDialog("INPUT ERROR!!: Cannot set slider value below zero with log scaling");
        }
    }

    private void setRGBminmax() {
        PropertyMap configuration = form.getFormModel().getProductSceneView().getSceneImage().getConfiguration();
        double rgbMin = configuration.getPropertyDouble(ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MIN_KEY, ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MIN_DEFAULT);
        double rgbMax = configuration.getPropertyDouble(ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MAX_KEY, ColorManipulationDefaults.PROPERTY_RGB_OPTIONS_MAX_DEFAULT);

        imageInfoEditor.setRGBminmax(rgbMin, rgbMax);
    }



    private void compute100Percent() {
        resetSchemeSelector();

        if (!imageInfoEditor.compute100Percent(form.getFormModel().getProductSceneView().getImageInfo().isLogScaled())) {
            ColorUtils.showErrorDialog("INPUT ERROR!!: Cannot set slider value below zero with log scaling");
        }
    }


    public static AbstractButton createToggleButton(String s) {
        return ToolButtonFactory.createButton(ImageUtilities.loadImageIcon(s, false), true);
    }

    public static AbstractButton createButton(String s) {
        return ToolButtonFactory.createButton(ImageUtilities.loadImageIcon(s, false), false);
    }
}
