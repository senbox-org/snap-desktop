package org.esa.snap.rcp.spectrallibrary.ui;

import org.esa.snap.rcp.spectrallibrary.model.SpectralProfileTableModel;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.junit.Test;

import java.awt.Color;
import java.awt.Paint;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class PreviewPanelTest {


    @Test
    public void setProfiles_usesDisplayColorAttributeWhenNoPaintOverrideExists() throws Exception {
        SpectralProfile profile = SpectralProfile.create("Profile_1", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute(SpectralProfileTableModel.ATTR_DISPLAY_COLOR, AttributeValue.ofString("#123456"));
        PreviewPanel panel = new PreviewPanel();

        panel.setProfiles(List.of(profile));

        Color seriesPaint = seriesPaint(panel, 0);
        assertEquals(0x12, seriesPaint.getRed());
        assertEquals(0x34, seriesPaint.getGreen());
        assertEquals(0x56, seriesPaint.getBlue());
    }

    private static Color seriesPaint(PreviewPanel panel, int seriesIndex) throws Exception {
        Field chartField = PreviewPanel.class.getDeclaredField("chart");
        chartField.setAccessible(true);
        JFreeChart chart = (JFreeChart) chartField.get(panel);

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chart.getXYPlot().getRenderer();
        Paint paint = renderer.getSeriesPaint(seriesIndex);
        assertTrue(paint instanceof Color);
        return (Color) paint;
    }
}
