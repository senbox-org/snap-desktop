package org.esa.snap.rcp.imagebrightness;

/**
 * This component shows the panel containing the three sliders: brightness, contrast, saturation.
 *
 * @author Jean Coravu
 */

import com.bc.ceres.core.*;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.math.Histogram;
import org.esa.snap.engine_utilities.util.Maths;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.SelectionSupport;
import org.esa.snap.rcp.windows.ToolTopComponent;
import org.esa.snap.ui.product.ProductSceneView;
import org.netbeans.api.annotations.common.NonNull;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderableOp;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

@TopComponent.Description(
        preferredID = "BrightnessContrastTopComponent",
        iconBase = "org/esa/snap/rcp/icons/BrightnessContrast.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = true,
        position = 60
)
@ActionID(category = "Window", id = "org.esa.snap.rcp.imagebrightness.BrightnessContrastToolTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/View/Tool Windows"),
        @ActionReference(path = "Toolbars/Tool Windows")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_BrightnessContrastTopComponent_Name",
        preferredID = "BrightnessContrastTopComponent"
)
@NbBundle.Messages({
        "CTL_BrightnessContrastTopComponent_Name=Brightness and Contrast",
        "CTL_BrightnessContrastTopComponent_HelpId=showBrightnessContrastWnd"
})

public class BrightnessContrastToolTopComponent extends ToolTopComponent implements SelectionSupport.Handler<ProductSceneView> {
    private SliderPanel brightnessPanel;
    private SliderPanel contrastPanel;
    private SliderPanel saturationPanel;
    private JLabel messageLabel;
    private PropertyChangeListener imageInfoChangeListener;
    private Map<ProductSceneView, BrightnessContrastData> visibleProductScenes;

    public BrightnessContrastToolTopComponent() {
        super();

        setDisplayName(getTitle());
        setLayout(new BorderLayout());

        StringBuilder str = new StringBuilder();
        str.append("<html>")
           .append("This tool window is used to change the brightness, contrast, saturation of an image.")
           .append("<br>")
           .append("Right now, there is no selected image view.")
           .append("</html>");
        this.messageLabel = new JLabel(str.toString(), JLabel.CENTER);

        ChangeListener sliderChangeListener = event -> applySliderValues();

        this.brightnessPanel = new SliderPanel("Brightness", sliderChangeListener, -255, 255);
        this.contrastPanel = new SliderPanel("Contrast", sliderChangeListener, -255, 255);
        this.saturationPanel = new SliderPanel("Saturation", sliderChangeListener, -100, 100);

        this.visibleProductScenes = new HashMap<>();

        this.imageInfoChangeListener = event -> {
            ProductSceneView selectedView = getSelectedProductSceneView();
            sceneImageInfoChangedOutside(selectedView);
        };

        ProductSceneView selectedView = getSelectedProductSceneView();
        if (selectedView == null) {
            displayNoSelectedImageView();
        } else {
            productSceneViewSelected(selectedView);
        }
    }

    @Override
    public void selectionChange(ProductSceneView oldValue, ProductSceneView newValue) {
    }

    @Override
    protected void productSceneViewSelected(@NonNull ProductSceneView selectedSceneView) {
        selectedSceneView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_IMAGE_INFO, this.imageInfoChangeListener);

        displaySelectedImageView();

        BrightnessContrastData brightnessContrastData = this.visibleProductScenes.get(selectedSceneView);
        if (brightnessContrastData == null) {
            RasterDataNode[] rasterDataNodes = selectedSceneView.getSceneImage().getRasters();

            ImageInfo imageInfo = ImageManager.getInstance().getImageInfo(rasterDataNodes);
            ImageInfo initialImageInfo = imageInfo.clone();
            brightnessContrastData = new BrightnessContrastData(initialImageInfo);
            this.visibleProductScenes.put(selectedSceneView, brightnessContrastData);
        }
        refreshSliderValues(brightnessContrastData);
    }

    @Override
    protected void productSceneViewDeselected(@NonNull ProductSceneView deselectedSceneView) {
        deselectedSceneView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_IMAGE_INFO, this.imageInfoChangeListener);

        displayNoSelectedImageView();
    }

    private String getTitle() {
        return Bundle.CTL_BrightnessContrastTopComponent_Name();
    }

    private void displayNoSelectedImageView() {
        removeAll();
        add(this.messageLabel, BorderLayout.CENTER);
    }

    private void refreshSliderValues(BrightnessContrastData brightnessContrastData) {
        this.brightnessPanel.setSliderValue(brightnessContrastData.getBrightnessSliderValue());
        this.contrastPanel.setSliderValue(brightnessContrastData.getContrastSliderValue());
        this.saturationPanel.setSliderValue(brightnessContrastData.getSaturationSliderValue());
    }

    private void sceneImageInfoChangedOutside(ProductSceneView selectedSceneView) {
        BrightnessContrastData brightnessContrastData = this.visibleProductScenes.get(selectedSceneView);
        RasterDataNode[] rasterDataNodes = selectedSceneView.getSceneImage().getRasters();
        ImageInfo imageInfo = ImageManager.getInstance().getImageInfo(rasterDataNodes);
        ImageInfo initialImageInfo = imageInfo.clone();
        brightnessContrastData.setInitialImageInfo(initialImageInfo);
        brightnessContrastData.setSliderValues(0, 0, 0);
        refreshSliderValues(brightnessContrastData);
    }

    private void displaySelectedImageView() {
        removeAll();

        JPanel colorsPanel = new JPanel(new GridLayout(3, 1, 0, 15));
        colorsPanel.add(this.brightnessPanel);
        colorsPanel.add(this.contrastPanel);
        colorsPanel.add(this.saturationPanel);

        JScrollPane scrollPane = new JScrollPane(colorsPanel);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        add(scrollPane, BorderLayout.NORTH);
    }

    private void applySliderValues() {
        ProductSceneView selectedSceneView = getSelectedProductSceneView();

        BrightnessContrastData brightnessContrastData = this.visibleProductScenes.get(selectedSceneView);
        brightnessContrastData.setSliderValues(this.brightnessPanel.getSliderValue(), this.contrastPanel.getSliderValue(), this.saturationPanel.getSliderValue());

        RasterDataNode[] rasterDataNodes = selectedSceneView.getSceneImage().getRasters();
        RasterDataNode firstDataNode = rasterDataNodes[0];

        ImageInfo imageInfo = firstDataNode.getImageInfo().clone();
        ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();

        ColorPaletteDef initialColorPaletteDef = brightnessContrastData.getInitialImageInfo().getColorPaletteDef();
        int pointCount = initialColorPaletteDef.getNumPoints();
        for (int i=0; i<pointCount; i++) {
            ColorPaletteDef.Point initialPoint = initialColorPaletteDef.getPointAt(i);
            int rgb = initialPoint.getColor().getRGB();

            int newRgb = computePixelBrightness(rgb, this.brightnessPanel.getSliderValue());
            newRgb = computePixelContrast(newRgb, this.contrastPanel.getSliderValue());
            newRgb = computePixelSaturation(newRgb, this.saturationPanel.getSliderValue());

            ColorPaletteDef.Point point = colorPaletteDef.getPointAt(i);
            point.setColor(new Color(newRgb));
        }

        selectedSceneView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_IMAGE_INFO, this.imageInfoChangeListener);
        try {
            selectedSceneView.setImageInfo(imageInfo);
            firstDataNode.setImageInfo(imageInfo);
        } finally {
            selectedSceneView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_IMAGE_INFO, this.imageInfoChangeListener);
        }
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

        float[] hsv = new float[3];
        Color.RGBtoHSB(red, green, blue, hsv);
        hsv[1] += (sliderValue * 0.01f);
        if (hsv[1] > 1.0f) {
            hsv[1] = 1.0f;
        } else if (hsv[1] < 0.0f) {
            hsv[1] = 0.0f;
        }

        return Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]);
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
