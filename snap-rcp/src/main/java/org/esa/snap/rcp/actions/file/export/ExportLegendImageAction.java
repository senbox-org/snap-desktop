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
package org.esa.snap.rcp.actions.file.export;

import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.ImageLegend;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.param.ParamGroup;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.PreferencesPropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.prefs.Preferences;

@ActionID(
        category = "File",
        id = "org.esa.snap.rcp.actions.file.export.ExportLegendImageAction"
)
@ActionRegistration(
        displayName = "#CTL_ExportLegendImageAction_MenuText",
        popupText = "#CTL_ExportLegendImageAction_PopupText",
        lazy = false

)
@ActionReferences({
        @ActionReference(path = "Menu/File/Export/Other", position = 10),
        @ActionReference(path = "Context/ProductSceneView", position = 90)
})
@NbBundle.Messages({
        "CTL_ExportLegendImageAction_MenuText=Colour Legend as Image",
        "CTL_ExportLegendImageAction_PopupText=Export Colour Legend as Image",
        "CTL_ExportLegendImageAction_ShortDescription=Export the colour legend of the current view as an image."
})

public class ExportLegendImageAction extends AbstractExportImageAction {
    private final static String[][] IMAGE_FORMAT_DESCRIPTIONS = {
            BMP_FORMAT_DESCRIPTION,
            PNG_FORMAT_DESCRIPTION,
            JPEG_FORMAT_DESCRIPTION,
            TIFF_FORMAT_DESCRIPTION,
    };

    private static final String HELP_ID = "exportLegendImageFile";
    private static final String HORIZONTAL_STR = "Horizontal";
    private static final String VERTICAL_STR = "Vertical";

    private SnapFileFilter[] imageFileFilters;

    private ParamGroup legendParamGroup;
    private ImageLegend imageLegend;
    @SuppressWarnings("FieldCanBeLocal")
    private Lookup.Result<ProductSceneView> result;


    public ExportLegendImageAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ExportLegendImageAction(Lookup lookup) {
        super(Bundle.CTL_ExportLegendImageAction_MenuText(), HELP_ID);
        putValue("popupText",Bundle.CTL_ExportLegendImageAction_PopupText());
        imageFileFilters = new SnapFileFilter[IMAGE_FORMAT_DESCRIPTIONS.length];
        for (int i = 0; i < IMAGE_FORMAT_DESCRIPTIONS.length; i++) {
            imageFileFilters[i] = createFileFilter(IMAGE_FORMAT_DESCRIPTIONS[i]);
        }

        result = lookup.lookupResult(ProductSceneView.class);
        result.addLookupListener(WeakListeners.create(LookupListener.class, this, result));
        setEnabled(false);
    }


    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return new ExportLegendImageAction(lookup);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        exportImage(imageFileFilters);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        ProductSceneView view = SnapApp.getDefault().getSelectedProductSceneView();
        boolean enabled = view != null && !view.isRGB();
        setEnabled(enabled);
    }


    @Override
    protected void configureFileChooser(SnapFileChooser fileChooser, ProductSceneView view, String imageBaseName) {
        legendParamGroup = createLegendParamGroup();
        final Preferences preferences = SnapApp.getDefault().getPreferences();
        legendParamGroup.setParameterValues(new PreferencesPropertyMap(preferences), null);

        modifyHeaderText(legendParamGroup, view.getRaster());
        fileChooser.setDialogTitle(SnapApp.getDefault().getInstanceName() + " - export Colour Legend Image"); /*I18N*/


        fileChooser.setCurrentFilename(imageBaseName + "_legend");
        final RasterDataNode raster = view.getRaster();
        imageLegend = new ImageLegend(raster.getImageInfo(), raster);
        fileChooser.setAccessory(createImageLegendAccessory(
                fileChooser,
                legendParamGroup,
                imageLegend, getHelpCtx().getHelpID()));
    }

    @Override
    protected RenderedImage createImage(String imageFormat, ProductSceneView view) {
        transferParamsToImageLegend(legendParamGroup, imageLegend);
        imageLegend.setBackgroundTransparencyEnabled(isTransparencySupportedByFormat(imageFormat));
        return imageLegend.createImage();
    }

    @Override
    protected boolean isEntireImageSelected() {
        return true;
    }

    private static ParamGroup createLegendParamGroup() {
        ParamGroup paramGroup = new ParamGroup();

        Parameter param = new Parameter("legend.usingHeader", Boolean.TRUE);
        param.getProperties().setLabel("Show header text");
        paramGroup.addParameter(param);

        param = new Parameter("legend.headerText", "");
        param.getProperties().setLabel("Header text");
        param.getProperties().setNumCols(24);
        param.getProperties().setNullValueAllowed(true);
        paramGroup.addParameter(param);

        param = new Parameter("legend.orientation", HORIZONTAL_STR);
        param.getProperties().setLabel("Orientation");
        param.getProperties().setValueSet(new String[]{HORIZONTAL_STR, VERTICAL_STR});
        param.getProperties().setValueSetBound(true);
        paramGroup.addParameter(param);

        param = new Parameter("legend.fontSize", 14);
        param.getProperties().setLabel("Font size");
        param.getProperties().setMinValue(4);
        param.getProperties().setMaxValue(100);
        paramGroup.addParameter(param);

        param = new Parameter("legend.foregroundColor", Color.black);
        param.getProperties().setLabel("Foreground colour");
        paramGroup.addParameter(param);

        param = new Parameter("legend.backgroundColor", Color.white);
        param.getProperties().setLabel("Background colour");
        paramGroup.addParameter(param);

        param = new Parameter("legend.backgroundTransparency", 0.0f);
        param.getProperties().setLabel("Background transparency");
        param.getProperties().setMinValue(0.0f);
        param.getProperties().setMaxValue(1.0f);
        paramGroup.addParameter(param);

        param = new Parameter("legend.antialiasing", Boolean.TRUE);
        param.getProperties().setLabel("Perform anti-aliasing");
        paramGroup.addParameter(param);

        return paramGroup;
    }

    private static void modifyHeaderText(ParamGroup legendParamGroup, RasterDataNode raster) {
        String name = raster.getName();
        String unit = raster.getUnit() != null ? raster.getUnit() : "-";
        unit = unit.replace('*', ' ');
        String headerText = name + " [" + unit + "]";
        legendParamGroup.getParameter("legend.headerText").setValue(headerText, null);
    }

    private static JComponent createImageLegendAccessory(final JFileChooser fileChooser,
                                                         final ParamGroup legendParamGroup,
                                                         final ImageLegend imageLegend, String helpId) {
        final JButton button = new JButton("Properties...");
        button.setMnemonic('P');
        button.addActionListener(e -> {
            final SnapFileFilter fileFilter = (SnapFileFilter) fileChooser.getFileFilter();
            final ImageLegendDialog dialog = new ImageLegendDialog(
                    legendParamGroup,
                    imageLegend,
                    isTransparencySupportedByFormat(fileFilter.getFormatName()), helpId);
            dialog.show();
        });
        final JPanel accessory = new JPanel(new BorderLayout());
        accessory.setBorder(new EmptyBorder(3, 3, 3, 3));
        accessory.add(button, BorderLayout.NORTH);
        return accessory;
    }

    private static void transferParamsToImageLegend(ParamGroup legendParamGroup, ImageLegend imageLegend) {
        Object value;

        value = legendParamGroup.getParameter("legend.usingHeader").getValue();
        imageLegend.setUsingHeader((Boolean) value);

        value = legendParamGroup.getParameter("legend.headerText").getValue();
        imageLegend.setHeaderText((String) value);

        value = legendParamGroup.getParameter("legend.orientation").getValue();
        imageLegend.setOrientation(HORIZONTAL_STR.equals(value) ? ImageLegend.HORIZONTAL : ImageLegend.VERTICAL);

        value = legendParamGroup.getParameter("legend.fontSize").getValue();
        imageLegend.setFont(imageLegend.getFont().deriveFont(((Number) value).floatValue()));

        value = legendParamGroup.getParameter("legend.backgroundColor").getValue();
        imageLegend.setBackgroundColor((Color) value);

        value = legendParamGroup.getParameter("legend.foregroundColor").getValue();
        imageLegend.setForegroundColor((Color) value);

        value = legendParamGroup.getParameter("legend.backgroundTransparency").getValue();
        imageLegend.setBackgroundTransparency(((Number) value).floatValue());

        value = legendParamGroup.getParameter("legend.antialiasing").getValue();
        imageLegend.setAntialiasing((Boolean) value);
    }


    public static class ImageLegendDialog extends ModalDialog {

        private ImageInfo imageInfo;
        private RasterDataNode raster;
        private boolean transparencyEnabled;

        private ParamGroup paramGroup;

        private Parameter usingHeaderParam;
        private Parameter headerTextParam;
        private Parameter orientationParam;
        private Parameter fontSizeParam;
        private Parameter backgroundColorParam;
        private Parameter foregroundColorParam;
        private Parameter antialiasingParam;
        private Parameter backgroundTransparencyParam;

        public ImageLegendDialog(ParamGroup paramGroup, ImageLegend imageLegend,
                                 boolean transparencyEnabled, String helpId) {
            super(SnapApp.getDefault().getMainFrame(), SnapApp.getDefault().getInstanceName() + " - Colour Legend Properties", ID_OK_CANCEL, helpId);
            this.imageInfo = imageLegend.getImageInfo();
            this.raster = imageLegend.getRaster();
            this.transparencyEnabled = transparencyEnabled;
            this.paramGroup = paramGroup;
            initParams();
            initUI();
            updateUIState();
            this.paramGroup.addParamChangeListener(event -> updateUIState());
        }

        private void updateUIState() {
            boolean headerTextEnabled = (Boolean) usingHeaderParam.getValue();
            headerTextParam.setUIEnabled(headerTextEnabled);
            backgroundTransparencyParam.setUIEnabled(transparencyEnabled);
        }

        public ParamGroup getParamGroup() {
            return paramGroup;
        }

        public void getImageLegend(ImageLegend imageLegend) {
            transferParamsToImageLegend(getParamGroup(), imageLegend);
        }

        public ImageInfo getImageInfo() {
            return imageInfo;
        }

        @Override
        protected void onOK() {
            final Preferences preferences = SnapApp.getDefault().getPreferences();
            getParamGroup().getParameterValues(new PreferencesPropertyMap(preferences));
            super.onOK();
        }

        private void initUI() {
            final JButton previewButton = new JButton("Preview...");
            previewButton.setMnemonic('v');
            previewButton.addActionListener(e -> showPreview());

            final GridBagConstraints gbc = new GridBagConstraints();
            final JPanel p = GridBagUtils.createPanel();

            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets.top = 0;

            gbc.gridy = 0;
            gbc.gridwidth = 2;
            p.add(usingHeaderParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            p.add(headerTextParam.getEditor().getLabelComponent(), gbc);
            p.add(headerTextParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 10;
            p.add(orientationParam.getEditor().getLabelComponent(), gbc);
            p.add(orientationParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 3;
            p.add(fontSizeParam.getEditor().getLabelComponent(), gbc);
            p.add(fontSizeParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 10;
            p.add(foregroundColorParam.getEditor().getLabelComponent(), gbc);
            p.add(foregroundColorParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 3;
            p.add(backgroundColorParam.getEditor().getLabelComponent(), gbc);
            p.add(backgroundColorParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;
            gbc.insets.top = 3;
            p.add(backgroundTransparencyParam.getEditor().getLabelComponent(), gbc);
            p.add(backgroundTransparencyParam.getEditor().getEditorComponent(), gbc);

            gbc.gridy++;

            gbc.insets.top = 10;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            p.add(antialiasingParam.getEditor().getEditorComponent(), gbc);

            gbc.insets.top = 10;
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.NORTHEAST;
            p.add(previewButton, gbc);

            p.setBorder(new EmptyBorder(7, 7, 7, 7));

            setContent(p);
        }

        private void initParams() {
            usingHeaderParam = paramGroup.getParameter("legend.usingHeader");
            headerTextParam = paramGroup.getParameter("legend.headerText");
            orientationParam = paramGroup.getParameter("legend.orientation");
            fontSizeParam = paramGroup.getParameter("legend.fontSize");
            foregroundColorParam = paramGroup.getParameter("legend.foregroundColor");
            backgroundColorParam = paramGroup.getParameter("legend.backgroundColor");
            backgroundTransparencyParam = paramGroup.getParameter("legend.backgroundTransparency");
            antialiasingParam = paramGroup.getParameter("legend.antialiasing");
        }

        private void showPreview() {
            final ImageLegend imageLegend = new ImageLegend(getImageInfo(), raster);
            getImageLegend(imageLegend);
            final BufferedImage image = imageLegend.createImage();
            final JLabel imageDisplay = new JLabel(new ImageIcon(image));
            imageDisplay.setOpaque(true);
            imageDisplay.addMouseListener(new MouseAdapter() {
                // Both events (releases & pressed) must be checked, otherwise it won't work on all
                // platforms

                /**
                 * Invoked when a mouse button has been released on a component.
                 */
                @Override
                public void mouseReleased(MouseEvent e) {
                    // On Windows
                    showPopup(e, image, imageDisplay);
                }

                /**
                 * Invoked when a mouse button has been pressed on a component.
                 */
                @Override
                public void mousePressed(MouseEvent e) {
                    // On Linux
                    // todo - clipboard does not work on linux.
                    // todo - better not to show popup until it works correctly
//                    showPopup(e, image, imageDisplay);
                }
            });
            final ModalDialog dialog = new ModalDialog(getParent(),
                                                       SnapApp.getDefault().getInstanceName() + " - Colour Legend Preview",
                                                       imageDisplay,
                                                       ID_OK, null);
            dialog.getJDialog().setResizable(false);
            dialog.show();
        }

        private static void showPopup(final MouseEvent e, final BufferedImage image, final JComponent imageDisplay) {
            if (e.isPopupTrigger()) {
                final JPopupMenu popupMenu = new JPopupMenu();
                final JMenuItem menuItem = new JMenuItem("Copy image to clipboard");
                menuItem.addActionListener(e1 -> SystemUtils.copyToClipboard(image));
                popupMenu.add(menuItem);
                popupMenu.show(imageDisplay, e.getX(), e.getY());
            }
        }
    }

}
