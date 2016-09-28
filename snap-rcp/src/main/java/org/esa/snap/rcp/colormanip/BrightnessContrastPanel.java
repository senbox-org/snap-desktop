package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.product.ProductSceneView;
import org.netbeans.api.annotations.common.NonNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jcoravu on 9/27/2016.
 */
public class BrightnessContrastPanel extends JPanel {
    private final ColorManipulationForm parentForm;
    private SliderPanel brightnessPanel;
    private SliderPanel contrastPanel;
    private SliderPanel saturationPanel;
    private PropertyChangeListener imageInfoChangeListener;
    private Map<ProductSceneView, BrightnessContrastData> visibleProductScenes;

    public BrightnessContrastPanel(ColorManipulationForm parentForm) {
        super(new BorderLayout());

        this.parentForm = parentForm;

        ChangeListener sliderChangeListener = event -> applySliderValues();

        this.brightnessPanel = new SliderPanel("Brightness", sliderChangeListener);
        this.contrastPanel = new SliderPanel("Contrast", sliderChangeListener);
        this.saturationPanel = new SliderPanel("Saturation", sliderChangeListener);

        int maximumPreferredWidth = Math.max(this.brightnessPanel.getTitlePreferredWidth(), this.contrastPanel.getTitlePreferredWidth());
        maximumPreferredWidth = Math.max(maximumPreferredWidth, this.saturationPanel.getTitlePreferredWidth());

        this.saturationPanel.setTitlePreferredWidth(maximumPreferredWidth);
        this.contrastPanel.setTitlePreferredWidth(maximumPreferredWidth);
        this.saturationPanel.setTitlePreferredWidth(maximumPreferredWidth);

        this.visibleProductScenes = new HashMap<>();

        this.imageInfoChangeListener = event -> {
            ImageInfo modifiedImageInfo = (ImageInfo) event.getNewValue();
            ProductSceneView selectedSceneView = (ProductSceneView)event.getSource();
            sceneImageInfoChangedOutside(selectedSceneView, modifiedImageInfo);
        };

        JPanel colorsPanel = new JPanel(new GridLayout(3, 1, 0, 15));
        colorsPanel.setBorder(new EmptyBorder(5, 5, 0, 0));
        colorsPanel.add(this.brightnessPanel);
        colorsPanel.add(this.contrastPanel);
        colorsPanel.add(this.saturationPanel);

        JButton resetButton = new JButton("Reset");
        resetButton.setFocusable(false);
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                resetSliderValues();
            }
        });

        JPanel componentsPanel = new JPanel();
        componentsPanel.setLayout(new BoxLayout(componentsPanel, BoxLayout.Y_AXIS));
        componentsPanel.add(colorsPanel);
        componentsPanel.add(resetButton);

        JScrollPane scrollPane = new JScrollPane(componentsPanel);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        add(scrollPane, BorderLayout.NORTH);
    }

    public void productSceneViewSelected(@NonNull ProductSceneView selectedSceneView) {
        selectedSceneView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_IMAGE_INFO, this.imageInfoChangeListener);

        BrightnessContrastData brightnessContrastData = this.visibleProductScenes.get(selectedSceneView);
        if (brightnessContrastData == null) {
            RasterDataNode[] rasterDataNodes = selectedSceneView.getSceneImage().getRasters();

            ImageInfo initialImageInfo = selectedSceneView.getImageInfo().clone();
            brightnessContrastData = new BrightnessContrastData(initialImageInfo);
            for (int i=0; i<rasterDataNodes.length; i++) {
                ImageInfo nodeImageInfo = rasterDataNodes[i].getImageInfo().clone();
                brightnessContrastData.putImageInfo(rasterDataNodes[i], nodeImageInfo);
            }

            this.visibleProductScenes.put(selectedSceneView, brightnessContrastData);
        }

        RGBChannelDef initialRGBChannelDef = brightnessContrastData.getInitialImageInfo().getRgbChannelDef();
        boolean enableSaturationPanel = (initialRGBChannelDef != null);
        this.saturationPanel.setEnabled(enableSaturationPanel);

        refreshSliderValues(brightnessContrastData);
    }

    public void productSceneViewDeselected(@NonNull ProductSceneView deselectedSceneView) {
        deselectedSceneView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_IMAGE_INFO, this.imageInfoChangeListener);
    }

    private void resetSliderValues() {
        ProductSceneView selectedSceneView = getSelectedProductSceneView();
        BrightnessContrastData brightnessContrastData = this.visibleProductScenes.get(selectedSceneView);
        brightnessContrastData.setSliderValues(0, 0, 0);
        refreshSliderValues(brightnessContrastData);
        applySliderValues();
    }

    private void refreshSliderValues(BrightnessContrastData brightnessContrastData) {
        this.brightnessPanel.setSliderValue(brightnessContrastData.getBrightnessSliderValue());
        this.contrastPanel.setSliderValue(brightnessContrastData.getContrastSliderValue());
        this.saturationPanel.setSliderValue(brightnessContrastData.getSaturationSliderValue());
    }

    private void sceneImageInfoChangedOutside(ProductSceneView selectedSceneView, ImageInfo modifiedImageInfo) {
        BrightnessContrastData brightnessContrastData = this.visibleProductScenes.get(selectedSceneView);
        ImageInfo initialImageInfo = modifiedImageInfo.clone();
        brightnessContrastData.setInitialImageInfo(initialImageInfo);
        brightnessContrastData.setSliderValues(0, 0, 0);
        refreshSliderValues(brightnessContrastData);
    }

    private ProductSceneView getSelectedProductSceneView() {
        return SnapApp.getDefault().getSelectedProductSceneView();
    }

    private void applySliderValues() {
        ProductSceneView selectedSceneView = getSelectedProductSceneView();

        int brightnessValue = this.brightnessPanel.getSliderValue();
        int contrastValue = this.contrastPanel.getSliderValue();
        int saturationValue = this.saturationPanel.getSliderValue();

        BrightnessContrastData brightnessContrastData = this.visibleProductScenes.get(selectedSceneView);
        brightnessContrastData.setSliderValues(brightnessValue, contrastValue, saturationValue);

        // recompute the slider values before applying them to the colors
        brightnessValue = computeSliderValueToApply(brightnessValue, this.brightnessPanel.getSliderMaximumValue(), 255);
        contrastValue = computeSliderValueToApply(contrastValue, this.contrastPanel.getSliderMaximumValue(), 255);
        saturationValue = computeSliderValueToApply(saturationValue, this.saturationPanel.getSliderMaximumValue(), 100);

        ImageInfo sceneImageInfo = this.parentForm.getFormModel().getModifiedImageInfo();

        RasterDataNode[] rasterDataNodes = selectedSceneView.getSceneImage().getRasters();

        RGBChannelDef initialRGBChannelDef = brightnessContrastData.getInitialImageInfo().getRgbChannelDef();
        if (initialRGBChannelDef == null) {
            ColorPaletteDef colorPaletteDef = sceneImageInfo.getColorPaletteDef();
            for (int k=0; k<rasterDataNodes.length; k++) {
                RasterDataNode currentDataNode = rasterDataNodes[k];

                ColorPaletteDef initialColorPaletteDef = brightnessContrastData.getInitialImageInfo(currentDataNode).getColorPaletteDef();
                int pointCount = initialColorPaletteDef.getNumPoints();
                for (int i=0; i<pointCount; i++) {
                    ColorPaletteDef.Point initialPoint = initialColorPaletteDef.getPointAt(i);
                    Color newColor = computeColor(initialPoint.getColor(), brightnessValue, contrastValue, saturationValue);
                    ColorPaletteDef.Point currentPoint = colorPaletteDef.getPointAt(i);
                    currentPoint.setColor(newColor);
                }
            }
        } else {
            RGBChannelDef rgbChannelDef = sceneImageInfo.getRgbChannelDef();
            for (int i=0; i<rasterDataNodes.length; i++) {
                RasterDataNode currentDataNode = rasterDataNodes[i];
                if (currentDataNode instanceof Band) {
                    ColorPaletteDef initialColorPaletteDef = brightnessContrastData.getInitialImageInfo(currentDataNode).getColorPaletteDef();

                    Color initialFirstColor = initialColorPaletteDef.getFirstPoint().getColor();
                    Color newInitialFirstColor = computeColor(initialFirstColor, brightnessValue, contrastValue, saturationValue);
                    float firstPercent = computePercent(initialFirstColor, newInitialFirstColor);
                    double min = initialRGBChannelDef.getMinDisplaySample(i);
                    min = min + (min * firstPercent);
                    rgbChannelDef.setMinDisplaySample(i, min);

                    Color initialLastColor = initialColorPaletteDef.getLastPoint().getColor();
                    Color newInitialLastColor = computeColor(initialLastColor, brightnessValue, contrastValue, saturationValue);
                    float lastPercent = computePercent(initialLastColor, newInitialLastColor);
                    double max = initialRGBChannelDef.getMaxDisplaySample(i);
                    max = max + (max * lastPercent);
                    rgbChannelDef.setMaxDisplaySample(i, max);
                }
            }
        }

        selectedSceneView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_IMAGE_INFO, this.imageInfoChangeListener);
        try {
            this.parentForm.applyChanges();
            for (int i=0; i<rasterDataNodes.length; i++) {
                RasterDataNode currentDataNode = rasterDataNodes[i];
                currentDataNode.getProduct().setModified(true);
            }
        } finally {
            selectedSceneView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_IMAGE_INFO, this.imageInfoChangeListener);
        }
    }

    private static Color computeColor(Color color, int brightnessValue, int contrastValue, int saturationValue) {
        int newRgb = computePixelBrightness(color.getRGB(), brightnessValue);
        newRgb = computePixelContrast(newRgb, contrastValue);
        newRgb = computePixelSaturation(newRgb, saturationValue);
        return new Color(newRgb);
    }

    private static int computeSliderValueToApply(int visibleSliderValue, int maximumVisibleSliderValue, int maximumAllowedValue) {
        float visiblePercent = (float)visibleSliderValue / (float) maximumVisibleSliderValue;
        float percent = Math.round(visiblePercent * maximumAllowedValue);
        return (int)percent;
    }

    private static float computePercent(Color initialColor, Color currentColor) {
        float initialRedPercent = (float)initialColor.getRed() / 255.0f;
        float initialGreenPercent = (float)initialColor.getGreen() / 255.0f;
        float initialBluePercent = (float)initialColor.getBlue() / 255.0f;

        float currentRedPercent = (float)currentColor.getRed() / 255.0f;
        float currentGreenPercent = (float)currentColor.getGreen() / 255.0f;
        float currentBluePercent = (float)currentColor.getBlue() / 255.0f;

        float redPercent = initialRedPercent - currentRedPercent;
        float greenPercent = initialGreenPercent - currentGreenPercent;
        float bluePercent = initialBluePercent - currentBluePercent;

        return (redPercent + greenPercent + bluePercent) / 3.0f;
    }

    private static int checkRGBValue(int v) {
        if (v > 255) {
            return 255;
        }
        if (v < 0) {
            return 0;
        }
        return v;
    }

    private static int computePixelBrightness(int pixel, int sliderValue) {
        int red = ColorUtils.red(pixel) + sliderValue;
        int green = ColorUtils.green(pixel) + sliderValue;
        int blue = ColorUtils.blue(pixel) + sliderValue;

        return ColorUtils.rgba(checkRGBValue(red), checkRGBValue(green), checkRGBValue(blue));
    }

    private static int computePixelSaturation(int pixel, int sliderValue) {
        int red = ColorUtils.red(pixel);
        int green = ColorUtils.green(pixel);
        int blue = ColorUtils.blue(pixel);

        float[] hsb = new float[3];
        Color.RGBtoHSB(red, green, blue, hsb);
        hsb[1] += (sliderValue * 0.01f);
        if (hsb[1] > 1.0f) {
            hsb[1] = 1.0f;
        } else if (hsb[1] < 0.0f) {
            hsb[1] = 0.0f;
        }

        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }

    private static int computePixelContrast(int pixel, int sliderValue) {
        float factor = (259.0f * (sliderValue + 255.0f)) / (255.0f * (259.0f - sliderValue));
        int red = ColorUtils.red(pixel);
        int green = ColorUtils.green(pixel);
        int blue = ColorUtils.blue(pixel);

        int newRed = (int)(factor * (red - 128) + 128);
        int newGreen = (int)(factor * (green - 128) + 128);
        int newBlue = (int)(factor * (blue - 128) + 128);
        return ColorUtils.rgba(checkRGBValue(newRed), checkRGBValue(newGreen), checkRGBValue(newBlue));
    }
}