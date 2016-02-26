package org.esa.snap.ui;

import javax.swing.JFormattedTextField;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

/**
 * A formatter to be used for decimal number in a {@link JFormattedTextField}
 *
 * @see JFormattedTextField.AbstractFormatter
 * @author Marco Peters
 */
public class DecimalFormatter extends JFormattedTextField.AbstractFormatter {

    private final DecimalFormat format;

    public DecimalFormatter(String pattern) {
        final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
        format = new DecimalFormat(pattern, decimalFormatSymbols);
        format.setParseIntegerOnly(false);
        format.setParseBigDecimal(false);
        format.setDecimalSeparatorAlwaysShown(true);
    }

    @Override
    public Object stringToValue(String text) throws ParseException {
        return format.parse(text).doubleValue();
    }

    @Override
    public String valueToString(Object value) throws ParseException {
        if (value == null) {
            return "";
        }
        return format.format(value);
    }
}
