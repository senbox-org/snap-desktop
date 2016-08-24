package org.esa.snap.rcp.imagebrightness;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * The <code>NumberPlainDocument</code> class can be used to allow only digits in the input text field.
 *
 * @author Jean Coravu
 */
public class NumberPlainDocument extends PlainDocument {
    private final int minimumNumber;
    private final int maximumNumber;

    /**
     * Constructs a new item by specifying the minimum number and the maximum number.
     *
     * @param minimumNumber the minimum number
     * @param maximumNumber the maximum number
     */
    public NumberPlainDocument(int minimumNumber, int maximumNumber) {
        super();

        this.minimumNumber = minimumNumber;
        this.maximumNumber = maximumNumber;
    }

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        StringBuilder text = new StringBuilder();
        text.append(getText(0, offs));
        text.append(str);
        text.append(getText(offs, getLength()-offs));
        int startIndex = (text.charAt(0) == '-') ? 1 : 0;
        for (int i=startIndex; i<text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return;
            }
        }
        boolean canConvert = true;
        if (text.charAt(0) == '-' && text.length() == 1) {
            canConvert = false;
        }
        if (canConvert) {
            int number = Integer.parseInt(text.toString());
            if (number < this.minimumNumber || number > this.maximumNumber) {
                return;
            }
        }

        super.insertString(offs, str, a);
    }
}
