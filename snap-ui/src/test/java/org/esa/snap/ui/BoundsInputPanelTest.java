package org.esa.snap.ui;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class BoundsInputPanelTest {


    @Test
    public void testDecimalFormatter() throws Exception {
        DecimalFormatter formatter = BoundsInputPanel.DECIMAL_FORMATTER;

        assertEquals("0.003", formatter.valueToString(0.003));
        assertEquals("0.000001", formatter.valueToString(0.000001));
        assertEquals("0.00000102", formatter.valueToString(0.00000102));
        assertEquals("123456789.0", formatter.valueToString(123456789));
        assertEquals("123456789.12345", formatter.valueToString(123456789.12345));
    }
}