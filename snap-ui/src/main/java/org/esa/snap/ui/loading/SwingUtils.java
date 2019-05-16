package org.esa.snap.ui.loading;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Created by jcoravu on 10/1/2019.
 */
public class SwingUtils {

    private SwingUtils() {
    }

    public static JButton buildBrowseButton(ActionListener actionListener, int textFieldPreferredHeight) {
        JButton browseButton = new JButton("...");
        browseButton.setFocusable(false);
        browseButton.addActionListener(actionListener);
        Dimension preferredSize = new Dimension(textFieldPreferredHeight, textFieldPreferredHeight);
        browseButton.setPreferredSize(preferredSize);
        browseButton.setMinimumSize(preferredSize);
        browseButton.setMaximumSize(preferredSize);
        return browseButton;
    }

    public static GridBagConstraints buildConstraints(int columnIndex, int rowIndex, int fillType, int anchorType, int columnSpan, int rowSpan, Insets aMargins) {
        GridBagConstraints constraints = buildConstraints(columnIndex, rowIndex, fillType, anchorType, columnSpan, rowSpan);
        constraints.insets.top = aMargins.top;
        constraints.insets.left = aMargins.left;
        constraints.insets.bottom = aMargins.bottom;
        constraints.insets.right = aMargins.right;
        return constraints;
    }

    public static GridBagConstraints buildConstraints(int columnIndex, int rowIndex, int fillType, int anchorType, int columnSpan, int rowSpan, int topMargin, int leftMargin) {
        GridBagConstraints constraints = buildConstraints(columnIndex, rowIndex, fillType, anchorType, columnSpan, rowSpan);
        constraints.insets.top = topMargin;
        constraints.insets.left = leftMargin;
        constraints.insets.bottom = 0;
        constraints.insets.right = 0;
        return constraints;
    }

    public static GridBagConstraints buildConstraints(int columnIndex, int rowIndex, int fillType, int anchorType, int columnSpan, int rowSpan) {
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = columnIndex; // place the component on the first column (zero based index)
        constraints.gridy = rowIndex; // place the component on the first row (zero based index)
        constraints.gridwidth = columnSpan; // the component will have one cell on horizontal
        constraints.gridheight = rowSpan; // the component will have one cell on vertical
        constraints.weightx = 0; // the cell will not receive extra horizontal space
        constraints.weighty = 0; // the cell will not receive extra vertical space
        if (fillType == GridBagConstraints.HORIZONTAL) {
            constraints.weightx = 1.0;
        } else if (fillType == GridBagConstraints.VERTICAL) {
            constraints.weighty = 1.0;
        } else if (fillType == GridBagConstraints.BOTH) {
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;
        }
        constraints.fill = fillType;
        constraints.anchor = anchorType;

        return constraints;
    }
}
