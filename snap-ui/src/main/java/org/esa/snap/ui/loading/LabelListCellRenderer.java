package org.esa.snap.ui.loading;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Created by jcoravu on 19/12/2018.
 */
public class LabelListCellRenderer<ItemType> extends JLabel implements ListCellRenderer<ItemType> {

    private final IItemRenderer<ItemType> itemRenderer;

    public LabelListCellRenderer(Insets margins) {
        this(margins, null);
    }

    public LabelListCellRenderer(Insets margins, IItemRenderer<ItemType> itemRenderer) {
        this.itemRenderer = itemRenderer;

        setOpaque(true);
        setBorder(new EmptyBorder(margins));
    }

    public LabelListCellRenderer(int itemHeight) {
        this(itemHeight, null);
    }

    public LabelListCellRenderer(int itemHeight, IItemRenderer<ItemType> itemRenderer) {
        this.itemRenderer = itemRenderer;

        setOpaque(true);

        Dimension rendererSize = getPreferredSize();
        rendererSize.height = itemHeight;
        setPreferredSize(rendererSize);
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

    protected String getItemDisplayText(ItemType item) {
        if (this.itemRenderer == null) {
            return (item == null) ? null : item.toString();
        }
        return this.itemRenderer.getItemDisplayText(item);
    }
}
