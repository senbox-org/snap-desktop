package org.esa.snap.rcp.util;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.text.DateFormat;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class DateTimePickerCellEditorTest {

    private static final DateFormat DATE_FORMAT = ProductData.UTC.createDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateFormat TIME_FORMAT = ProductData.UTC.createDateFormat("HH:mm:ss");

    @Test
    public void testDateFormatTimeZone() throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        DateTimePickerCellEditor editor = new DateTimePickerCellEditor(DATE_FORMAT, TIME_FORMAT);
        DateFormat[] formats = editor.getFormats();
        assertEquals(1, formats.length);
        assertSame(DATE_FORMAT, formats[0]);
        assertEquals(DATE_FORMAT, formats[0]);
        assertEquals(TimeZone.getTimeZone("UTC"), formats[0].getTimeZone());
        assertSame(TIME_FORMAT, editor.getTimeFormat());
        assertEquals(TimeZone.getTimeZone("UTC"), editor.getTimeFormat().getTimeZone());
    }

}
