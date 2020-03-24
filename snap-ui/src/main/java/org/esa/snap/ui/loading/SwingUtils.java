package org.esa.snap.ui.loading;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Created by jcoravu on 10/1/2019.
 */
public class SwingUtils {

    public static final Color TRANSPARENT_COLOR = new Color(255, 255, 255, 0);

    public static LineBorder LINE_BORDER = new LineBorder(Color.GRAY, 1);

    private SwingUtils() {
    }

    public static ImageIcon loadImage(String resourceImagePath) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL imageURL = classLoader.getResource(resourceImagePath);
        if (imageURL == null) {
            throw new NullPointerException("The image '"+resourceImagePath+"' does not exist into the sources.");
        }
        return new ImageIcon(imageURL);
    }

    public static ImageIcon loadImage(String resourceImagePath, Dimension buttonSize, Integer scaledImagePadding) {
        ImageIcon icon = loadImage(resourceImagePath);
        if (scaledImagePadding != null && scaledImagePadding.intValue() >= 0) {
            Image scaledImage = getScaledImage(icon.getImage(), buttonSize.width, buttonSize.height, scaledImagePadding.intValue());
            icon = new ImageIcon(scaledImage);
        }
        return icon;
    }

    public static JButton buildButton(String resourceImagePath, ActionListener buttonListener, Dimension buttonSize, Integer scaledImagePadding) {
        ImageIcon icon = loadImage(resourceImagePath, buttonSize, scaledImagePadding);
        JButton button = new JButton(icon);
        button.setFocusable(false);
        button.addActionListener(buttonListener);
        button.setPreferredSize(buttonSize);
        button.setMinimumSize(buttonSize);
        button.setMaximumSize(buttonSize);
        return button;
    }

    private static Image getScaledImage(Image srcImg, int destinationImageWidth, int destinationImageHeight, int padding) {
        BufferedImage resizedImg = new BufferedImage(destinationImageWidth, destinationImageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, padding, padding, destinationImageWidth-padding, destinationImageHeight-padding, 0, 0, srcImg.getWidth(null), srcImg.getHeight(null), null);
        g2.dispose();
        return resizedImg;
    }

    public static JComboBox<String> buildComboBox(String[] values, String valueToSelect, int textFieldPreferredHeight, boolean isEditable) {
        ItemRenderer<String> itemRenderer = new ItemRenderer<String>() {
            @Override
            public String getItemDisplayText(String item) {
                return (item == null) ? " " : item;
            }
        };
        JComboBox<String> comboBox = new CustomComboBox(itemRenderer, textFieldPreferredHeight, isEditable);
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                comboBox.addItem(values[i]);
            }
        }
        if (valueToSelect != null) {
            for (int i=0; i<values.length; i++) {
                if (valueToSelect.equals(values[i])) {
                    comboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        return comboBox;
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
