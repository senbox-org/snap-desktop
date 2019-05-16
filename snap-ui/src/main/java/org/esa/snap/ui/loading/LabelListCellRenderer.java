package org.esa.snap.ui.loading;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Created by jcoravu on 19/12/2018.
 */
public abstract class LabelListCellRenderer<ItemType> extends JLabel implements ListCellRenderer<ItemType> {

    public LabelListCellRenderer(Insets margins) {
        setOpaque(true);
        setBorder(new EmptyBorder(margins));
    }

    @Override
    public JLabel getListCellRendererComponent(JList<? extends ItemType> list, ItemType value, int index, boolean isSelected, boolean cellHasFocus) {
        Color backgroundColor;
        Color foregroundColor;
        if (isSelected) {
            backgroundColor = list.getSelectionBackground();
            foregroundColor = list.getSelectionForeground();
        } else {
            backgroundColor = list.getBackground();
            foregroundColor = list.getForeground();
        }
        setBackground(backgroundColor);
        setForeground(foregroundColor);

        String itemDisplayName = getItemDisplayText(value);
        setText(itemDisplayName);
        setEnabled(list.isEnabled());
        setFont(list.getFont());

        return this;
    }

    protected abstract String getItemDisplayText(ItemType value);
}
