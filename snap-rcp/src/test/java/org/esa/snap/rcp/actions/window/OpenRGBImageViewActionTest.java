package org.esa.snap.rcp.actions.window;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.math.Range;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenRGBImageViewActionTest {

    @Test
    public void testMergeChannelDefs_allValuesFromUserInput() {
        final RGBImageProfile imageProfile = new RGBImageProfile("whatever",
                new String[]{"r", "g", "b"},
                null,
                null,
                new Range[]{new Range(1, 2), new Range(3, 4), new Range(5, 6)});
        final Band[] bands = new Band[3];
        bands[0] = mock(Band.class);
        bands[1] = mock(Band.class);
        bands[2] = mock(Band.class);

        final RGBChannelDef rgbChannelDef = OpenRGBImageViewAction.mergeRgbChannelDefs(imageProfile, bands);
        assertEquals(1.0, rgbChannelDef.getMinDisplaySample(0), 1e-8);
        assertEquals(2.0, rgbChannelDef.getMaxDisplaySample(0), 1e-8);
        assertEquals(3.0, rgbChannelDef.getMinDisplaySample(1), 1e-8);
        assertEquals(4.0, rgbChannelDef.getMaxDisplaySample(1), 1e-8);
        assertEquals(5.0, rgbChannelDef.getMinDisplaySample(2), 1e-8);
        assertEquals(6.0, rgbChannelDef.getMaxDisplaySample(2), 1e-8);
    }

    @Test
    public void testMergeChannelDefs_someValuesFromBandChannelDef() {
        final RGBImageProfile imageProfile = new RGBImageProfile("whatever",
                new String[]{"r", "g", "b"},
                null,
                null,
                new Range[]{new Range(1, 2), new Range(Double.NaN, 4), new Range(5, Double.NaN)});
        final Band[] bands = new Band[3];
        bands[0] = mock(Band.class);

        bands[1] = mock(Band.class);
        final RGBChannelDef chDef_1 = new RGBChannelDef();
        chDef_1.setMinDisplaySample(1, 0.13);
        when(bands[1].getImageInfo()).thenReturn(new ImageInfo(chDef_1));

        bands[2] = mock(Band.class);
        final RGBChannelDef chDef_2 = new RGBChannelDef();
        chDef_2.setMaxDisplaySample(2, 6.78);
        when(bands[2].getImageInfo()).thenReturn(new ImageInfo(chDef_2));

        final RGBChannelDef rgbChannelDef = OpenRGBImageViewAction.mergeRgbChannelDefs(imageProfile, bands);
        assertEquals(1.0, rgbChannelDef.getMinDisplaySample(0), 1e-8);
        assertEquals(2.0, rgbChannelDef.getMaxDisplaySample(0), 1e-8);
        assertEquals(0.13, rgbChannelDef.getMinDisplaySample(1), 1e-8);
        assertEquals(4.0, rgbChannelDef.getMaxDisplaySample(1), 1e-8);
        assertEquals(5.0, rgbChannelDef.getMinDisplaySample(2), 1e-8);
        assertEquals(6.78, rgbChannelDef.getMaxDisplaySample(2), 1e-8);
    }

    @Test
    public void testMergeChannelDefs_someValuesFromColorPalette() {
        final RGBImageProfile imageProfile = new RGBImageProfile("whatever",
                new String[]{"r", "g", "b"},
                null,
                null,
                new Range[]{new Range(1, Double.NaN), new Range(3, 4), new Range(Double.NaN, 6)});
        final Band[] bands = new Band[3];
        bands[0] = mock(Band.class);
        final ColorPaletteDef colorPaletteDef_0 = new ColorPaletteDef(0.8, 11.9);
        when(bands[0].getImageInfo()).thenReturn(new ImageInfo(colorPaletteDef_0));

        bands[1] = mock(Band.class);

        bands[2] = mock(Band.class);
        final ColorPaletteDef colorPaletteDef_2 = new ColorPaletteDef(0.9, 12.0);
        when(bands[2].getImageInfo()).thenReturn(new ImageInfo(colorPaletteDef_2));

        final RGBChannelDef rgbChannelDef = OpenRGBImageViewAction.mergeRgbChannelDefs(imageProfile, bands);
        assertEquals(1.0, rgbChannelDef.getMinDisplaySample(0), 1e-8);
        assertEquals(11.9, rgbChannelDef.getMaxDisplaySample(0), 1e-8);
        assertEquals(3.0, rgbChannelDef.getMinDisplaySample(1), 1e-8);
        assertEquals(4.0, rgbChannelDef.getMaxDisplaySample(1), 1e-8);
        assertEquals(0.9, rgbChannelDef.getMinDisplaySample(2), 1e-8);
        assertEquals(6.0, rgbChannelDef.getMaxDisplaySample(2), 1e-8);
    }
}
