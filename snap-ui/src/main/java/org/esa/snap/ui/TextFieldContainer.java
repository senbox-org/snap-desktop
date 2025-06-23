package org.esa.snap.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Created by knowles on 4/5/17.
 */
public class TextFieldContainer {

    private boolean valid = false;
    private Number value;
    private JTextField textfield;
    private JLabel label;

    private String valueString;
    private String name;
    private Number minval;
    private Number maxval;
    private Number defval;
    private Container parentDialogContentPane = null;

    private boolean list = false;
    private NumType numType = NumType.DOUBLE;

    public static enum NumType {
        BYTE,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE
    }


    public TextFieldContainer(String name, NumType numType, int numCols,  Container parentDialogContentPane) {
        this(name,  Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,  numType, numCols, parentDialogContentPane);
    }

    public TextFieldContainer(String name, Number defval, NumType numType, int numCols,  Container parentDialogContentPane) {
        this(name,  defval, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,  numType, numCols, parentDialogContentPane);
    }

    public TextFieldContainer(String name, Number defval, Number minval, Number maxval, NumType numType, int numCols,  Container parentDialogContentPane) {
        this.name = name;
        this.defval = defval;
        this.minval = minval;
        this.maxval = maxval;
        this.numType = numType;
        this.parentDialogContentPane = parentDialogContentPane;

        textfield = new JTextField(numCols);

        label = new JLabel(name);
        getTextfield().setName(name);
        //   if (Double.isNaN(defval.doubleValue())) {
        //      getTextfield().setText("");
        //   } else {
        getTextfield().setText(defval.toString());
        //  }
        textfieldHandler();

        setValid(validate(false, true));
    }


    public void setToolTipText(String toolTipText) {
        if (textfield != null) {
            textfield.setToolTipText(toolTipText);
        }

        if (label != null) {
            label.setToolTipText(toolTipText);
        }

    }
    private void textfieldHandler() {

        getTextfield().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                textfieldHandlerAction();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                textfieldHandlerAction();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                textfieldHandlerAction();
            }
        });
    }

    private void textfieldHandlerAction() {

        setValid(validate(true, true));
    }


    // userEntryMode is more forgiving about whether to show the Dialog since user may not be finished typing.
    public boolean validate(boolean showDialog, boolean userEntryMode) {

        try {
            valueString = getTextfield().getText().toString();
            value = Double.valueOf(valueString);


            if (!testNumType(value, numType)) {
                if (showDialog) {
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                            "ERROR: Value in " + getName() + " must be of type " + numType.toString() + ")",
                            "Invalid Input",
                            JOptionPane.ERROR_MESSAGE);
                }

                return false;
            }


            if (getValue().doubleValue() < getMinval().doubleValue() || getValue().doubleValue() > getMaxval().doubleValue()) {
                if (userEntryMode) {

                    //todo Danny this test may be able to be refined
                    // case 1: if minVal = 25 and user type 100 then problem with first char but following logic resolves this one
                    // case 2: if minVal = 25 and user types 0.01 then problem with first char
                    // case 2 resolution needs to be looking at whether a 0 to 1 fraction is being forced
                    // test user is typing positive number
                    if (getValue().doubleValue() < getMinval().doubleValue() && getValue().doubleValue() >= 0 && getMinval().doubleValue() >= 0) {
                        return false;
                    }
                    // test user is typing negative number
                    if (getValue().doubleValue() > getMaxval().doubleValue() && getValue().doubleValue() <= 0 && getMaxval().doubleValue() <= 0) {
                        return false;
                    }

                }
                if (showDialog) {
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                            "ERROR: Valid " + getName() + " range is (" + getMinval() + " to " + getMaxval() + ")",
                            "Invalid Input",
                            JOptionPane.ERROR_MESSAGE);
                }

                return false;
            }
        } catch (NumberFormatException exception) {

            String trimmed = getTextfield().getText().toString().trim();

            if (userEntryMode && (trimmed.length() == 0 ||  "-".equals(trimmed) || ".".equals(trimmed))) {
                return false;
            }
            if (showDialog) {
                JOptionPane.showMessageDialog(getParentDialogContentPane(),
                        getName() + "  " + exception.toString(),
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }

        return true;
    }

    public void reset() {
        //    if (Double.isNaN(defval.doubleValue())) {
        //        getTextfield().setText("");
        //   } else {
        getTextfield().setText(defval.toString());
        //    }
    }



    public void reset(Number defval) {
        this.defval = defval;

        getTextfield().setText(defval.toString());
    }

    public void setEnabled(boolean enabled) {
        if (getLabel() != null) {
            getLabel().setEnabled(enabled);
        }

        if (getTextfield() != null) {
            getTextfield().setEnabled(enabled);
        }
    }

    public void setVisible(boolean visible) {
        if (getLabel() != null) {
            getLabel().setVisible(visible);
        }

        if (getTextfield() != null) {
            getTextfield().setVisible(visible);
        }
    }

    public static boolean testNumType (Number number, NumType numType) {

        double numberDouble = number.doubleValue();

        if (numType == NumType.BYTE) {
            return  (numberDouble == number.byteValue()) ?  true :  false;
        }

        if (numType == NumType.SHORT) {
            return  (numberDouble == number.shortValue()) ?  true :  false;
        }

        if (numType == NumType.INT) {
            return  (numberDouble == number.intValue()) ?  true :  false;
        }

        if (numType == NumType.LONG) {
            return  (numberDouble == number.longValue()) ?  true :  false;
        }

        if (numType == NumType.FLOAT) {
            return  (numberDouble == number.floatValue()) ?  true :  false;
        }

        return true;
    }


    private void setValid(boolean valid) {
        this.valid = valid;
    }


    public JTextField getTextfield() {
        return textfield;
    }

    public JLabel getLabel() {
        return label;
    }

    public boolean isValid(boolean showDialog) {
        if (!valid && showDialog) {
            validate(true, false);
        }
        return valid;
    }

    public Number getValue() {
        return value;
    }

    public String getValueString() {
        return valueString;
    }

    public void setValueString(String valueString) {
        this.valueString = valueString;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Number getMinval() {
        return minval;
    }

    public void setMinval(int minval) {
        this.minval = minval;
    }

    public Number getMaxval() {
        return maxval;
    }

    public void setMaxval(int maxval) {
        this.maxval = maxval;
    }

    public Number getDefval() {
        return defval;
    }

    public void setDefval(int defval) {
        this.defval = defval;
    }


    public boolean isList() {
        return list;
    }

    public void setList(boolean list) {
        this.list = list;
    }



    public Container getParentDialogContentPane() {
        return parentDialogContentPane;
    }

    public void setParentDialogContentPane(Container parentDialogContentPane) {
        this.parentDialogContentPane = parentDialogContentPane;
    }
}