package org.esa.snap.rcp.statistics;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class MetadataPlotPanelTest {
    @Test
    public void getRecordIndices() throws Exception {
        double[] recordIndices;

        recordIndices = MetadataPlotPanel.getRecordIndices(3, 5, 10);
        assertArrayEquals(new double[]{3, 4, 5, 6, 7}, recordIndices, 1.0e-6);

        recordIndices = MetadataPlotPanel.getRecordIndices(3, 8, 10);
        assertArrayEquals(new double[]{3, 4, 5, 6, 7, 8, 9, 10}, recordIndices, 1.0e-6);

        recordIndices = MetadataPlotPanel.getRecordIndices(12, 8, 10);
        assertArrayEquals(new double[]{10}, recordIndices, 1.0e-6);

    }

}