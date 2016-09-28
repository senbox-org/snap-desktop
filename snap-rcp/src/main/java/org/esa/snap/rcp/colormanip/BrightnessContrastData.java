package org.esa.snap.rcp.colormanip;

import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.util.HashMap;
import java.util.Map;

/**
 * The <code>BrightnessContrastData</code> class is used to store the initial image info of the band and slider values.
 *
 * @author Jean Coravu
 */
public class BrightnessContrastData {
    private final Map<RasterDataNode, ImageInfo> initialImageInfoMap;
    private ImageInfo initialImageInfo;
    private int brightnessSliderValue;
    private int contrastSliderValue;
    private int saturationSliderValue;

    /**
     * Constructs a new item.
     *
     * @param initialImageInfo the initial image info of the displayed band
     */
    public BrightnessContrastData(ImageInfo initialImageInfo) {
        this.initialImageInfoMap = new HashMap<>();
        this.initialImageInfo = initialImageInfo;

        this.brightnessSliderValue = 0;
        this.contrastSliderValue = 0;
        this.saturationSliderValue = 0;
    }

    public void putImageInfo(RasterDataNode rasterDataNode, ImageInfo imageInfo) {
        this.initialImageInfoMap.put(rasterDataNode, imageInfo);
    }

    public ImageInfo getInitialImageInfo(RasterDataNode rasterDataNode) {
        return this.initialImageInfoMap.get(rasterDataNode);
    }

    /**
     * Returns the initial image info.
     *
     * @return the initial image info, should never be <code>null</code>
     */
    public ImageInfo getInitialImageInfo() {
        return initialImageInfo;
    }

    /**
     * Returns the brightness slider value.
     *
     * @return the brightness slider value
     */
    public int getBrightnessSliderValue() {
        return brightnessSliderValue;
    }

    /**
     * Returns the contrast slider value.
     *
     * @return the contrast slider value
     */
    public int getContrastSliderValue() {
        return contrastSliderValue;
    }

    /**
     * Returns the saturation slider value.
     *
     * @return the saturation slider value
     */
    public int getSaturationSliderValue() {
        return saturationSliderValue;
    }

    /**
     * Sets the initial image info, before changing the current image info of the displayed band.
     *
     * @param initialImageInfo the initial image info
     */
    public void setInitialImageInfo(ImageInfo initialImageInfo) {
        this.initialImageInfo = initialImageInfo;
    }

    /**
     * Sets the slider values.
     *
     * @param brightnessSliderValue the brightness slider value
     * @param contrastSliderValue the contrast slider value
     * @param saturationSliderValue the saturation slider value
     */
    public void setSliderValues(int brightnessSliderValue, int contrastSliderValue, int saturationSliderValue) {
        this.brightnessSliderValue = brightnessSliderValue;
        this.contrastSliderValue = contrastSliderValue;
        this.saturationSliderValue = saturationSliderValue;
    }
}
